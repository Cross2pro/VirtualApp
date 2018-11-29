package com.lody.virtual.client.stub;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.lody.virtual.client.core.VirtualCore;


/**
 * @author Lody
 */
public class KeepAliveService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(!VirtualCore.getConfig().isHideForegroundNotification()) {
            HiddenForeNotification.bindForeground(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
