package com.lody.virtual.client.ipc;

import android.app.Notification;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.server.interfaces.INotificationManager;
import com.lody.virtual.server.notification.NotificationCompat;

import com.xdja.zs.INotificationCallback;

/**
 * Fake notification manager
 */
public class VNotificationManager {
    private static final VNotificationManager sInstance = new VNotificationManager();
    private final NotificationCompat mNotificationCompat;
    private INotificationManager mService;

    public INotificationManager getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (VNotificationManager.class) {
                final Object pmBinder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(INotificationManager.class, pmBinder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return INotificationManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.NOTIFICATION));
    }

    private VNotificationManager() {
        mNotificationCompat = NotificationCompat.create();
    }

    public static VNotificationManager get() {
        return sInstance;
    }

    public boolean dealNotification(int id, Notification notification, String packageName) {
        if (notification == null) return false;
        return VirtualCore.get().getHostPkg().equals(packageName)
                || mNotificationCompat.dealNotification(id, notification, packageName);
    }

    public int dealNotificationId(int id, String packageName, String tag, int userId) {
        try {
            return getService().dealNotificationId(id, packageName, tag, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return id;
    }

    public String dealNotificationTag(int id, String packageName, String tag, int userId) {
        try {
            return getService().dealNotificationTag(id, packageName, tag, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return tag;
    }

    public boolean areNotificationsEnabledForPackage(String packageName, int userId) {
        try {
            return getService().areNotificationsEnabledForPackage(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void setNotificationsEnabledForPackage(String packageName, boolean enable, int userId) {
        try {
            getService().setNotificationsEnabledForPackage(packageName, enable, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void addNotification(int id, String tag, String packageName, int userId) {
        try {
            getService().addNotification(id, tag, packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelAllNotification(String packageName, int userId) {
        try {
            getService().cancelAllNotification(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelNotification(String packageName, String tag, int id, int userId) {
        try {
            getService().cancelNotification(packageName, tag, id, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void registerCallback(INotificationCallback iNotificationCallback) {
        try {
            getService().registerCallback(iNotificationCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
