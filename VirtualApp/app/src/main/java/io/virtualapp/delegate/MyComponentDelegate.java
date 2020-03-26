package io.virtualapp.delegate;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

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
        if(!"com.xdja.swbg".equals(packageName)){
            return;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity.getClass().getName().endsWith("InCallActivity")) {
                    //方案1
//                    Intent service = new Intent()
//                            .setClassName("com.xdja.swbg", "com.csipsimple.service.SipService");
//                    activity.stopService(service);
//                    activity.startService(service);
                }
            }
        });
    }

}
