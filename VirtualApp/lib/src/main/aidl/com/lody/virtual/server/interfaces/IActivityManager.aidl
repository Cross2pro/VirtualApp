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
import com.lody.virtual.remote.ClientConfig;

import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.BadgerInfo;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.remote.VParceledListSlice;

/**
 * @author Lody
 */
interface IActivityManager{

    ClientConfig initProcess(String packageName, String processName, int userId);

    void appDoneExecuting(in String packageName, int userId);

    int getFreeStubCount();

    int checkPermission(boolean is64bit, String permission, int pid, int uid);

    int getSystemPid();

    int getSystemUid();

    int getUidByPid(int pid);

    boolean isAppProcess(String processName);

    boolean isAppRunning(String packageName, int userId, boolean foreground);

    boolean isAppPid(int pid);

    String getAppProcessName(int pid);

    java.util.List<String> getProcessPkgList(int pid);

    void killAllApps();

    void killAppByPkg(String pkg, int userId);

    void killApplicationProcess(String processName, int vuid);

    void dump();

    String getInitialPackage(int pid);

    int startActivities(in Intent[] intents,in  String[] resolvedTypes,in  IBinder token,in  Bundle options, int userId);

    int startActivity(in Intent intent,in  ActivityInfo info,in  IBinder resultTo,in  Bundle options, String resultWho, int requestCode, int userId);

    boolean finishActivityAffinity(int userId, in IBinder token);

    void onActivityCreated(in IBinder record, in IBinder token, int taskId);

    void onActivityResumed(int userId,in  IBinder token);

    boolean onActivityDestroyed(int userId,in  IBinder token);

    void onActivityFinish(int userId, in IBinder token);

    ComponentName getActivityClassForToken(int userId,in  IBinder token);

    String getCallingPackage(int userId,in  IBinder token);

    int getCallingUidByPid(int pid);

    ComponentName getCallingActivity(int userId,in  IBinder token);

    AppTaskInfo getTaskInfo(int taskId);

    String getPackageForToken(int userId,in  IBinder token);

    IBinder acquireProviderClient(int userId,in  ProviderInfo info);

    void addOrUpdateIntentSender(in IntentSenderData sender, int userId);

    void removeIntentSender(in IBinder token);

    IntentSenderData getIntentSender(in IBinder token);

    void processRestarted(String packageName, String processName, int userId);

    boolean broadcastFinish(in IBinder token);

    void notifyBadgerChange(in BadgerInfo info);

    void setAppInactive(String packageName, boolean idle, int userId);

    boolean isAppInactive(String packageName, int userId);

    boolean isVAServiceToken(in IBinder token);

    ComponentName startService(in Intent service, String resolvedType, int userId);

    int stopService(in IBinder caller,in  Intent service, String resolvedType, int userId);

    boolean stopServiceToken(in ComponentName className,in  IBinder token, int startId, int userId);

    void setServiceForeground(in ComponentName className,in  IBinder token, int id,in  Notification notification, boolean removeNotification, int userId);

    int bindService(in IBinder caller,in  IBinder token,in  Intent service, String resolvedType, IServiceConnection connection, int flags, int userId);

    boolean unbindService(in IServiceConnection connection, int userId);

    void unbindFinished(in IBinder token,in  Intent service, boolean doRebind, int userId);

    void serviceDoneExecuting(in IBinder token, int type, int startId, int res, int userId);

    IBinder peekService(in Intent service, String resolvedType, int userId);

    void publishService(in IBinder token,in  Intent intent,in  IBinder service, int userId);

    VParceledListSlice getServices(int maxNum, int flags, int userId);

    void handleDownloadCompleteIntent(in Intent intent);

    int getRunningAppMemorySize(String packageName, int userId);

    void closeAllLongSocket(String packageName, int userId);
}
