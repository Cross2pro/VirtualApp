package com.xdja.zs;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.xdja.activitycounter.ActivityCounterManager;

@SuppressLint("OverrideAbstract")
public class NotificationListener extends NotificationListenerService {
    String Tag = "zs_NotificationListener";

    public NotificationListener() {
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        Log.e(Tag, "onListenerConnected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Notification notification = sbn.getNotification();
        String packageName = sbn.getPackageName();
        Bundle extras = notification.extras;
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
        int notificationIcon = extras.getInt(Notification.EXTRA_SMALL_ICON);
        Bitmap notificationLargeIcon = ((Bitmap)extras.getParcelable(Notification.EXTRA_LARGE_ICON));
        CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        Log.e(Tag, String.format("onNotificationPosted [Title: %s] [Text: %s] [SubText: %s] [pkgName %s]",
                notificationTitle, notificationText, notificationSubText, packageName));

        if(packageName.equals("com.android.server.telecom") && ActivityCounterManager.get().isForeGround())
        {
            Log.e(Tag, "delete this notify " + sbn.getKey());
            cancelNotification(sbn.getKey());
            //snoozeNotification(sbn.getKey(), 10 * 1000);
        }
    }
}