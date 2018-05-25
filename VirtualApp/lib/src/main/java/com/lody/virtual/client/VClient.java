package com.lody.virtual.client;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.Environment;
import android.util.Log;
import android.text.TextUtils;

import com.lody.virtual.client.core.CrashHandler;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.AppInstrumentation;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.hook.secondary.ProxyServiceFactory;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.ipc.VirtualStorageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.helper.compat.StorageManagerCompat;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.ReflectException;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.VDeviceInfo;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityThread;
import mirror.android.app.ActivityThreadNMR1;
import mirror.android.app.ContextImpl;
import mirror.android.app.ContextImplKitkat;
import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApk;
import mirror.android.app.LoadedApkICS;
import mirror.android.app.LoadedApkKitkat;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.res.CompatibilityInfo;
import mirror.android.providers.Settings;
import mirror.android.renderscript.RenderScriptCacheDir;
import mirror.android.view.CompatibilityInfoHolder;
import mirror.android.view.DisplayAdjustments;
import mirror.android.view.HardwareRenderer;
import mirror.android.view.RenderScript;
import mirror.android.view.ThreadedRenderer;
import mirror.com.android.internal.content.ReferrerIntent;
import mirror.dalvik.system.VMRuntime;
import mirror.java.lang.ThreadGroupN;

import static com.lody.virtual.helper.compat.ActivityManagerCompat.SERVICE_DONE_EXECUTING_ANON;
import static com.lody.virtual.helper.compat.ActivityManagerCompat.SERVICE_DONE_EXECUTING_START;
import static com.lody.virtual.helper.compat.ActivityManagerCompat.SERVICE_DONE_EXECUTING_STOP;
import static com.lody.virtual.os.VUserHandle.getUserId;

/**
 * @author Lody
 */

public final class VClient extends IVClient.Stub {

    private static final int NEW_INTENT = 11;
    private static final int RECEIVER = 12;
    public static final int CREATE_SERVICE = 13;
    public static final int SERVICE_ARGS = 14;
    public static final int STOP_SERVICE = 15;
    public static final int BIND_SERVICE = 16;
    public static final int UNBIND_SERVICE = 17;

    private static final String TAG = VClient.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static final VClient gClient = new VClient();
    private final H mH = new H();
    private Instrumentation mInstrumentation = AppInstrumentation.getDefault();
    final ArrayMap<IBinder, Service> mServices = new ArrayMap<>();
    private IBinder token;
    private int vuid;
    private int vpid;
    private ConditionVariable mTempLock;
    private VDeviceInfo deviceInfo;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private CrashHandler crashHandler;
    private InstalledAppInfo mAppInfo;

    public InstalledAppInfo getAppInfo() {
        return mAppInfo;
    }

    public static VClient get() {
        return gClient;
    }

    public boolean isBound() {
        return mBoundApplication != null;
    }

    public boolean isNotCopyApk() {
        InstalledAppInfo appInfo = getAppInfo();
        return appInfo != null && appInfo.notCopyApk;
    }

    public VDeviceInfo getDeviceInfo() {
        if (deviceInfo == null) {
            synchronized (this) {
                if (deviceInfo == null) {
                    deviceInfo = VDeviceManager.get().getDeviceInfo(getUserId(vuid));
                }
            }
        }
        return deviceInfo;
    }

    public Application getCurrentApplication() {
        return mInitialApplication;
    }

    public String getCurrentPackage() {
        return mBoundApplication != null ?
                mBoundApplication.appInfo.packageName : VPackageManager.get().getNameForUid(getVUid());
    }

    public ApplicationInfo getCurrentApplicationInfo() {
        return mBoundApplication != null ? mBoundApplication.appInfo : null;
    }

    public CrashHandler getCrashHandler() {
        return crashHandler;
    }

    public void setCrashHandler(CrashHandler crashHandler) {
        this.crashHandler = crashHandler;
    }

    public int getVUid() {
        return vuid;
    }

    public int getVpid() {
        return vpid;
    }

    public int getBaseVUid() {
        return VUserHandle.getAppId(vuid);
    }

    public ClassLoader getClassLoader(ApplicationInfo appInfo) {
        Context context = createPackageContext(appInfo.packageName);
        return context.getClassLoader();
    }

    private void sendMessage(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        mH.sendMessage(msg);
    }

    @Override
    public IBinder getAppThread() {
        return ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
    }

    @Override
    public IBinder getToken() {
        return token;
    }

    public void initProcess(IBinder token, int vuid, int vpid) {
        this.token = token;
        this.vuid = vuid;
        this.vpid = vpid;
    }

    private void handleNewIntent(NewIntentData data) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent = ReferrerIntent.ctor.newInstance(data.intent, data.creator);
        } else {
            intent = data.intent;
        }
        if (ActivityThread.performNewIntents != null) {
            ActivityThread.performNewIntents.call(
                    VirtualCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent)
            );
        } else {
            ActivityThreadNMR1.performNewIntents.call(
                    VirtualCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent),
                    true);
        }
    }

    public boolean bindApplication(final String packageName, final String processName) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            if (!VClient.get().isBound()) {
                bindApplicationNoCheck(packageName, processName, new ConditionVariable());
            }
            return true;
        } else {
            final ConditionVariable lock = new ConditionVariable();
            VirtualRuntime.getUIHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (!VClient.get().isBound()) {
                        bindApplicationNoCheck(packageName, processName, lock);
                    }
                    lock.open();
                }
            });
            lock.block();
        }
        return false;
    }

    private void bindApplicationNoCheck(String packageName, String processName, ConditionVariable lock) {
        mTempLock = lock;
        VDeviceInfo deviceInfo = getDeviceInfo();
        if (processName == null) {
            processName = packageName;
        }
        try {
            setupUncaughtHandler();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            fixInstalledProviders();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        mirror.android.os.Build.SERIAL.set(deviceInfo.getSerial());
        mirror.android.os.Build.DEVICE.set(Build.DEVICE.replace(" ", "_"));
        ActivityThread.mInitialApplication.set(
                VirtualCore.mainThread(),
                null
        );
        AppBindData data = new AppBindData();
        InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        if (info == null) {
            new Exception("app not exist").printStackTrace();
            Process.killProcess(0);
            System.exit(0);
        }
        mAppInfo = info;
        data.appInfo = VPackageManager.get().getApplicationInfo(packageName, 0, getUserId(vuid));
        data.processName = processName;
        data.providers = VPackageManager.get().queryContentProviders(processName, getVUid(), PackageManager.GET_META_DATA);
        VLog.i(TAG, "Binding application %s (%s)", data.appInfo.packageName, data.processName);
        mBoundApplication = data;
        VirtualRuntime.setupRuntime(data.processName, data.appInfo);
        int targetSdkVersion = data.appInfo.targetSdkVersion;
        if (targetSdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy newPolicy = new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy()).permitNetwork().build();
            StrictMode.setThreadPolicy(newPolicy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && targetSdkVersion < Build.VERSION_CODES.LOLLIPOP) {
            mirror.android.os.Message.updateCheckRecycle.call(targetSdkVersion);
        }
        if (VASettings.ENABLE_IO_REDIRECT) {
            startIOUniformer();
        }
        NativeEngine.launchEngine();
        Object mainThread = VirtualCore.mainThread();
        NativeEngine.startDexOverride();
        Context context = createPackageContext(data.appInfo.packageName);
        System.setProperty("java.io.tmpdir", context.getCacheDir().getAbsolutePath());
        File codeCacheDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            codeCacheDir = context.getCodeCacheDir();
        } else {
            codeCacheDir = context.getCacheDir();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (HardwareRenderer.setupDiskCache != null) {
                HardwareRenderer.setupDiskCache.call(codeCacheDir);
            }
        } else {
            if (ThreadedRenderer.setupDiskCache != null) {
                ThreadedRenderer.setupDiskCache.call(codeCacheDir);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (RenderScriptCacheDir.setupDiskCache != null) {
                RenderScriptCacheDir.setupDiskCache.call(codeCacheDir);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (RenderScript.setupDiskCache != null) {
                RenderScript.setupDiskCache.call(codeCacheDir);
            }
        }
        Object boundApp = fixBoundApp(mBoundApplication);
        mBoundApplication.info = ContextImpl.mPackageInfo.get(context);
        mirror.android.app.ActivityThread.AppBindData.info.set(boundApp, data.info);
        VMRuntime.setTargetSdkVersion.call(VMRuntime.getRuntime.call(), data.appInfo.targetSdkVersion);

        Configuration configuration = context.getResources().getConfiguration();
        Object compatInfo = CompatibilityInfo.ctor.newInstance(data.appInfo, configuration.screenLayout, configuration.smallestScreenWidthDp, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                DisplayAdjustments.setCompatibilityInfo.call(ContextImplKitkat.mDisplayAdjustments.get(context), compatInfo);
            }
            DisplayAdjustments.setCompatibilityInfo.call(LoadedApkKitkat.mDisplayAdjustments.get(mBoundApplication.info), compatInfo);
        } else {
            CompatibilityInfoHolder.set.call(LoadedApkICS.mCompatibilityInfo.get(mBoundApplication.info), compatInfo);
        }

        if(data.appInfo != null && "com.tencent.mm".equals(data.appInfo.packageName)
                && "com.tencent.mm".equals(data.appInfo.processName)){
            ClassLoader originClassLoader = context.getClassLoader();
            fixWeChatTinker(context, data.appInfo, originClassLoader);
        }

        mInitialApplication = LoadedApk.makeApplication.call(data.info, false, null);
        mirror.android.app.ActivityThread.mInitialApplication.set(mainThread, mInitialApplication);
        ContextFixer.fixContext(mInitialApplication);
        if (Build.VERSION.SDK_INT >= 24 && "com.tencent.mm:recovery".equals(processName)) {
            fixWeChatRecovery(mInitialApplication);
        }
        if (data.providers != null) {
            installContentProviders(mInitialApplication, data.providers);
        }

        VirtualCore.get().getComponentDelegate().beforeApplicationCreate(mInitialApplication);
        try {
            mInstrumentation.callApplicationOnCreate(mInitialApplication);
            InvocationStubManager.getInstance().checkEnv(HCallbackStub.class);
            Application createdApp = ActivityThread.mInitialApplication.get(mainThread);
            if (createdApp != null) {
                mInitialApplication = createdApp;
            }
        } catch (Exception e) {
            if (!mInstrumentation.onException(mInitialApplication, e)) {
                throw new RuntimeException(
                        "Unable to create application " + mInitialApplication.getClass().getName()
                                + ": " + e.toString(), e);
            }
        }
        VActivityManager.get().appDoneExecuting();
        VirtualCore.get().getComponentDelegate().afterApplicationCreate(mInitialApplication);
        if (lock != null) {
            lock.open();
            mTempLock = null;
        }
    }

    private void fixWeChatRecovery(Application app) {
        try {
            Field field = app.getClassLoader().loadClass("com.tencent.recovery.Recovery").getField("context");
            field.setAccessible(true);
            if (field.get(null) != null) {
                return;
            }
            field.set(null, app.getBaseContext());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void fixWeChatTinker(Context context, ApplicationInfo applicationInfo, ClassLoader appClassLoader)
    {
        String dataDir = applicationInfo.dataDir;
        File tinker = new File(dataDir, "tinker");
        if(tinker.exists()){
            Log.e("wxd", " deleteWechatTinker " + tinker.getPath());
            FileUtils.deleteDir(tinker);
        }
        File tinker_temp = new File(dataDir, "tinker_temp");
        if(tinker_temp.exists()){
            Log.e("wxd", " deleteWechatTinker " + tinker_temp.getPath());
            FileUtils.deleteDir(tinker_temp);
        }
        File tinker_server = new File(dataDir, "tinker_server");
        if(tinker_server.exists()){
            Log.e("wxd", " deleteWechatTinker " + tinker_server.getPath());
            FileUtils.deleteDir(tinker_server);
        }
    }

    private void setupUncaughtHandler() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        ThreadGroup newRoot = new RootThreadGroup(root);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            final List<ThreadGroup> groups = mirror.java.lang.ThreadGroup.groups.get(root);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (groups) {
                List<ThreadGroup> newGroups = new ArrayList<>(groups);
                newGroups.remove(newRoot);
                mirror.java.lang.ThreadGroup.groups.set(newRoot, newGroups);
                groups.clear();
                groups.add(newRoot);
                mirror.java.lang.ThreadGroup.groups.set(root, groups);
                for (ThreadGroup group : newGroups) {
                    mirror.java.lang.ThreadGroup.parent.set(group, newRoot);
                }
            }
        } else {
            final ThreadGroup[] groups = ThreadGroupN.groups.get(root);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (groups) {
                ThreadGroup[] newGroups = groups.clone();
                ThreadGroupN.groups.set(newRoot, newGroups);
                ThreadGroupN.groups.set(root, new ThreadGroup[]{newRoot});
                for (Object group : newGroups) {
                    ThreadGroupN.parent.set(group, newRoot);
                }
                ThreadGroupN.ngroups.set(root, 1);
            }
        }
    }

    @SuppressLint("SdCardPath")
    private void startIOUniformer() {
        ApplicationInfo info = mBoundApplication.appInfo;
        String packageName = info.packageName;
        int userId = VUserHandle.myUserId();
        File wifiMacAddressFile = deviceInfo.getWifiFile(userId);
        String wifiMacAddressPath = wifiMacAddressFile != null ? wifiMacAddressFile.getPath() : null;
        String dataDir = VEnvironment.getDataUserPackageDirectory(userId, packageName).getPath();//info.dataDir;
        if (!TextUtils.isEmpty(wifiMacAddressPath)) {
            NativeEngine.redirectDirectory("/sys/class/net/wlan0/address", wifiMacAddressPath);
            NativeEngine.redirectDirectory("/sys/class/net/eth0/address", wifiMacAddressPath);
            NativeEngine.redirectDirectory("/sys/class/net/wifi/address", wifiMacAddressPath);
        } else {
            //default wifi mac
        }
        File buildProp = new File(VEnvironment.getUserSystemDirectory(userId), "build.prop");
        if(buildProp.exists()) {
            NativeEngine.redirectDirectory("/system/build.prop", buildProp.getAbsolutePath());
        }

        NativeEngine.redirectDirectory("/data/data/" + packageName, dataDir);
        NativeEngine.redirectDirectory("/data/user/0/" + packageName, dataDir);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NativeEngine.redirectDirectory("/data/user_de/0/" + packageName, dataDir);
        }
        String libPath = VEnvironment.getAppLibDirectory(packageName).getAbsolutePath();
        NativeEngine.redirectDirectory("/data/data/" + packageName + "/lib/", libPath);
        NativeEngine.redirectDirectory("/data/user/0/" + packageName + "/lib/", libPath);

        if(isNotCopyApk() && !VirtualCore.get().isUseVirtualLibraryFiles(packageName, info.publicSourceDir)) {
            ApplicationInfo outside = null;
            try {
                outside = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(packageName, 0);
            } catch (Throwable e) {
                //ignore
            }
            if (outside != null && NativeLibraryHelperCompat.isSupportNative32(outside)) {
                String path = NativeLibraryHelperCompat.getNativeLibraryDir32(outside);
                if (path != null) {
                    NativeEngine.dlOpenWhitelist(path);
                }
            }
        }

        //safekey adapter
        String subPathData = "/Android/data/"+info.packageName;
        File[] efd = VEnvironment.getTFRoots();
        for(File f:efd){
            if(f==null)
                continue;
            String filename = f.getAbsolutePath();
            if(filename.contains("/emulated/0/"))
                continue;
            Log.e("lxf","XXX " + f.getAbsolutePath());
            String tfRoot = VEnvironment.getTFRoot(f.getAbsolutePath()).getAbsolutePath();
            NativeEngine.redirectDirectory(tfRoot+subPathData
                    ,VEnvironment.getTFVirtualRoot(tfRoot,subPathData).getAbsolutePath());
            Log.e("lxf","XXX " + tfRoot+subPathData);
            Log.e("lxf","XXX " + VEnvironment.getTFVirtualRoot(tfRoot,subPathData).getAbsolutePath());
        }

        VirtualStorageManager vsManager = VirtualStorageManager.get();
        vsManager.setVirtualStorage(info.packageName, userId, VEnvironment.getExternalStorageDirectory(userId).getAbsolutePath());
        String vsPath = vsManager.getVirtualStorage(info.packageName, userId);
        boolean enable = vsManager.isVirtualStorageEnable(info.packageName, userId);

        if (enable && vsPath != null) {
            File vsDirectory = new File(vsPath);
            if (vsDirectory.exists() || vsDirectory.mkdirs()) {
                HashSet<String> mountPoints = getMountPoints();
                for (String mountPoint : mountPoints) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (Environment.isExternalStorageRemovable(new File(mountPoint))) {
                            continue;
                        }
                    }
                    NativeEngine.redirectDirectory(mountPoint, vsPath);
                }
            }
        }

        NativeEngine.enableIORedirect(!VirtualCore.get().isDisableDlOpen(info.packageName));
    }

    @SuppressLint("SdCardPath")
    private HashSet<String> getMountPoints() {
        HashSet<String> mountPoints = new HashSet<>(3);
        mountPoints.add("/mnt/sdcard/");
        mountPoints.add("/sdcard/");
        String[] points = StorageManagerCompat.getAllPoints(VirtualCore.get().getContext());
        if (points != null) {
            Collections.addAll(mountPoints, points);
        }
        return mountPoints;

    }

    private Context createPackageContext(String packageName) {
        try {
            Context hostContext = VirtualCore.get().getContext();
            return hostContext.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            VirtualRuntime.crash(new RemoteException());
        }
        throw new RuntimeException();
    }

    private Object fixBoundApp(AppBindData data) {
        Object thread = VirtualCore.mainThread();
        Object boundApp = mirror.android.app.ActivityThread.mBoundApplication.get(thread);
        mirror.android.app.ActivityThread.AppBindData.appInfo.set(boundApp, data.appInfo);
        mirror.android.app.ActivityThread.AppBindData.processName.set(boundApp, data.processName);
        mirror.android.app.ActivityThread.AppBindData.instrumentationName.set(
                boundApp,
                new ComponentName(data.appInfo.packageName, Instrumentation.class.getName())
        );
        ActivityThread.AppBindData.providers.set(boundApp, data.providers);
        try {
            Reflect.on(data.info).set("mSecurityViolation", false);
        } catch (ReflectException e) {
            // ignore
        }
        return boundApp;
    }

    private void installContentProviders(Context app, List<ProviderInfo> providers) {
        long origId = Binder.clearCallingIdentity();
        Object mainThread = VirtualCore.mainThread();
        try {
            for (ProviderInfo cpi : providers) {
                try {
                    ActivityThread.installProvider(mainThread, app, cpi, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    @Override
    public IBinder acquireProviderClient(ProviderInfo info) {
        if (mTempLock != null) {
            mTempLock.block();
        }
        if(!isBound()) {
            VClient.get().bindApplication(info.packageName, info.processName);
        }
        IInterface provider = null;
        String[] authorities = info.authority.split(";");
        String authority = authorities.length == 0 ? info.authority : authorities[0];
        ContentResolver resolver = VirtualCore.get().getContext().getContentResolver();
        ContentProviderClient client = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                client = resolver.acquireUnstableContentProviderClient(authority);
            } else {
                client = resolver.acquireContentProviderClient(authority);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (client != null) {
            provider = mirror.android.content.ContentProviderClient.mContentProvider.get(client);
            client.release();
        }
        return provider != null ? provider.asBinder() : null;
    }

    private void fixInstalledProviders() {
        clearSettingProvider();
        Map clientMap = ActivityThread.mProviderMap.get(VirtualCore.mainThread());
        for (Object clientRecord : clientMap.values()) {
            if (BuildCompat.isOreo()) {
                IInterface provider = ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = ContentProviderHolderOreo.info.get(holder);
                if (!info.authority.startsWith(VASettings.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    ContentProviderHolderOreo.provider.set(holder, provider);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                IInterface provider = ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = IActivityManager.ContentProviderHolder.info.get(holder);
                if (!info.authority.startsWith(VASettings.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
            } else {
                String authority = ActivityThread.ProviderClientRecord.mName.get(clientRecord);
                IInterface provider = ActivityThread.ProviderClientRecord.mProvider.get(clientRecord);
                if (provider != null && !authority.startsWith(VASettings.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, authority, provider);
                    ActivityThread.ProviderClientRecord.mProvider.set(clientRecord, provider);
                }
            }
        }

    }

    private void clearSettingProvider() {
        Object cache;
        cache = Settings.System.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        cache = Settings.Secure.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && Settings.Global.TYPE != null) {
            cache = Settings.Global.sNameValueCache.get();
            if (cache != null) {
                clearContentProvider(cache);
            }
        }
    }

    private static void clearContentProvider(Object cache) {
        if (BuildCompat.isOreo()) {
            Object holder = Settings.NameValueCacheOreo.mProviderHolder.get(cache);
            if (holder != null) {
                Settings.ContentProviderHolder.mContentProvider.set(holder, null);
            }
        } else {
            Settings.NameValueCache.mContentProvider.set(cache, null);
        }
    }

    @Override
    public void finishActivity(IBinder token) {
        VActivityManager.get().finishActivity(token);
    }

    @Override
    public void closeAllLongSocket() throws RemoteException {
        NativeEngine.nativeCloseAllSocket();
    }

    @Override
    public void scheduleNewIntent(String creator, IBinder token, Intent intent) {
        NewIntentData data = new NewIntentData();
        data.creator = creator;
        data.token = token;
        data.intent = intent;
        sendMessage(NEW_INTENT, data);
    }

    @Override
    public void scheduleCreateService(IBinder token, ServiceInfo info) throws RemoteException {
        CreateServiceData data = new CreateServiceData();
        data.token = token;
        data.info = info;
        sendMessage(CREATE_SERVICE, data);
    }

    @Override
    public void scheduleBindService(IBinder token, Intent intent, boolean rebind) throws RemoteException {
        BindServiceData data = new BindServiceData();
        data.token = token;
        data.intent = intent;
        data.rebind = rebind;
        sendMessage(BIND_SERVICE, data);
    }

    @Override
    public void scheduleUnbindService(IBinder token, Intent intent) throws RemoteException {
        BindServiceData data = new BindServiceData();
        data.token = token;
        data.intent = intent;
        sendMessage(UNBIND_SERVICE, data);
    }

    @Override
    public void scheduleServiceArgs(IBinder token, int startId, Intent args) throws RemoteException {
        ServiceArgsData data = new ServiceArgsData();
        data.token = token;
        data.startId = startId;
        data.args = args;
        sendMessage(SERVICE_ARGS, data);
    }

    @Override
    public void scheduleStopService(IBinder token) throws RemoteException {
        sendMessage(STOP_SERVICE, token);
    }

    @Override
    public void scheduleReceiver(String processName, ComponentName component, Intent intent, PendingResultData resultData) {
        ReceiverData receiverData = new ReceiverData();
        receiverData.resultData = resultData;
        receiverData.intent = intent;
        receiverData.component = component;
        receiverData.processName = processName;
        sendMessage(RECEIVER, receiverData);
    }

    private void handleReceiver(ReceiverData data) {
        BroadcastReceiver.PendingResult result = data.resultData.build();
        try {
            if (!isBound()) {
                bindApplication(data.component.getPackageName(), data.processName);
            }
            Context context = mInitialApplication.getBaseContext();
            Context receiverContext = ContextImpl.getReceiverRestrictedContext.call(context);
            String className = data.component.getClassName();
            BroadcastReceiver receiver = (BroadcastReceiver) context.getClassLoader().loadClass(className).newInstance();
            mirror.android.content.BroadcastReceiver.setPendingResult.call(receiver, result);
            data.intent.setExtrasClassLoader(context.getClassLoader());
            if (data.intent.getComponent() == null) {
                data.intent.setComponent(data.component);
            }
            receiver.onReceive(receiverContext, data.intent);
            if (mirror.android.content.BroadcastReceiver.getPendingResult.call(receiver) != null) {
                result.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unable to start receiver " + data.component
                            + ": " + e.toString(), e);
        }
        VActivityManager.get().broadcastFinish(data.resultData);
    }

    private void handleCreateService(CreateServiceData data) {
        ServiceInfo info = data.info;
        if (!isBound()) {
            bindApplication(info.packageName, info.processName);
        }
        Application application = getCurrentApplication();
        Service service;
        try {
            service = (Service) application.getClassLoader().loadClass(info.name).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to instantiate service " + info.name
                            + ": " + e.toString(), e);
        }
        try {
            Context context = VirtualCore.get().getContext().createPackageContext(
                    data.info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            ContextImpl.setOuterContext.call(context, service);
            mirror.android.app.Service.attach.call(
                    service,
                    context,
                    VirtualCore.mainThread(),
                    info.name,
                    token,
                    application,
                    ActivityManagerNative.getDefault.call()
            );
            ContextFixer.fixContext(service);
            service.onCreate();
            mServices.put(data.token, service);
            VActivityManager.get().serviceDoneExecuting(data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to create service " + data.info.name
                            + ": " + e.toString(), e);
        }
    }

    private void handleBindService(BindServiceData data) {
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                if (!data.rebind) {
                    IBinder binder = s.onBind(data.intent);
                    VActivityManager.get().publishService(data.token, data.intent, binder);
                } else {
                    s.onRebind(data.intent);
                    VActivityManager.get().serviceDoneExecuting(
                            data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to bind to service " + s
                                + " with " + data.intent + ": " + e.toString(), e);
            }
        }
    }

    private void handleUnbindService(BindServiceData data) {
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                boolean doRebind = s.onUnbind(data.intent);
                if (doRebind) {
                    VActivityManager.get().unbindFinished(
                            data.token, data.intent, true);
                } else {
                    VActivityManager.get().serviceDoneExecuting(
                            data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to unbind to service " + s
                                + " with " + data.intent + ": " + e.toString(), e);
            }
        }
    }

    private void handleServiceArgs(ServiceArgsData data) {
        Service s = mServices.get(data.token);
        if (s != null) {
            try {
                if (data.args != null) {
                    data.args.setExtrasClassLoader(s.getClassLoader());
                }
                int res;
                if (!data.taskRemoved) {
                    res = s.onStartCommand(data.args, data.flags, data.startId);
                } else {
                    s.onTaskRemoved(data.args);
                    res = 0;
                }
                VActivityManager.get().serviceDoneExecuting(
                        data.token, SERVICE_DONE_EXECUTING_START, data.startId, res);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to start service " + s
                                + " with " + data.args + ": " + e.toString(), e);
            }
        }
    }

    private void handleStopService(IBinder token) {
        Service s = mServices.remove(token);
        if (s != null) {
            try {
                s.onDestroy();
                VActivityManager.get().serviceDoneExecuting(
                        token, SERVICE_DONE_EXECUTING_STOP, 0, 0);
            } catch (Exception e) {
                if (!mInstrumentation.onException(s, e)) {
                    throw new RuntimeException(
                            "Unable to stop service " + s
                                    + ": " + e.toString(), e);
                }
            }
        }
    }

    @Override
    public IBinder createProxyService(ComponentName component, IBinder binder) {
        return ProxyServiceFactory.getProxyService(getCurrentApplication(), component, binder);
    }

    @Override
    public String getDebugInfo() {
        return "process : " + VirtualRuntime.getProcessName() + "\n" +
                "initialPkg : " + VirtualRuntime.getInitialPackageName() + "\n" +
                "vuid : " + vuid;
    }

    private static class RootThreadGroup extends ThreadGroup {

        RootThreadGroup(ThreadGroup parent) {
            super(parent, "VA");
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            CrashHandler handler = VClient.gClient.crashHandler;
            if (handler != null) {
                handler.handleUncaughtException(t, e);
            } else {
                VLog.e("uncaught", e);
                System.exit(0);
            }
        }
    }

    private final class NewIntentData {
        String creator;
        IBinder token;
        Intent intent;
    }

    private final class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }

    private final class ReceiverData {
        PendingResultData resultData;
        Intent intent;
        ComponentName component;
        String processName;
    }

    static final class CreateServiceData {
        IBinder token;
        ServiceInfo info;
    }

    static final class BindServiceData {
        IBinder token;
        Intent intent;
        boolean rebind;
    }

    static final class ServiceArgsData {
        IBinder token;
        boolean taskRemoved;
        int startId;
        int flags;
        Intent args;
    }

    private class H extends Handler {

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEW_INTENT: {
                    handleNewIntent((NewIntentData) msg.obj);
                    break;
                }
                case RECEIVER: {
                    handleReceiver((ReceiverData) msg.obj);
                    break;
                }
                case CREATE_SERVICE: {
                    handleCreateService((CreateServiceData) msg.obj);
                    break;
                }
                case SERVICE_ARGS: {
                    handleServiceArgs((ServiceArgsData) msg.obj);
                    break;
                }
                case STOP_SERVICE: {
                    handleStopService((IBinder) msg.obj);
                    break;
                }
                case BIND_SERVICE: {
                    handleBindService((BindServiceData) msg.obj);
                    break;
                }
                case UNBIND_SERVICE: {
                    handleUnbindService((BindServiceData) msg.obj);
                    break;
                }
            }
        }
    }
}
