package com.lody.virtual.server.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.helper.utils.Singleton;
import com.xdja.zs.INotificationCallback;
import com.lody.virtual.server.interfaces.INotificationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VNotificationManagerService extends INotificationManager.Stub {
    private static final String TAG = VNotificationManagerService.class.getSimpleName();
    private static final Singleton<VNotificationManagerService> gService = new Singleton<VNotificationManagerService>() {
        @Override
        protected VNotificationManagerService create() {
            return new VNotificationManagerService();
        }
    };
    private NotificationManager mNotificationManager;
    //    static final String TAG = NotificationCompat.class.getSimpleName();
    private final List<String> mDisables = new ArrayList<>();
    //VApp's Notifications
    private final HashMap<String, List<NotificationInfo>> mNotifications = new HashMap<>();
    private Context mContext;
    private INotificationCallback iNotificationCallback;

    private void init(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void systemReady(Context context) {
        get().init(context);
    }

    public static VNotificationManagerService get() {
        return gService.get();
    }

    /***
     * fake notification's id
     *
     * @param id          notification's id
     * @param packageName notification's package
     * @param userId      user
     * @return
     */
    @Override
    public int dealNotificationId(int id, String packageName, String tag, int userId) {
        Log.e(TAG, "dealNotificationId");
        return id;
    }

    /***
     * fake notification's tag
     *
     * @param id          notification's id
     * @param packageName notification's package
     * @param tag         notification's tag
     * @param userId      user
     * @return
     */
    @Override
    public String dealNotificationTag(int id, String packageName, String tag, int userId) {
        Log.e(TAG, "dealNotificationTag");
        if (TextUtils.equals(mContext.getPackageName(), packageName)) {
            return tag;
        }
        if (tag == null) {
            return packageName + "@" + userId;
        }
        return packageName + ":" + tag + "@" + userId;
    }

    @Override
    public boolean areNotificationsEnabledForPackage(String packageName, int userId) {
        Log.e(TAG, "areNotificationsEnabledForPackage");
        return !mDisables.contains(packageName + ":" + userId);
    }

    @Override
    public void setNotificationsEnabledForPackage(String packageName, boolean enable, int userId) {
        Log.e(TAG, "setNotificationsEnabledForPackage");
        String key = packageName + ":" + userId;
        if (enable) {
            if (mDisables.contains(key)) {
                mDisables.remove(key);
            }
        } else {
            if (!mDisables.contains(key)) {
                mDisables.add(key);
            }
        }
        //TODO: save mDisables ?
    }

    @Override
    public void addNotification(int id, String tag, String packageName, int userId) throws RemoteException {
        Log.e(TAG, "addNotification id: " + id);
        Log.e(TAG, "addNotification tag: " + tag);
        Log.e(TAG, "addNotification packageName: " + packageName);
        Log.e(TAG, "addNotification userId: " + userId);
        NotificationInfo notificationInfo = new NotificationInfo(id, tag, packageName, userId);
        synchronized (mNotifications) {
            List<NotificationInfo> list = mNotifications.get(packageName);
            if (list == null) {
                list = new ArrayList<>();
                mNotifications.put(packageName, list);
            }
            if (!list.contains(notificationInfo)) {
                list.add(notificationInfo);
            }
        }
        if (iNotificationCallback == null) {
            return;
        }
        List<NotificationInfo> notificationInfos = mNotifications.get(packageName);
        int size = notificationInfos == null ? 0 : notificationInfos.size();
        Log.e(TAG, "addNotification size: " + size);
        iNotificationCallback.addNotification(packageName, size);
    }

    @Override
    public void cancelAllNotification(String packageName, int userId) throws RemoteException {
        Log.e(TAG, "cancelAllNotification packageName: " + packageName);
        Log.e(TAG, "cancelAllNotification userId: " + userId);
        List<NotificationInfo> infos = new ArrayList<>();
        synchronized (mNotifications) {
            List<NotificationInfo> list = mNotifications.get(packageName);
            if (list != null) {
                int count = list.size();
                for (int i = count - 1; i >= 0; i--) {
                    NotificationInfo info = list.get(i);
                    if (info.userId == userId) {
                        infos.add(info);
                        list.remove(i);
                    }
                }
            }
        }
        for (NotificationInfo info : infos) {
            Log.e(TAG, "cancelAllNotification tag: " + info.tag + " id: " + info.id + " userId:" + info.userId);
            mNotificationManager.cancel(info.tag, info.id);
        }
        if (iNotificationCallback == null) {
            return;
        }
        iNotificationCallback.cancelAllNotification(packageName);
    }

    public void cancelNotification(String pkg, String tag, int id, int userId) throws RemoteException {
        Log.e(TAG, "cancelNotification pkg: " + pkg);
        Log.e(TAG, "cancelNotification tag: " + tag);
        Log.e(TAG, "cancelNotification id: " + id);
        Log.e(TAG, "cancelNotification userId: " + userId);
        List<NotificationInfo> infos = new ArrayList<>();
        synchronized (mNotifications) {
            List<NotificationInfo> list = mNotifications.get(pkg);
            if (list != null) {
                int count = list.size();
                for (int i = count - 1; i >= 0; i--) {
                    NotificationInfo info = list.get(i);
                    if (info.userId == userId && info.id == id) {
                        infos.add(info);
                        list.remove(i);
                    }
                }
            }
        }
        for (NotificationInfo info : infos) {
            Log.e(TAG, "cancelNotification tag: " + info.tag + " id: " + info.id + " userId:" + info.userId);
            mNotificationManager.cancel(info.tag, info.id);
        }
        if (iNotificationCallback == null) {
            return;
        }
        List<NotificationInfo> notificationInfos = mNotifications.get(pkg);
        int size = notificationInfos == null ? 0 : notificationInfos.size();
        Log.e(TAG, "cancelNotification size: " + size);
        iNotificationCallback.cancelNotification(pkg, size);
    }

    public void registerCallback(INotificationCallback iNotificationCallback) {
        this.iNotificationCallback = iNotificationCallback;
    }

    private static class NotificationInfo {
        int id;
        String tag;
        String packageName;
        int userId;

        NotificationInfo(int id, String tag, String packageName, int userId) {
            this.id = id;
            this.tag = tag;
            this.packageName = packageName;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NotificationInfo) {
                NotificationInfo that = (NotificationInfo) obj;
                return that.id == id && TextUtils.equals(that.tag, tag)
                        && TextUtils.equals(packageName, that.packageName)
                        && that.userId == userId;
            }
            return super.equals(obj);
        }
    }

}
