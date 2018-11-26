package com.lody.virtual.client.core;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.text.TextUtils;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.R;
import com.lody.virtual.client.VClient;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.TaskDescriptionDelegate;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.compat.UriCompat;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.ServiceCache;
import com.lody.virtual.server.bit64.V64BitHelper;
import com.lody.virtual.server.interfaces.IAppManager;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.lody.virtual.server.interfaces.IPackageObserver;

import com.xdja.zs.IAppPermissionCallback;
import com.xdja.zs.IControllerServiceCallback;
import com.xdja.zs.IVSCallback;
import com.xdja.zs.IUiCallback;
import com.xdja.zs.INotificationCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexFile;
import mirror.android.app.ActivityThread;

/**
 * @author Lody
 * @version 3.5
 */
public final class VirtualCore {

    public static final int GET_HIDDEN_APP = 0x00000001;
    private static final String TAG = VirtualCore.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static VirtualCore gCore = new VirtualCore();
    private final int myUid = Process.myUid();
    /**
     * Client Package Manager
     */
    private PackageManager unHookPackageManager;
    /**
     * Host package name
     */
    private String hostPkgName;
    /**
     * ActivityThread instance
     */
    private Object mainThread;
    private Context context;
    /**
     * Main ProcessName
     */
    private String mainProcessName;
    /**
     * Real Process Name
     */
    private String processName;
    private ProcessType processType;
    private boolean is64Bit;
    private IAppManager mService;
    private boolean isStartUp;
    private PackageInfo hostPkgInfo;
    private int systemPid, systemUid;
    private ConditionVariable initLock = new ConditionVariable();
    private AppCallback mAppCallback;
    private TaskDescriptionDelegate taskDescriptionDelegate;

    private VirtualCore() {
    }

    public boolean isDisableDlOpen(String packageName) {
        return getSettingRuleResult(SettingRule.DisableDlOpen, packageName);
    }

    public boolean isDisableNotCopyApk(String packageName) {
        return getSettingRuleResult(SettingRule.DisableNotCopyApk, packageName);
    }

    /**
     * check so for /data/data/app/lib is 64bit?
     */
    public boolean isUseOwnLibraryFiles(String packageName) {
        if(shouldRun64BitProcess(packageName)){
            return false;
        }
        return !getSettingRuleResult(SettingRule.UseOutsideLibraryFiles, packageName);
    }

    public boolean isUseRealDir(String packageName) {
        if (VASettings.USE_REAL_DATA_DIR) {
            return true;
        }
        return getSettingRuleResult(SettingRule.UseRealDataDir, packageName);
    }

    /**
     * @see SettingRule
     * @see #addSettingRule(SettingRule, String)
     * @see #addSettingRuleRegex(SettingRule, String)
     * @deprecated
     */
    public void setSettingHandler(Object settingHandler) {
    }

    public static VirtualCore get() {
        return gCore;
    }

    public static PackageManager getPM() {
        return get().getPackageManager();
    }

    public static Object mainThread() {
        return get().mainThread;
    }

    public ConditionVariable getInitLock() {
        return initLock;
    }

    public int myUid() {
        return myUid;
    }

    public int myUserId() {
        return VUserHandle.getUserId(myUid);
    }

    public AppCallback getAppCallback() {
        return mAppCallback == null ? AppCallback.EMPTY : mAppCallback;
    }

    /**
     * @see #setAppCallback
     * @deprecated
     */
    public void setComponentDelegate(AppCallback delegate) {
        setAppCallback(delegate);
    }

    public void setAppCallback(AppCallback delegate) {
        this.mAppCallback = delegate;
    }

    public void setCrashHandler(CrashHandler handler) {
        VClient.get().setCrashHandler(handler);
    }

    public TaskDescriptionDelegate getTaskDescriptionDelegate() {
        return taskDescriptionDelegate;
    }

    public void setTaskDescriptionDelegate(TaskDescriptionDelegate taskDescriptionDelegate) {
        this.taskDescriptionDelegate = taskDescriptionDelegate;
    }

    public int[] getGids() {
        return hostPkgInfo.gids;
    }

    public Context getContext() {
        return context;
    }

    public PackageManager getPackageManager() {
        return context.getPackageManager();
    }

    public boolean isSystemApp() {
        ApplicationInfo applicationInfo = getContext().getApplicationInfo();
        return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public String getHostPkg() {
        return hostPkgName;
    }

    public int getTargetSdkVersion() {
        return context.getApplicationInfo().targetSdkVersion;
    }

    public PackageManager getUnHookPackageManager() {
        return unHookPackageManager;
    }

    public boolean checkSelfPermission(String permission, boolean is64Bit) {
        if (is64Bit) {
            return PackageManager.PERMISSION_GRANTED == unHookPackageManager.checkPermission(permission, VASettings.PACKAGE_NAME_64BIT);
        } else {
            return PackageManager.PERMISSION_GRANTED == unHookPackageManager.checkPermission(permission, VASettings.PACKAGE_NAME);
        }
    }

    public void startup(Context context) throws Throwable {
        if (!isStartUp) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("VirtualCore.startup() must called in main thread.");
            }
            VLog.d(TAG, "startup:%s(%d)", com.lody.virtual.Build.VERSION_NAME, com.lody.virtual.Build.VERSION_CODE);
            Constants.SHORTCUT_ACTION = VASettings.PACKAGE_NAME + ".virtual.action.shortcut";
            VASettings.STUB_CP_AUTHORITY = VASettings.PACKAGE_NAME + "." + VASettings.STUB_CP_AUTHORITY;
            VASettings.STUB_CP_AUTHORITY_64BIT = VASettings.PACKAGE_NAME_64BIT + "." + VASettings.STUB_CP_AUTHORITY_64BIT;
            ServiceManagerNative.SERVICE_CP_AUTH = VASettings.PACKAGE_NAME + "." + ServiceManagerNative.SERVICE_DEF_AUTH;
            V64BitHelper.AUTHORITY = VASettings.PACKAGE_NAME_64BIT + "." + V64BitHelper.DEF_AUTHORITY;
            this.context = context;
            mainThread = ActivityThread.currentActivityThread.call();
            unHookPackageManager = context.getPackageManager();
            UriCompat.AUTH = VASettings.PACKAGE_NAME + ".virtual.fileprovider";//.virtual.proxy.provider

            hostPkgInfo = unHookPackageManager.getPackageInfo(VASettings.PACKAGE_NAME, PackageManager.GET_PROVIDERS);
            detectProcessType();
            if (is64BitEngine()) {
                VLog.e(TAG, "===========  64Bit Engine(%s) ===========", processType.name());
            }
            InvocationStubManager invocationStubManager = InvocationStubManager.getInstance();
            invocationStubManager.init();
            invocationStubManager.injectAll();
            ContextFixer.fixContext(context);
            isStartUp = true;
            if (initLock != null) {
                initLock.open();
                initLock = null;
            }
        }
    }

    public void waitForEngine() {
        ServiceManagerNative.ensureServerStarted();
    }

    public boolean isEngineLaunched() {
        String engineProcessName = getEngineProcessName();
        for (ActivityManager.RunningAppProcessInfo info : getRunningAppProcessesEx()) {
            if (info.processName.endsWith(engineProcessName)) {
                return true;
            }
        }
        return false;
    }

    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesEx() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = new ArrayList<>(am.getRunningAppProcesses());
        List<ActivityManager.RunningAppProcessInfo> list64 = V64BitHelper.getRunningAppProcess64();
        if (list64 != null) {
            list.addAll(list64);
        }
        return list;
    }

    public List<ActivityManager.RunningTaskInfo> getRunningTasksEx(int maxNum) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = new ArrayList<>(am.getRunningTasks(maxNum));
        List<ActivityManager.RunningTaskInfo> list64 = V64BitHelper.getRunningTasks64(maxNum);
        if (list64 != null) {
            list.addAll(list64);
        }
        return list;
    }

    public String getEngineProcessName() {
        return context.getString(R.string.engine_process_name);
    }

    public void initialize(VirtualInitializer initializer) {
        if (initializer == null) {
            throw new IllegalStateException("Initializer = NULL");
        }
        switch (processType) {
            case Main:
                initializer.onMainProcess();
                break;
            case VAppClient:
                initializer.onVirtualProcess();
                break;
            case Server:
                initializer.onServerProcess();
                break;
            case CHILD:
                initializer.onChildProcess();
                break;
        }
    }

    private void detectProcessType() {
        // Host package name
        hostPkgName = context.getApplicationInfo().packageName;
        // Main process name
        mainProcessName = context.getApplicationInfo().processName;
        // Current process name
        processName = ActivityThread.getProcessName.call(mainThread);
        is64Bit = hostPkgName.equals(VASettings.PACKAGE_NAME_64BIT);
        if (processName.equals(mainProcessName)) {
            processType = ProcessType.Main;
        } else if (processName.endsWith(Constants.SERVER_PROCESS_NAME)) {
            processType = ProcessType.Server;
        } else if (VActivityManager.get().isAppProcess(processName)) {
            processType = ProcessType.VAppClient;
        } else {
            processType = ProcessType.CHILD;
        }
        if (isVAppProcess()) {
            systemPid = VActivityManager.get().getSystemPid();
            systemUid = VActivityManager.get().getSystemUid();
        }
        if (isVAppProcess()) {
            if (!isAppInstalled(GmsSupport.GOOGLE_FRAMEWORK_PACKAGE)) {
                GmsSupport.remove(GmsSupport.GOOGLE_FRAMEWORK_PACKAGE);
                addVisibleOutsidePackage(GmsSupport.GOOGLE_FRAMEWORK_PACKAGE);
            }
        }
    }

    public boolean is64BitEngine() {
        return is64Bit;
    }

    private IAppManager getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object remote = getStubInterface();
                mService = LocalProxyUtils.genProxy(IAppManager.class, remote);
            }
        }
        return mService;
    }

    private Object getStubInterface() {
        return IAppManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.APP));
    }

    /**
     * @return If the current process is used to VA.
     */
    public boolean isVAppProcess() {
        return ProcessType.VAppClient == processType;
    }

    /**
     * @return If the current process is the main.
     */
    public boolean isMainProcess() {
        return ProcessType.Main == processType;
    }

    /**
     * @return If the current process is the child.
     */
    public boolean isChildProcess() {
        return ProcessType.CHILD == processType;
    }

    /**
     * @return If the current process is the server.
     */
    public boolean isServerProcess() {
        return ProcessType.Server == processType;
    }

    /**
     * @return the <em>actual</em> process name
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * @return the <em>Main</em> process name
     */
    public String getMainProcessName() {
        return mainProcessName;
    }

    /**
     * Optimize the Dalvik-Cache for the specified package.
     *
     * @param pkg package name
     * @throws IOException
     */
    @Deprecated
    public void preOpt(String pkg) throws IOException {
        InstalledAppInfo info = getInstalledAppInfo(pkg, 0);
        if (info != null && !info.notCopyApk) {
            DexFile.loadDex(info.apkPath, info.getOdexFile().getPath(), 0).close();
        }
    }

    /**
     * Check if the specified app running in foreground / background?
     *
     * @param packageName package name
     * @param userId      user id
     * @return if the specified app running in foreground / background.
     */
    public boolean isAppRunning(String packageName, int userId) {
        return VActivityManager.get().isAppRunning(packageName, userId);
    }

    public InstallResult installPackageSync(String apkPath, int flags) {
        final ConditionVariable lock = new ConditionVariable();
        final InstallResult[] out = new InstallResult[1];
        installPackage(apkPath, flags, new InstallCallback() {
            @Override
            public void onFinish(InstallResult result) {
                lock.open();
                out[0] = result;
            }
        });
        lock.block();
        return out[0];
    }

    @Deprecated
    public InstallResult installPackage(String apkPath, int flags) {
        return installPackageSync(apkPath, flags);
    }


    public interface InstallCallback {
        void onFinish(InstallResult result);
    }

    public void installPackage(String apkPath, int flags, final InstallCallback callback) {
        ResultReceiver receiver = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                resultData.setClassLoader(InstallResult.class.getClassLoader());
                if (callback != null) {
                    InstallResult res = resultData.getParcelable("result");
                    callback.onFinish(res);
                }
            }
        };
        try {
            getService().installPackage(apkPath, flags, receiver);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public InstallResult installPackageFromAsset(String asset, int flags) {
        InputStream inputStream = null;
        try {
            inputStream = getContext().getAssets().open(asset);
            return installPackageFromStream(inputStream, flags);
        } catch (Throwable e) {
            InstallResult res = new InstallResult();
            res.error = e.getMessage();
            return res;
        } finally {
            FileUtils.closeQuietly(inputStream);
        }
    }

    public InstallResult installPackageFromStream(InputStream inputStream, int flags) {
        try {
            File dir = getContext().getCacheDir();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File apkFile = new File(dir, "tmp_" + System.currentTimeMillis() + ".apk");
            FileUtils.writeToFile(inputStream, apkFile);
            InstallResult res = installPackageSync(apkFile.getAbsolutePath(), flags);
            apkFile.delete();
            return res;
        } catch (Throwable e) {
            InstallResult res = new InstallResult();
            res.error = e.getMessage();
            return res;
        }
    }

    public void addVisibleOutsidePackage(String pkg) {
        try {
            getService().addVisibleOutsidePackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void removeVisibleOutsidePackage(String pkg) {
        try {
            getService().removeVisibleOutsidePackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isOutsidePackageVisible(String pkg) {
        try {
            return getService().isOutsidePackageVisible(pkg);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppInstalled(String pkg) {
        try {
            return getService().isAppInstalled(pkg);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isPackageLaunchable(String packageName) {
        InstalledAppInfo info = getInstalledAppInfo(packageName, 0);
        return info != null
                && getLaunchIntent(packageName, info.getInstalledUsers()[0]) != null;
    }

    public Intent getLaunchIntent(String packageName, int userId) {
        VPackageManager pm = VPackageManager.get();
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.size() <= 0) {
            // reuse the intent instance
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName,
                ris.get(0).activityInfo.name);
        return intent;
    }

    public boolean createShortcut(int userId, String packageName, OnEmitShortcutListener listener) {
        return createShortcut(userId, packageName, null, listener);
    }

    public boolean createShortcut(int userId, String packageName, Intent splash, OnEmitShortcutListener listener) {
        InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
        if (setting == null) {
            return false;
        }
        ApplicationInfo appInfo = setting.getApplicationInfo(userId);
        PackageManager pm = context.getPackageManager();
        String name;
        Bitmap icon;
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            name = sequence.toString();
            icon = BitmapUtils.drawableToBitmap(appInfo.loadIcon(pm));
        } catch (Throwable e) {
            return false;
        }
        if (listener != null) {
            String newName = listener.getName(name);
            if (newName != null) {
                name = newName;
            }
            Bitmap newIcon = listener.getIcon(icon);
            if (newIcon != null) {
                icon = newIcon;
            }
        }
        Intent targetIntent = getLaunchIntent(packageName, userId);
        if (targetIntent == null) {
            return false;
        }
        Intent shortcutIntent = wrapperShortcutIntent(targetIntent, splash, packageName, userId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutInfo likeShortcut = null;
            likeShortcut = new ShortcutInfo.Builder(getContext(), packageName + "@" + userId)
                    .setLongLabel(name)
                    .setShortLabel(name)
                    .setIcon(Icon.createWithBitmap(icon))
                    .setIntent(shortcutIntent)
                    .build();
            ShortcutManager shortcutManager = getContext().getSystemService(ShortcutManager.class);
            if (shortcutManager != null) {
                try {
                    shortcutManager.requestPinShortcut(likeShortcut,
                            PendingIntent.getActivity(getContext(), packageName.hashCode() + userId, shortcutIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender());
                } catch (Throwable e) {
                    return false;
                }
            }
        } else {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapUtils.warrperIcon(icon, 256, 256));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            try {
                context.sendBroadcast(addIntent);
            } catch (Throwable e) {
                return false;
            }
        }
        return true;
    }

    public boolean removeShortcut(int userId, String packageName, Intent splash, OnEmitShortcutListener listener) {
        InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
        if (setting == null) {
            return false;
        }
        ApplicationInfo appInfo = setting.getApplicationInfo(userId);
        PackageManager pm = context.getPackageManager();
        String name;
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            name = sequence.toString();
        } catch (Throwable e) {
            return false;
        }
        if (listener != null) {
            String newName = listener.getName(name);
            if (newName != null) {
                name = newName;
            }
        }
        Intent targetIntent = getLaunchIntent(packageName, userId);
        if (targetIntent == null) {
            return false;
        }
        Intent shortcutIntent = wrapperShortcutIntent(targetIntent, splash, packageName, userId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        } else {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        }
        return true;
    }

    /**
     * @param intent target activity
     * @param splash loading activity
     * @param userId userId
     */
    public Intent wrapperShortcutIntent(Intent intent, Intent splash, String packageName, int userId) {
        Intent shortcutIntent = new Intent();
        shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
        shortcutIntent.setAction(Constants.SHORTCUT_ACTION);
        shortcutIntent.setPackage(getHostPkg());
        if (splash != null) {
            shortcutIntent.putExtra("_VA_|_splash_", splash.toUri(0));
        }
        shortcutIntent.putExtra("_VA_|_pkg_", packageName);
//        shortcutIntent.putExtra("_VA_|_intent_", (String)null);//targetIntent);
        shortcutIntent.putExtra("_VA_|_uri_", intent.toUri(0));
        shortcutIntent.putExtra("_VA_|_user_id_", userId);
        return shortcutIntent;
    }

    public abstract static class UiCallback extends IUiCallback.Stub {
    }

    public abstract static class VSCallback extends IVSCallback.Stub {
    }

    public abstract static class AppPermissionCallback extends IAppPermissionCallback.Stub {
    }
    public abstract static class NotificationCallback extends INotificationCallback.Stub {
    }

    public abstract static class ControllerServiceCallback extends IControllerServiceCallback.Stub{

    }


    public void setUiCallback(Intent intent, IUiCallback callback) {
        if (callback != null) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, "_VA_|_ui_callback_", callback.asBinder());
            intent.putExtra("_VA_|_sender_", bundle);
        }
    }

    public InstalledAppInfo getInstalledAppInfo(String pkg, int flags) {
        try {
            return getService().getInstalledAppInfo(pkg, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getInstalledAppCount() {
        try {
            return getService().getInstalledAppCount();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isStartup() {
        return isStartUp;
    }

    public boolean uninstallPackageAsUser(String pkgName, int userId) {
        try {
            return getService().uninstallPackageAsUser(pkgName, userId);
        } catch (RemoteException e) {
            // Ignore
        }
        return false;
    }

    public boolean uninstallPackage(String pkgName) {
        try {
            return getService().uninstallPackage(pkgName);
        } catch (RemoteException e) {
            // Ignore
        }
        return false;
    }

    public Resources getResources(String pkg) throws Resources.NotFoundException {
        InstalledAppInfo installedAppInfo = getInstalledAppInfo(pkg, 0);
        if (installedAppInfo != null) {
            AssetManager assets = mirror.android.content.res.AssetManager.ctor.newInstance();
            mirror.android.content.res.AssetManager.addAssetPath.call(assets, installedAppInfo.apkPath);
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        throw new Resources.NotFoundException(pkg);
    }

    public synchronized ActivityInfo resolveActivityInfo(Intent intent, int userId) {
        ActivityInfo activityInfo = null;
        if (intent.getComponent() == null) {
            ResolveInfo resolveInfo = VPackageManager.get().resolveIntent(intent, intent.getType(), 0, userId);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                activityInfo = resolveInfo.activityInfo;
                intent.setClassName(activityInfo.packageName, activityInfo.name);
            }
        } else {
            activityInfo = resolveActivityInfo(intent.getComponent(), userId);
        }
        if (activityInfo != null) {
            if (activityInfo.targetActivity != null) {
                ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
                activityInfo = VPackageManager.get().getActivityInfo(componentName, 0, userId);
                intent.setComponent(componentName);
            }
        }
        return activityInfo;
    }

    public ActivityInfo resolveActivityInfo(ComponentName componentName, int userId) {
        return VPackageManager.get().getActivityInfo(componentName, 0, userId);
    }

    public ServiceInfo resolveServiceInfo(Intent intent, int userId) {
        ServiceInfo serviceInfo = null;
        ResolveInfo resolveInfo = VPackageManager.get().resolveService(intent, intent.getType(), 0, userId);
        if (resolveInfo != null) {
            serviceInfo = resolveInfo.serviceInfo;
        }
        return serviceInfo;
    }

    public void killApp(String pkg, int userId) {
        VActivityManager.get().killAppByPkg(pkg, userId);
    }

    public void killAllApps() {
        VActivityManager.get().killAllApps();
    }

    public List<InstalledAppInfo> getInstalledApps(int flags) {
        try {
            return getService().getInstalledApps(flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public List<InstalledAppInfo> getInstalledAppsAsUser(int userId, int flags) {
        try {
            return getService().getInstalledAppsAsUser(userId, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void clearAppRequestListener() {
        try {
            getService().clearAppRequestListener();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void scanApps() {
        try {
            getService().scanApps();
        } catch (RemoteException e) {
            // Ignore
        }
    }

    public IAppRequestListener getAppRequestListener() {
        try {
            return getService().getAppRequestListener();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setAppRequestListener(final AppRequestListener listener) {
        IAppRequestListener inner = new IAppRequestListener.Stub() {
            @Override
            public void onRequestInstall(final String path) {
                VirtualRuntime.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRequestInstall(path);
                    }
                });
            }

            @Override
            public void onRequestUninstall(final String pkg) {
                VirtualRuntime.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRequestUninstall(pkg);
                    }
                });
            }
        };
        try {
            getService().setAppRequestListener(inner);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isPackageLaunched(int userId, String packageName) {
        try {
            return getService().isPackageLaunched(userId, packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setPackageHidden(int userId, String packageName, boolean hidden) {
        try {
            getService().setPackageHidden(userId, packageName, hidden);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean installPackageAsUser(int userId, String packageName) {
        try {
            return getService().installPackageAsUser(userId, packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppInstalledAsUser(int userId, String packageName) {
        try {
            return getService().isAppInstalledAsUser(userId, packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int[] getPackageInstalledUsers(String packageName) {
        try {
            return getService().getPackageInstalledUsers(packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public abstract static class PackageObserver extends IPackageObserver.Stub {
    }

    public void registerObserver(IPackageObserver observer) {
        try {
            getService().registerObserver(observer);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterObserver(IPackageObserver observer) {
        try {
            getService().unregisterObserver(observer);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isOutsideInstalled(String packageName) {
        try {
            return unHookPackageManager.getApplicationInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }
        return false;
    }

    public boolean is64BitEngineInstalled() {
        return isOutsideInstalled(VASettings.PACKAGE_NAME_64BIT);
    }

    /**
     * @param rule
     * @param packageNameOrWord packageName or regex
     *                          com.kk.demo or com.kk.demo.*  com.*.fgo
     *                          other regex:
     * @see SettingRule#DisableDlOpen
     * @see SettingRule#DisableNotCopyApk
     * @see SettingRule#UseOutsideLibraryFiles
     * @see SettingRule#UseRealDataDir
     * @see #addSettingRuleRegex(SettingRule, String)
     */
    public void addSettingRule(SettingRule rule, String packageNameOrWord) {
        if (packageNameOrWord == null) return;
        if (packageNameOrWord.contains("*")) {
            addSettingRule(rule, wrapperRegex(packageNameOrWord), true);
            return;
        }
        addSettingRule(rule, packageNameOrWord, false);
    }

    /**
     * @param rule com.kk.demo
     *             com.kk.demo.1
     *             com.kk.demo.2
     * @param word com.kk.demo*
     * @see SettingRule#DisableDlOpen
     * @see SettingRule#DisableNotCopyApk
     * @see SettingRule#UseOutsideLibraryFiles
     * @see SettingRule#UseRealDataDir
     */
    public void addSettingRuleRegex(SettingRule rule, String word) {
        addSettingRule(rule, wrapperRegex(word), true);
    }

    /**
     * @param rule
     * @param word regex=true:packageName.matchs(word);regex=false:packageName.equals(word)
     * @see SettingRule#DisableDlOpen
     * @see SettingRule#DisableNotCopyApk
     * @see SettingRule#UseOutsideLibraryFiles
     * @see SettingRule#UseRealDataDir
     */
    public void addSettingRule(SettingRule rule, String word, boolean regex) {
        if (TextUtils.isEmpty(word)) {
            return;
        }
        try {
            getService().addSettingRule(rule.ordinal(), word, regex);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    /**
     * @param rule
     * @param word regex=true:packageName.matchs(word);regex=false:packageName.equals(word)
     * @see SettingRule#DisableDlOpen
     * @see SettingRule#DisableNotCopyApk
     * @see SettingRule#UseOutsideLibraryFiles
     * @see SettingRule#UseRealDataDir
     */
    public void removeSettingRule(SettingRule rule, String word, boolean regex) {
        if (TextUtils.isEmpty(word)) {
            return;
        }
        try {
            getService().removeSettingRule(rule.ordinal(), word, regex);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    private String wrapperRegex(String word) {
        return word.replace(".", "\\.")
                .replace("*", "[\\.a-zA-Z0-9_]*");
    }

    /**
     * @param rule
     * @see SettingRule#DisableDlOpen
     * @see SettingRule#DisableNotCopyApk
     * @see SettingRule#UseOutsideLibraryFiles
     * @see SettingRule#UseRealDataDir
     */
    public boolean getSettingRuleResult(SettingRule rule, String packageName) {
        if (rule == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return getService().inSettingRule(rule.ordinal(), packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public boolean shouldRun64BitProcess(String packageName) {
        if(!is64BitEngineInstalled()){
            return false;
        }
        try {
            return getService().isShouldRun64BitProcess(packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getSystemPid() {
        return systemPid;
    }

    public int getSystemUid() {
        return systemUid;
    }

    /**
     * Process type
     */
    private enum ProcessType {
        /**
         * Server process
         */
        Server,
        /**
         * Virtual app process
         */
        VAppClient,
        /**
         * Main process
         */
        Main,
        /**
         * Child process
         */
        CHILD
    }

    public interface AppRequestListener {
        void onRequestInstall(String path);

        void onRequestUninstall(String pkg);
    }

    public interface OnEmitShortcutListener {
        Bitmap getIcon(Bitmap originIcon);

        String getName(String originName);
    }

    public static abstract class VirtualInitializer {
        public void onMainProcess() {
        }

        public void onVirtualProcess() {
        }

        public void onServerProcess() {
        }

        public void onChildProcess() {
        }
    }
}
