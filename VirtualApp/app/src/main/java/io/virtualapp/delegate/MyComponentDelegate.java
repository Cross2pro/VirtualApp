package io.virtualapp.delegate;

import android.app.Application;
import android.content.Intent;

import com.lody.virtual.client.core.AppCallback;
import com.lody.virtual.helper.utils.VLog;

import andhook.lib.xposed.XC_MethodHook;
import andhook.lib.xposed.XposedBridge;
import andhook.lib.xposed.XposedHelpers;


public class MyComponentDelegate implements AppCallback {

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
