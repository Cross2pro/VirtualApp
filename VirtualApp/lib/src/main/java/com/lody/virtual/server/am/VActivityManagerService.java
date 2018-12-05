package com.lody.virtual.server.am;

import android.app.ActivityManager;
import android.app.IServiceConnection;
import android.app.IStopUserCallback;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.lody.virtual.client.IVClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.ipc.ProviderCall;
import com.lody.virtual.client.ipc.VNotificationManager;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.compat.PermissionCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VBinder;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.BadgerInfo;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.ClientConfig;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.VParceledListSlice;
import com.lody.virtual.server.bit64.V64BitHelper;
import com.lody.virtual.server.interfaces.IActivityManager;
import com.lody.virtual.server.pm.PackageCacheManager;
import com.lody.virtual.server.pm.PackageSetting;
import com.lody.virtual.server.pm.VAppManagerService;
import com.lody.virtual.server.pm.VPackageManagerService;
import com.lody.virtual.server.secondary.BinderDelegateService;
import com.xdja.zs.controllerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mirror.android.app.IServiceConnectionO;

import static com.lody.virtual.os.VBinder.getCallingPid;
import static com.lody.virtual.os.VUserHandle.getUserId;

/**
 * @author Lody
 */
public class VActivityManagerService extends IActivityManager.Stub {

    private static final Singleton<VActivityManagerService> sService = new Singleton<VActivityManagerService>() {
        @Override
        protected VActivityManagerService create() {
            return new VActivityManagerService();
        }
    };
    private static final String TAG = VActivityManagerService.class.getSimpleName();
    private final SparseArray<ProcessRecord> mPidsSelfLocked = new SparseArray<ProcessRecord>();
    private final ActivityStack mActivityStack = new ActivityStack(this);
    private final Set<ServiceRecord> mHistory = new HashSet<ServiceRecord>();
    private final ProcessMap<ProcessRecord> mProcessNames = new ProcessMap<>();
    private final ProcessMap<ProcessRecord> mPendingProcessNames = new ProcessMap<>();
    private final Map<IBinder, IntentSenderData> mIntentSenderMap = new HashMap<>();
    private final List<ProcessRecord> mPendingProcessLocked = Collections.synchronizedList(new ArrayList<ProcessRecord>());
    private NotificationManager nm = (NotificationManager) VirtualCore.get().getContext()
            .getSystemService(Context.NOTIFICATION_SERVICE);
    private final Map<String, Boolean> sIdeMap = new HashMap<>();
    private boolean mResult;

    private ActivityManager am = (ActivityManager) VirtualCore.get().getContext()
                        .getSystemService(Context.ACTIVITY_SERVICE);

    public static VActivityManagerService get() {
        return sService.get();
    }

    private static ServiceInfo resolveServiceInfo(Intent service, int userId) {
        if (service != null) {
            return VirtualCore.get().resolveServiceInfo(service, userId);
        }
        return null;
    }


    @Override
    public int startActivity(Intent intent, ActivityInfo info, IBinder resultTo, Bundle options, String resultWho, int requestCode, int userId) {
        synchronized (this) {
            return mActivityStack.startActivityLocked(userId, intent, info, resultTo, options, resultWho, requestCode, VBinder.getCallingUid());
        }
    }

    @Override
    public int startActivities(Intent[] intents, String[] resolvedTypes, IBinder token, Bundle options, int userId) {
        synchronized (this) {
            ActivityInfo[] infos = new ActivityInfo[intents.length];
            for (int i = 0; i < intents.length; i++) {
                ActivityInfo ai = VirtualCore.get().resolveActivityInfo(intents[i], userId);
                if (ai == null) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }
                infos[i] = ai;
            }
            return mActivityStack.startActivitiesLocked(userId, intents, infos, resolvedTypes, token, options, VBinder.getCallingUid());
        }
    }


    @Override
    public int getSystemPid() {
        return Process.myPid();
    }

    @Override
    public int getSystemUid() {
        return Process.myUid();
    }

    @Override
    public void onActivityCreated(IBinder record, IBinder token, int taskId) {
        int pid = Binder.getCallingPid();
        ProcessRecord targetApp;
        synchronized (mPidsSelfLocked) {
            targetApp = findProcessLocked(pid);
        }
        if (targetApp != null) {
            mActivityStack.onActivityCreated(targetApp, token, taskId, (ActivityRecord) record);
        }
    }

    @Override
    public void onActivityResumed(int userId, IBinder token) {
        mActivityStack.onActivityResumed(userId, token);
    }

    @Override
    public boolean onActivityDestroyed(int userId, IBinder token) {
        ActivityRecord r = mActivityStack.onActivityDestroyed(userId, token);
        return r != null;
    }

    @Override
    public void onActivityFinish(int userId, IBinder token) {
        mActivityStack.onActivityFinish(userId, token);
    }

    @Override
    public AppTaskInfo getTaskInfo(int taskId) {
        return mActivityStack.getTaskInfo(taskId);
    }

    @Override
    public String getPackageForToken(int userId, IBinder token) {
        return mActivityStack.getPackageForToken(userId, token);
    }

    @Override
    public ComponentName getActivityClassForToken(int userId, IBinder token) {
        return mActivityStack.getActivityClassForToken(userId, token);
    }


    private void processDead(ProcessRecord record) {
        synchronized (mHistory) {
            Iterator<ServiceRecord> iterator = mHistory.iterator();
            while (iterator.hasNext()) {
                ServiceRecord r = iterator.next();
                if (r.process != null && r.process.pid == record.pid) {
                    iterator.remove();
                }
            }
            mActivityStack.processDied(record);
        }
    }

    public void finishAllActivity(ProcessRecord record) {
        mActivityStack.finishAllActivity(record);
    }


    @Override
    public IBinder acquireProviderClient(int userId, ProviderInfo info) {
        String processName = info.processName;
        ProcessRecord r = startProcessIfNeedLocked(processName, userId, info.packageName, -1, VBinder.getCallingUid());
        if (r != null && r.client.asBinder().isBinderAlive()) {
            try {
                return r.client.acquireProviderClient(info);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void addOrUpdateIntentSender(IntentSenderData sender, int userId) {
        if (sender == null || sender.token == null) {
            return;
        }

        synchronized (mIntentSenderMap) {
            IntentSenderData data = mIntentSenderMap.get(sender.token);
            if (data == null) {
                mIntentSenderMap.put(sender.token, sender);
            } else {
                data.replace(sender);
            }
        }
    }

    @Override
    public void removeIntentSender(IBinder token) {
        if (token != null) {
            synchronized (mIntentSenderMap) {
                mIntentSenderMap.remove(token);
            }
        }
    }

    @Override
    public IntentSenderData getIntentSender(IBinder token) {
        if (token != null) {
            synchronized (mIntentSenderMap) {
                return mIntentSenderMap.get(token);
            }
        }
        return null;
    }

    @Override
    public ComponentName getCallingActivity(int userId, IBinder token) {
        return mActivityStack.getCallingActivity(userId, token);
    }

    @Override
    public String getCallingPackage(int userId, IBinder token) {
        return mActivityStack.getCallingPackage(userId, token);
    }

    private void addRecord(ServiceRecord r) {
        synchronized (mHistory) {
            mHistory.add(r);
        }
    }

    private ServiceRecord findRecordLocked(int userId, ServiceInfo serviceInfo) {
        synchronized (mHistory) {
            for (ServiceRecord r : mHistory) {
                // If service is not created, and bindService with the flag that is
                // not BIND_AUTO_CREATE, r.process is null
                if ((r.process == null || r.process.userId == userId)
                        && ComponentUtils.isSameComponent(serviceInfo, r.serviceInfo)) {
                    return r;
                }
            }
            return null;
        }
    }

    private ServiceRecord findRecordLocked(IServiceConnection connection) {
        synchronized (mHistory) {
            for (ServiceRecord r : mHistory) {
                if (r.containConnection(connection)) {
                    return r;
                }
            }
            return null;
        }
    }


    @Override
    public ComponentName startService(Intent service, String resolvedType, int userId) {
        synchronized (this) {
            return startServiceCommonLocked(service, true, userId);
        }
    }

    private ComponentName startServiceCommonLocked(Intent service,
                                                   boolean scheduleServiceArgs, int userId) {
        ServiceInfo serviceInfo = resolveServiceInfo(service, userId);
        if (serviceInfo == null) {
            return null;
        }
        ProcessRecord targetApp = startProcessIfNeedLocked(ComponentUtils.getProcessName(serviceInfo),
                userId,
                serviceInfo.packageName, -1, VBinder.getCallingUid());

        if (targetApp == null) {
            VLog.e(TAG, "Unable to start new process (" + ComponentUtils.toComponentName(serviceInfo) + ").");
            return null;
        }
        ServiceRecord r = findRecordLocked(userId, serviceInfo);
        if (r == null) {
            r = new ServiceRecord();
            r.startId = 0;
            r.activeSince = SystemClock.elapsedRealtime();
            r.process = targetApp;
            r.serviceInfo = serviceInfo;
            try {
                targetApp.client.scheduleCreateService(r, r.serviceInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            startShadowService(targetApp);
            addRecord(r);
        }
        r.lastActivityTime = SystemClock.uptimeMillis();
        if (scheduleServiceArgs) {
            r.startId++;
            try {
                targetApp.client.scheduleServiceArgs(r, r.startId, service);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return ComponentUtils.toComponentName(serviceInfo);
    }

    private void startShadowService(ProcessRecord app) {
        String serviceName = StubManifest.getStubServiceName(app.vpid);
        Intent intent = new Intent();
        intent.setClassName(StubManifest.getStubPackageName(app.is64bit), serviceName);
        try {
            VirtualCore.get().getContext().bindService(intent, app.conn, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int stopService(IBinder caller, Intent service, String resolvedType, int userId) {
        synchronized (this) {
            ServiceInfo serviceInfo = resolveServiceInfo(service, userId);
            if (serviceInfo == null) {
                return 0;
            }
            ServiceRecord r = findRecordLocked(userId, serviceInfo);
            if (r == null) {
                return 0;
            }
            stopServiceCommon(r, ComponentUtils.toComponentName(serviceInfo));
            return 1;
        }
    }

    @Override
    public boolean stopServiceToken(ComponentName className, IBinder token, int startId, int userId) {
        synchronized (this) {
            ServiceRecord r = (ServiceRecord) token;
            if (r != null && (r.startId == startId || startId == -1)) {
                stopServiceCommon(r, className);
                return true;
            }

            return false;
        }
    }

    private void stopServiceCommon(ServiceRecord r, ComponentName className) {
        for (ServiceRecord.IntentBindRecord bindRecord : r.bindings) {
            for (IServiceConnection connection : bindRecord.connections) {
                // Report to all of the connections that the service is no longer
                // available.
                try {
                    if (BuildCompat.isOreo()) {
                        IServiceConnectionO.connected.call(connection, className, null, true);
                    } else {
                        connection.connected(className, null);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            try {
                r.process.client.scheduleUnbindService(r, bindRecord.intent);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        try {
            r.process.client.scheduleStopService(r);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mHistory.remove(r);
    }

    @Override
    public int bindService(IBinder caller, IBinder token, Intent service, String resolvedType,
                           IServiceConnection connection, int flags, int userId) {
        synchronized (this) {
            ServiceInfo serviceInfo = resolveServiceInfo(service, userId);
            if (serviceInfo == null) {
                return 0;
            }
            ServiceRecord r = findRecordLocked(userId, serviceInfo);
            boolean firstLaunch = r == null;
            if (firstLaunch) {
                if ((flags & Context.BIND_AUTO_CREATE) != 0) {
                    startServiceCommonLocked(service, false, userId);
                    r = findRecordLocked(userId, serviceInfo);
                }
            }
            if (r == null) {
                return 0;
            }
            ServiceRecord.IntentBindRecord boundRecord = r.peekBinding(service);

            if (boundRecord != null && boundRecord.binder != null && boundRecord.binder.isBinderAlive()) {
                if (boundRecord.doRebind) {
                    try {
                        r.process.client.scheduleBindService(r, service, true);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                ComponentName componentName = new ComponentName(r.serviceInfo.packageName, r.serviceInfo.name);
                connectServiceLocked(connection, componentName, boundRecord, false);
            } else {
                try {
                    r.process.client.scheduleBindService(r, service, false);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            r.lastActivityTime = SystemClock.uptimeMillis();
            r.addToBoundIntent(service, connection);
            return 1;
        }
    }


    @Override
    public boolean unbindService(IServiceConnection connection, int userId) {
        synchronized (this) {
            ServiceRecord r = findRecordLocked(connection);
            if (r == null) {
                return false;
            }

            for (ServiceRecord.IntentBindRecord bindRecord : r.bindings) {
                if (!bindRecord.containConnection(connection)) {
                    continue;
                }
                bindRecord.removeConnection(connection);
                try {
                    r.process.client.scheduleUnbindService(r, bindRecord.intent);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            if (r.startId <= 0 && r.getConnectionCount() <= 0) {
                try {
                    r.process.client.scheduleStopService(r);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    synchronized (mHistory) {
                        mHistory.remove(r);
                    }
                }
            }
            return true;
        }
    }

    @Override
    public void unbindFinished(IBinder token, Intent service, boolean doRebind, int userId) {
        synchronized (this) {
            ServiceRecord r = (ServiceRecord) token;
            if (r != null) {
                ServiceRecord.IntentBindRecord boundRecord = r.peekBinding(service);
                if (boundRecord != null) {
                    boundRecord.doRebind = doRebind;
                }
            }
        }
    }


    @Override
    public boolean isVAServiceToken(IBinder token) {
        return token instanceof ServiceRecord;
    }


    @Override
    public void serviceDoneExecuting(IBinder token, int type, int startId, int res, int userId) {
        synchronized (this) {
            ServiceRecord r = (ServiceRecord) token;
            if (r == null) {
                return;
            }
            if (ActivityManagerCompat.SERVICE_DONE_EXECUTING_STOP == type) {
                synchronized (mHistory) {
                    mHistory.remove(r);
                }
            }
        }
    }

    @Override
    public IBinder peekService(Intent service, String resolvedType, int userId) {
        synchronized (this) {
            ServiceInfo serviceInfo = resolveServiceInfo(service, userId);
            if (serviceInfo == null) {
                return null;
            }
            ServiceRecord r = findRecordLocked(userId, serviceInfo);
            if (r != null) {
                ServiceRecord.IntentBindRecord boundRecord = r.peekBinding(service);
                if (boundRecord != null) {
                    return boundRecord.binder;
                }
            }
            return null;
        }
    }

    @Override
    public void publishService(IBinder token, Intent intent, IBinder service, int userId) {
        synchronized (this) {
            ServiceRecord r = (ServiceRecord) token;
            if (r != null) {
                ServiceRecord.IntentBindRecord boundRecord = r.peekBinding(intent);
                if (boundRecord != null) {
                    boundRecord.binder = service;
                    for (IServiceConnection conn : boundRecord.connections) {
                        ComponentName component = ComponentUtils.toComponentName(r.serviceInfo);
                        connectServiceLocked(conn, component, boundRecord, false);
                    }
                }
            }
        }
    }

    private void connectServiceLocked(IServiceConnection conn, ComponentName component, ServiceRecord.IntentBindRecord r, boolean dead) {
        try {
            BinderDelegateService delegateService = new BinderDelegateService(component, r.binder);
            if (BuildCompat.isOreo()) {
                IServiceConnectionO.connected.call(conn, component, delegateService, dead);
            } else {
                conn.connected(component, delegateService);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public VParceledListSlice<ActivityManager.RunningServiceInfo> getServices(int maxNum, int flags, int userId) {
        synchronized (mHistory) {
            List<ActivityManager.RunningServiceInfo> services = new ArrayList<>(mHistory.size());
            for (ServiceRecord r : mHistory) {
                if (r.process.userId != userId) {
                    continue;
                }
                ActivityManager.RunningServiceInfo info = new ActivityManager.RunningServiceInfo();
                info.uid = r.process.vuid;
                info.pid = r.process.pid;
                ProcessRecord processRecord;
                synchronized (mPidsSelfLocked) {
                    processRecord = findProcessLocked(r.process.pid);
                }
                if (processRecord != null) {
                    info.process = processRecord.processName;
                    info.clientPackage = processRecord.info.packageName;
                }
                info.activeSince = r.activeSince;
                info.lastActivityTime = r.lastActivityTime;
                info.clientCount = r.getClientCount();
                info.service = ComponentUtils.toComponentName(r.serviceInfo);
                info.started = r.startId > 0;
                services.add(info);
            }
            return new VParceledListSlice<>(services);
        }
    }

    @Override
    public void setServiceForeground(ComponentName className, IBinder token, int id, Notification notification,
                                     boolean removeNotification, int userId) {
        ServiceRecord r = (ServiceRecord) token;
        if (r != null) {
            if (id != 0) {
                if (notification == null) {
                    throw new IllegalArgumentException("null notification");
                }
                if (r.foregroundId != id) {
                    if (r.foregroundId != 0) {
                        cancelNotification(userId, r.foregroundId, r.serviceInfo.packageName);
                    }
                    r.foregroundId = id;
                }
                r.foregroundNoti = notification;
                postNotification(userId, id, r.serviceInfo.packageName, notification);
            } else {
                if (removeNotification) {
                    cancelNotification(userId, r.foregroundId, r.serviceInfo.packageName);
                    r.foregroundId = 0;
                    r.foregroundNoti = null;
                }
            }
        }
    }

    private void cancelNotification(int userId, int id, String pkg) {
        id = VNotificationManager.get().dealNotificationId(id, pkg, null, userId);
        String tag = VNotificationManager.get().dealNotificationTag(id, pkg, null, userId);
        nm.cancel(tag, id);
    }

    private void postNotification(int userId, int id, String pkg, Notification notification) {
        id = VNotificationManager.get().dealNotificationId(id, pkg, null, userId);
        String tag = VNotificationManager.get().dealNotificationTag(id, pkg, null, userId);
        VNotificationManager.get().addNotification(id, tag, pkg, userId);
        try {
            nm.notify(tag, id, notification);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processRestarted(String packageName, String processName, int userId) {
        int callingVUid = VBinder.getCallingUid();
        int callingPid = VBinder.getCallingPid();
        synchronized (this) {
            ProcessRecord app;
            synchronized (mPidsSelfLocked) {
                app = findProcessLocked(callingPid);
            }
            if (app == null) {
                String stubProcessName = getProcessName(callingPid);
                if (stubProcessName == null) {
                    return;
                }
                int vpid = parseVPid(stubProcessName);
                if (vpid != -1) {
                    startProcessIfNeedLocked(processName, userId, packageName, vpid, callingVUid);
                }
            }
        }
    }

    private int parseVPid(String stubProcessName) {
        String prefix;
        if (stubProcessName == null) {
            return -1;
        } else if (stubProcessName.startsWith(StubManifest.PACKAGE_NAME_64BIT)) {
            prefix = StubManifest.PACKAGE_NAME_64BIT + ":p";
        } else if (stubProcessName.startsWith(StubManifest.PACKAGE_NAME)) {
            prefix = VirtualCore.get().getHostPkg() + ":p";
        } else {
            return -1;
        }
        if (stubProcessName.startsWith(prefix)) {
            try {
                return Integer.parseInt(stubProcessName.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return -1;
    }


    private String getProcessName(int pid) {
        for (ActivityManager.RunningAppProcessInfo info : VirtualCore.get().getRunningAppProcessesEx()) {
            if (info.pid == pid) {
                return info.processName;
            }
        }
        return null;
    }


    private boolean attachClient(final ProcessRecord app, final IBinder clientBinder) {
        IVClient client = IVClient.Stub.asInterface(clientBinder);
        if (client == null) {
            app.kill();
            return false;
        }
        try {
            clientBinder.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    clientBinder.unlinkToDeath(this, 0);
                    onProcessDead(app);
                }
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        app.client = client;
        notifyAppProcessStatus(app, 0, true);
        synchronized (mProcessNames) {
            mProcessNames.put(app.processName, app.vuid, app);
        }
        synchronized (mPidsSelfLocked) {
            mPidsSelfLocked.put(app.pid, app);
        }
        return true;
    }

    private void notifyAppProcessStatus(ProcessRecord app, int uid, boolean status){
        try{
            if(status == true) {
                controllerManager.get().getService().appProcessStart(app.info.packageName, app.processName, app.pid);
                {
                    controllerManager.get().getService().appStart(app.info.packageName);
                }
            }else {
                controllerManager.get().getService().appProcessStop(app.info.packageName, app.processName, app.pid);
                {
                    controllerManager.get().getService().appStop(app.info.packageName);
                }
            }
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    private void onProcessDead(ProcessRecord record) {
        synchronized (mProcessNames) {
            mProcessNames.remove(record.processName, record.vuid);
        }
        notifyAppProcessStatus(record, 0, false);
        synchronized (mPidsSelfLocked) {
            mPidsSelfLocked.remove(record.pid);
        }
        processDead(record);
    }

    @Override
    public int getFreeStubCount() {
        return StubManifest.STUB_COUNT - mPidsSelfLocked.size();
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        if (permission == null) {
            return PackageManager.PERMISSION_DENIED;
        }
        if ("android.permission.INTERACT_ACROSS_USERS".equals(permission) || "android.permission.INTERACT_ACROSS_USERS_FULL".equals(permission)) {
            return PackageManager.PERMISSION_DENIED;
        }
        if (uid == 0) {
            return PackageManager.PERMISSION_GRANTED;
        }
        return VPackageManagerService.get().checkUidPermission(permission, uid);
    }

    @Override
    public ClientConfig initProcess(String packageName, String processName, int userId) {
        synchronized (this) {
            ProcessRecord r = startProcessIfNeedLocked(processName, userId, packageName, -1, VBinder.getCallingUid());
            if (r != null) {
                return r.getClientConfig();
            }
            return null;
        }
    }

    @Override
    public void appDoneExecuting(String packageName, int userId) {
        int pid = VBinder.getCallingPid();
        ProcessRecord r = findProcessLocked(pid);
        if (r != null) {
            r.pkgList.add(packageName);
        }
    }

    ProcessRecord startProcessIfNeedLocked(String processName, int userId, String packageName, int vpid, int callingUid) {
        runProcessGC();
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        ApplicationInfo info = VPackageManagerService.get().getApplicationInfo(packageName, 0, userId);
        if (ps == null || info == null) {
            return null;
        }
        if (!ps.isLaunched(userId)) {
            sendFirstLaunchBroadcast(ps, userId);
            ps.setLaunched(userId, true);
            VAppManagerService.get().savePersistenceData();
        }
        int vuid = VUserHandle.getUid(userId, ps.appId);
        boolean is64bit = ps.isRunOn64BitProcess();
        ProcessRecord app;
        if (vpid == -1) {
            synchronized (mPendingProcessNames) {
                app = mPendingProcessNames.get(processName, vuid);
            }
            if (app != null) {
                VLog.w(TAG, "wait process init : " + processName);
                app.initLock.block();
                return app;
            }
            synchronized (mProcessNames) {
                app = mProcessNames.get(processName, vuid);
            }
            if (app != null && app.client.asBinder().isBinderAlive()) {
                return app;
            }
            VLog.w(TAG, "start new process : " + processName);
            vpid = queryFreeStubProcess(is64bit);
        }
        if (vpid == -1) {
            VLog.e(TAG, "Unable to query free stub for : " + processName);
            return null;
        }
        app = new ProcessRecord(info, processName, vuid, vpid, callingUid, is64bit);
        synchronized (mPendingProcessNames) {
            mPendingProcessNames.put(processName, vuid, app);
            mPendingProcessLocked.add(app);
        }
        if (initProcess(app)) {
            return app;
        } else {
            return null;
        }
    }


    private void runProcessGC() {
        if (VActivityManagerService.get().getFreeStubCount() < 3) {
            // run GC
            killAllApps();
        }
    }

    private void sendFirstLaunchBroadcast(PackageSetting ps, int userId) {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_FIRST_LAUNCH, Uri.fromParts("package", ps.packageName, null));
        intent.setPackage(ps.packageName);
        intent.putExtra(Intent.EXTRA_UID, VUserHandle.getUid(ps.appId, userId));
        intent.putExtra("android.intent.extra.user_handle", userId);
        sendBroadcastAsUser(intent, new VUserHandle(userId));
    }


    @Override
    public int getUidByPid(int pid) {
        synchronized (mPidsSelfLocked) {
            ProcessRecord r = findProcessLocked(pid);
            if (r != null) {
                return r.vuid;
            }
        }
        if (pid == Process.myPid()) {
            return Process.SYSTEM_UID;
        }
        return -1;
    }

    private void startRequestPermissions(boolean is64bit, String[] permissions,
                                         final ConditionVariable permissionLock) {

        PermissionCompat.startRequestPermissions(VirtualCore.get().getContext(), is64bit, permissions, new PermissionCompat.CallBack() {
            @Override
            public boolean onResult(int requestCode, String[] permissions, int[] grantResults) {
                try {
                    mResult = PermissionCompat.isRequestGranted(grantResults);
                } finally {
                    permissionLock.open();
                }
                return mResult;
            }
        });
    }


    private boolean initProcess(ProcessRecord app) {
        requestPermissionIfNeed(app);
        Bundle extras = new Bundle();
        extras.putParcelable("_VA_|_client_config_", app.getClientConfig());
        Bundle res = ProviderCall.callSafely(app.getProviderAuthority(), "_VA_|_init_process_", null, extras);
        if (res == null) {
            return false;
        }
        app.pid = res.getInt("_VA_|_pid_");
        IBinder clientBinder = BundleCompat.getBinder(res, "_VA_|_client_");
        try {
            return attachClient(app, clientBinder);
        } finally {
            app.initLock.open();
            app.initLock = null;
            synchronized (mPendingProcessNames) {
                mPendingProcessNames.remove(app.processName, app.vuid);
                mPendingProcessLocked.remove(app);
            }
        }
    }

    private void requestPermissionIfNeed(ProcessRecord app) {
        if (PermissionCompat.isCheckPermissionRequired(app.info.targetSdkVersion)) {
            String[] permissions = VPackageManagerService.get().getDangrousPermissions(app.info.packageName);
            if (!PermissionCompat.checkPermissions(permissions, app.is64bit)) {
                final ConditionVariable permissionLock = new ConditionVariable();
                startRequestPermissions(app.is64bit, permissions, permissionLock);
                permissionLock.block();
            }
        }
    }

    public int queryFreeStubProcess(boolean is64bit) {
        synchronized (mPidsSelfLocked) {
            for (int vpid = 0; vpid < StubManifest.STUB_COUNT; vpid++) {
                int N = mPidsSelfLocked.size();
                boolean using = false;
                while (N-- > 0) {
                    ProcessRecord r = mPidsSelfLocked.valueAt(N);
                    if (mPendingProcessLocked.contains(r)
                            || (r.vpid == vpid && r.is64bit == is64bit)) {
                        using = true;
                        break;
                    }
                }
                if (using) {
                    continue;
                }
                return vpid;
            }
        }
        return -1;
    }

    @Override
    public boolean isAppProcess(String processName) {
        return parseVPid(processName) != -1;
    }

    @Override
    public boolean isAppPid(int pid) {
        synchronized (mPidsSelfLocked) {
            return findProcessLocked(pid) != null;
        }
    }

    @Override
    public String getAppProcessName(int pid) {
        synchronized (mPidsSelfLocked) {
            ProcessRecord r = mPidsSelfLocked.get(pid);
            if (r != null) {
                return r.processName;
            }
        }
        return null;
    }

    @Override
    public List<String> getProcessPkgList(int pid) {
        synchronized (mPidsSelfLocked) {
            ProcessRecord r = mPidsSelfLocked.get(pid);
            if (r != null) {
                return new ArrayList<>(r.pkgList);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void killAllApps() {
        synchronized (mPidsSelfLocked) {
            for (int i = 0; i < mPidsSelfLocked.size(); i++) {
                try {
                    ProcessRecord r = mPidsSelfLocked.valueAt(i);
                    ArrayList<ServiceRecord> tmprecord = new ArrayList<ServiceRecord>();
                    synchronized (mHistory) {
                        for (ServiceRecord sr : mHistory) {
                            if (sr.process == r) {
                                tmprecord.add(sr);
                            }
                        }
                    }
                    for (ServiceRecord tsr : tmprecord) {
                        Log.e("wxd", " killService " + tsr.serviceInfo.toString() + " in " + r.processName + ":" + r.pid);
                        stopServiceCommon(tsr, ComponentUtils.toComponentName(tsr.serviceInfo));
                    }
                    Log.e("wxd", " killAllApps " + r.processName + " pid : " + r.pid);
                    r.client.clearSettingProvider();
                    finishAllActivity(r);
                    r.kill();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void killAppByPkg(final String pkg, int userId) {
        synchronized (mPidsSelfLocked) {
            int N = mPidsSelfLocked.size();
            while (N-- > 0) {
                ProcessRecord r = mPidsSelfLocked.valueAt(N);
                if (r.userId == userId && r.info.packageName.equals(pkg)) {
                    for (String pkgName : r.pkgList){
                        Log.e("wxd", " killAppByPkg item " +pkgName);
                    }
                    Log.e("wxd", " killAppByPkg package" +pkg);
                    Log.e("wxd", " killAppByPkg pid" +r.pid);
                    {
                        try {
                            ArrayList<ServiceRecord> tmprecord = new ArrayList<ServiceRecord>();
                            synchronized (mHistory)
                            {
                                for (ServiceRecord sr : mHistory) {
                                    if (sr.process == r)
                                    {
                                        tmprecord.add(sr);
                                    }
                                }
                            }
                            for(ServiceRecord tsr : tmprecord)
                            {
                                Log.e("wxd", " killService " +  tsr.serviceInfo.toString() + " in " + r.processName + ":" + r.pid);
                                stopServiceCommon(tsr, ComponentUtils.toComponentName(tsr.serviceInfo));
                            }
                            Log.e("wxd", " killAppByPkg  " + r.pid);
                            r.client.clearSettingProvider();
                            finishAllActivity(r);
                            r.kill();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isAppRunning(String packageName, int userId, boolean foreground) {
        boolean running = false;
        synchronized (mPidsSelfLocked) {
            int N = mPidsSelfLocked.size();
            while (N-- > 0) {
                ProcessRecord r = mPidsSelfLocked.valueAt(N);
                if (r.userId != userId) {
                    continue;
                }
                if (!r.info.packageName.equals(packageName)) {
                    continue;
                }
                if (foreground) {
                    if (!r.info.processName.equals(packageName)) {
                        continue;
                    }
                }
                try {
                    running = r.client.isAppRunning();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }
            return running;
        }
    }

    public int getRunningAppMemorySize(String packageName, int userId) throws RemoteException {
        synchronized (mPidsSelfLocked) {
            int size = 0;
            int N = mPidsSelfLocked.size();
            while (N-- > 0) {
                ProcessRecord r = mPidsSelfLocked.valueAt(N);
                if (r.userId == userId && r.info.packageName.equals(packageName)) {
                    int[] pids = new int[] {r.pid};
                    Debug.MemoryInfo[] memoryInfo = am.getProcessMemoryInfo(pids);
                    size = size + memoryInfo[0].dalvikPrivateDirty;
                }
            }
            Log.i("wxd", " getRunningAppMemorySize : " + size);
            return size;
        }
    }

    public void closeAllLongSocket(String packageName, int userId) throws RemoteException {
        synchronized (mPidsSelfLocked) {
            int N = mPidsSelfLocked.size();
            while (N-- > 0) {
                ProcessRecord r = mPidsSelfLocked.valueAt(N);
                if (r.userId == userId && r.info.packageName.equals(packageName)) {
                    r.client.closeAllLongSocket();
                }
            }
        }
    }

    @Override
    public void killApplicationProcess(final String processName, int uid) {
        synchronized (mProcessNames) {
            ProcessRecord r = mProcessNames.get(processName, uid);
            if (r != null) {
                if (r.is64bit) {
                    V64BitHelper.forceStop64(r.pid);
                } else {
                    r.kill();
                }
            }
        }
    }

    @Override
    public void dump() {

    }

    @Override
    public String getInitialPackage(int pid) {
        synchronized (mPidsSelfLocked) {
            ProcessRecord r = mPidsSelfLocked.get(pid);
            if (r != null) {
                return r.info.packageName;
            }
            return null;
        }
    }


    /**
     * Should guard by {@link VActivityManagerService#mPidsSelfLocked}
     *
     * @param pid pid
     */
    public ProcessRecord findProcessLocked(int pid) {
        return mPidsSelfLocked.get(pid);
    }

    /**
     * Should guard by {@link VActivityManagerService#mProcessNames}
     *
     * @param uid vuid
     */
    public ProcessRecord findProcessLocked(String processName, int uid) {
        return mProcessNames.get(processName, uid);
    }

    public int stopUser(int userHandle, IStopUserCallback.Stub stub) {
        synchronized (mPidsSelfLocked) {
            int N = mPidsSelfLocked.size();
            while (N-- > 0) {
                ProcessRecord r = mPidsSelfLocked.valueAt(N);
                if (r.userId == userHandle) {
                    r.kill();
                }
            }
        }
        try {
            stub.userStopped(userHandle);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void sendOrderedBroadcastAsUser(Intent intent, VUserHandle user, String receiverPermission,
                                           BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
                                           String initialData, Bundle initialExtras) {
        Context context = VirtualCore.get().getContext();
        if (user != null) {
            intent.putExtra("_VA_|_user_id_", user.getIdentifier());
        }
        context.sendOrderedBroadcast(intent, null/* permission */, resultReceiver, scheduler, initialCode, initialData,
                initialExtras);
    }

    public void sendBroadcastAsUser(Intent intent, VUserHandle user) {
        SpecialComponentList.protectIntent(intent);
        Context context = VirtualCore.get().getContext();
        if (user != null) {
            intent.putExtra("_VA_|_user_id_", user.getIdentifier());
        }
        context.sendBroadcast(intent);
    }

    public boolean bindServiceAsUser(Intent service, ServiceConnection connection, int flags, VUserHandle user) {
        service = new Intent(service);
        if (user != null) {
            service.putExtra("_VA_|_user_id_", user.getIdentifier());
        }
        return VirtualCore.get().getContext().bindService(service, connection, flags);
    }

    public void sendBroadcastAsUser(Intent intent, VUserHandle user, String permission) {
        SpecialComponentList.protectIntent(intent);
        Context context = VirtualCore.get().getContext();
        if (user != null) {
            intent.putExtra("_VA_|_user_id_", user.getIdentifier());
        }
        context.sendBroadcast(intent);
    }

    boolean handleStaticBroadcast(BroadcastIntentData data, ActivityInfo info, int appId, PendingResultData result) {
        int vuid = VUserHandle.getUid(data.userId, appId);
        if (data.targetPackage != null && !data.targetPackage.equals(info.packageName)) {
            return false;
        }
        return handleUserBroadcast(vuid, info, data.intent, result);
    }

    private boolean handleUserBroadcast(int vuid, ActivityInfo info, Intent targetIntent, PendingResultData result) {
        handleStaticBroadcastAsUser(vuid, info, targetIntent, result);
        return true;
    }

    private void handleStaticBroadcastAsUser(int vuid, ActivityInfo info, Intent intent,
                                             PendingResultData result) {
        synchronized (this) {
            ProcessRecord r = findProcessLocked(info.processName, vuid);
            if (r == null) {
                int userId = getUserId(vuid);
                if (SpecialComponentList.allowedStartFromBroadcast(info.packageName)) {
                    r = startProcessIfNeedLocked(info.processName, userId, info.packageName, -1, -1);
                }
            }
            if (r != null && r.client != null) {
                performScheduleReceiver(r.client, info, intent,
                        result);
            }
        }
    }

    private void performScheduleReceiver(IVClient client, ActivityInfo info, Intent intent,
                                         PendingResultData result) {

        ComponentName componentName = ComponentUtils.toComponentName(info);
        BroadcastSystem.get().broadcastSent(info, result);
        try {
            client.scheduleReceiver(info.processName, componentName, intent, result);
        } catch (Throwable e) {
            if (result != null) {
                result.finish();
            }
        }
    }

    @Override
    public boolean broadcastFinish(IBinder token) {
        return BroadcastSystem.get().broadcastFinish(token);
    }

    @Override
    public void notifyBadgerChange(BadgerInfo info) {
        Intent intent = new Intent(Constants.ACTION_BADGER_CHANGE);
        intent.putExtra("userId", info.userId);
        intent.putExtra("packageName", info.packageName);
        intent.putExtra("badgerCount", info.badgerCount);
        VirtualCore.get().getContext().sendBroadcast(intent);
    }

    @Override
    public int getCallingUidByPid(int pid) {
        synchronized (mPidsSelfLocked) {
            ProcessRecord r = findProcessLocked(pid);
            if (r != null) {
                return r.getCallingVUid();
            }
        }
        return -1;
    }

    @Override
    public void setAppInactive(String packageName, boolean idle, int userId) {
        synchronized (sIdeMap) {
            sIdeMap.put(packageName + "@" + userId, idle);
        }
    }

    @Override
    public boolean isAppInactive(String packageName, int userId) {
        synchronized (sIdeMap) {
            Boolean idle = sIdeMap.get(packageName + "@" + userId);
            return idle != null && !idle;
        }
    }

}