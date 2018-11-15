package com.lody.virtual.server.interfaces;

import android.app.IServiceConnection;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.BadgerInfo;
import com.lody.virtual.remote.PendingIntentData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.VParceledListSlice;

/**
 * @author Lody
 */
interface IActivityManager{

    int initProcess(String packageName, String processName, int userId, int callingUid);

    int getFreeStubCount();

    int getSystemPid();

    int getUidByPid(int pid);

    boolean isAppProcess(String processName);

    boolean isAppRunning(String packageName, int userId);

    boolean isAppPid(int pid);

    String getAppProcessName(int pid);

    java.util.List<String> getProcessPkgList(int pid);

    void killAllApps();

    void killAppByPkg(String pkg, int userId);

    void killApplicationProcess(String processName, int vuid);

    void dump();

    String getInitialPackage(int pid);

    void handleApplicationCrash();

    void appDoneExecuting();

    int startActivities(in Intent[] intents,in  String[] resolvedTypes,in  IBinder token,in  Bundle options, int userId, int callingUid);

    int startActivity(in Intent intent,in  ActivityInfo info,in  IBinder resultTo,in  Bundle options, String resultWho, int requestCode, int userId, int callingUid);

    void onActivityCreated(in ComponentName component,in  ComponentName caller,in  IBinder token,in  Intent intent, String affinity, int taskId, int launchMode, int flags);

    void onActivityResumed(int userId,in  IBinder token);

    boolean onActivityDestroyed(int userId,in  IBinder token);

    ComponentName getActivityClassForToken(int userId,in  IBinder token);

    String getCallingPackage(int userId,in  IBinder token);

    int getCallingUidByPid(int pid);

    ComponentName getCallingActivity(int userId,in  IBinder token);

    AppTaskInfo getTaskInfo(int taskId);

    String getPackageForToken(int userId,in  IBinder token);

    boolean isVAServiceToken(in IBinder token);

    ComponentName startService(in IBinder caller,in  Intent service, String resolvedType, int userId, int callingUid);

    int stopService(in IBinder caller,in  Intent service, String resolvedType, int userId);

    boolean stopServiceToken(in ComponentName className,in  IBinder token, int startId, int userId);

    void setServiceForeground(in ComponentName className,in  IBinder token, int id,in  Notification notification, boolean removeNotification, int userId);

    int bindService(in IBinder caller,in  IBinder token,in  Intent service, String resolvedType, IServiceConnection connection, int flags, int userId, int callingUid);

    boolean unbindService(in IServiceConnection connection, int userId);

    void unbindFinished(in IBinder token,in  Intent service, boolean doRebind, int userId);

    void serviceDoneExecuting(in IBinder token, int type, int startId, int res, int userId);

    IBinder peekService(in Intent service, String resolvedType, int userId);

    void publishService(in IBinder token,in  Intent intent,in  IBinder service, int userId);

    VParceledListSlice getServices(int maxNum, int flags, int userId);

    IBinder acquireProviderClient(int userId,in  ProviderInfo info, int callingUid);

    PendingIntentData getPendingIntent(in IBinder binder);

    void addPendingIntent(in IBinder binder, String packageName);

    void removePendingIntent(in IBinder binder);

    String getPackageForIntentSender(in IBinder binder);

    void processRestarted(String packageName, String processName, int userId, int callingUid);

    void broadcastFinish(in PendingResultData res);

    void notifyBadgerChange(in BadgerInfo info);

    void setAppInactive(String packageName, boolean idle, int userId);

    boolean isAppInactive(String packageName, int userId);

    int getRunningAppMemorySize(String packageName, int userId);

    void closeAllLongSocket(String packageName, int userId);
}
