package com.lody.virtual.server.interfaces;

// Declare any non-default types here with import statements

interface INotificationCallback {
    void addNotification(String packageName, int notificationCount);
    void cancelAllNotification(String packageName);
    void cancelNotification(String packageName, int notificationCount);
}
