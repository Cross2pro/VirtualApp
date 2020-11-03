package io.virtualapp.delegate;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.AppCallback;
import com.lody.virtual.helper.compat.BuildCompat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class MyComponentDelegate implements AppCallback {

    @Override
    public void beforeStartApplication(String packageName, String processName, Context context) {
    }

    @Override
    public void beforeApplicationCreate(String packageName, String processName, Application application) {

    }

    @Override
    public void afterApplicationCreate(String packageName, String processName, Application application) {
//        Log.e("kk-test", "path=" + NativeEngine.getRedirectedPath("/data/data/" + packageName + "/"));
//        Log.e("kk-test", "path=" + NativeEngine.getRedirectedPath("/data/data/" + packageName));
//        try {
//            if(BuildCompat.isQ()){
//                XposedHelpers.findAndHookMethod("android.os.FileObserver$ObserverThread", application.getClassLoader(),
//                        "startWatching", int.class, String[].class, int.class, int[].class,new XC_MethodHook() {
//                            @Override
//                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                                String[] paths = (String[])param.args[1];
//                                for(int i = 0;i<paths.length;i++) {
//                                    paths[i] = NativeEngine.getRedirectedPath(paths[i]);
//                                }
//                                Log.w("MethodInvocationStub", "startWatching:"+Arrays.toString((String[])param.args[1]));
//                                super.beforeHookedMethod(param);
//                            }
//                        });
//            }else {
//                XposedHelpers.findAndHookMethod("android.os.FileObserver$ObserverThread", application.getClassLoader(),
//                        "startWatching", int.class, String.class, int.class, new XC_MethodHook() {
//                            @Override
//                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                                param.args[1] = NativeEngine.getRedirectedPath((String) param.args[1]);
//                                Log.w("MethodInvocationStub", "startWatching:"+param.args[1]);
//                                super.beforeHookedMethod(param);
//                            }
//                        });
//            }
//        }catch (Throwable e){
//            try {
//                Class<?> clazz = Class.forName("android.os.FileObserver$ObserverThread", true, application.getClassLoader());
//                Method[] methods = clazz.getDeclaredMethods();
//                for(Method method:methods){
//                    if(Modifier.isNative(method.getModifiers())){
//                        Log.e("MethodInvocationStub", method.getName()+":"+ Arrays.toString(method.getParameterTypes()));
//                    }
//                }
//            }catch (Throwable e2){
//                e2.printStackTrace();
//            }
//        }
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
