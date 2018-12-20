package io.virtualapp.delegate;

import android.app.Application;
import android.content.Context;

import com.lody.virtual.client.core.AppCallback;


public class MyComponentDelegate implements AppCallback {

    @Override
    public void beforeStartApplication(String packageName, String processName, Context context) {

    }

    @Override
    public void beforeApplicationCreate(String packageName, String processName, Application application) {

    }

    @Override
    public void afterApplicationCreate(String packageName, String processName, Application application) {

    }

}
