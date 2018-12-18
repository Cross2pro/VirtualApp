package io.virtualapp.delegate;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.lody.virtual.client.core.AppCallback;


public class MyComponentDelegate implements AppCallback {

    @Override
    public void beforeStartApplication(String packageName, String processName, Context context) {
    }

    @Override
    public void beforeApplicationCreate(Application application) {
    }

    @Override
    public void afterApplicationCreate(Application application) {

    }

    @Override
    public void onSendBroadcast(Intent intent) {

    }
}
