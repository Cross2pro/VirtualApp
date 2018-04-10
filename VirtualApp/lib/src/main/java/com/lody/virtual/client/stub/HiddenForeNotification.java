package com.lody.virtual.client.stub;

import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.IBinder;

public class HiddenForeNotification extends Service {
    private static final int ID = 2781;

    public static void bindForeground(Service service) {
        Builder builder = new Builder(service.getApplicationContext());
        builder.setSmallIcon(android.R.drawable.ic_dialog_dialer);
        if (VERSION.SDK_INT > 24) {
            builder.setContentTitle("Running...");
            builder.setContentText("Keep app alive to receive new messages on time");
        } else {
            builder.setContentTitle("Tip: Message Should Be Hidden");
            builder.setContentText("Get Removed");
            builder.setContentIntent(PendingIntent.getService(service, 0, new Intent(service, HiddenForeNotification.class), 0));
        }
        service.startForeground(ID, builder.getNotification());
        if (VERSION.SDK_INT <= 24) {
            service.startService(new Intent(service, HiddenForeNotification.class));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Builder builder = new Builder(getBaseContext());
            builder.setSmallIcon(android.R.drawable.ic_dialog_dialer);
            builder.setContentTitle("Remove Service Notification");
            builder.setContentText("Remove Service Notification");
            startForeground(ID, builder.getNotification());
            stopForeground(true);
            stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}