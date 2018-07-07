package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

/**
 * @author Lody
 */
public interface INotificationManager extends IPCInterface {

    int dealNotificationId(int id, String packageName, String tag, int userId) throws RemoteException;

    String dealNotificationTag(int id, String packageName, String tag, int userId) throws RemoteException;

    boolean areNotificationsEnabledForPackage(String packageName, int userId) throws RemoteException;

    void setNotificationsEnabledForPackage(String packageName, boolean enable, int userId) throws RemoteException;

    void addNotification(int id, String tag, String packageName, int userId) throws RemoteException;

    void cancelAllNotification(String packageName, int userId) throws RemoteException;

    void cancelNotification(String pkg, String tag, int id, int userId) throws RemoteException;

    void registerCallback(INotificationCallback iNotificationCallback) throws RemoteException;

    abstract class Stub implements INotificationManager {
        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }
    }
}