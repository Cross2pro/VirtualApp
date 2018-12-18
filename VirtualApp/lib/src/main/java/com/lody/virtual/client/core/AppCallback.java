package com.lody.virtual.client.core;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

public interface AppCallback {

    AppCallback EMPTY = new AppCallback() {

        @Override
        public void onSendBroadcast(Intent intent) {
            // Empty
        }

        @Override
        public void beforeStartApplication(String packageName, String processName, Context context) {

        }

        @Override
        public void beforeApplicationCreate(Application application) {
            // Empty
        }

        @Override
        public void afterApplicationCreate(Application application) {
            // Empty
        }
    };

    void beforeStartApplication(String packageName, String processName, Context context);

    void beforeApplicationCreate(Application application);

    void afterApplicationCreate(Application application);

    void onSendBroadcast(Intent intent);
}
