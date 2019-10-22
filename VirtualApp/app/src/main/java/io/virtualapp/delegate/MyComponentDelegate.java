package io.virtualapp.delegate;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;

import com.lody.virtual.client.core.AppCallback;

import andhook.lib.XC_MethodHook;
import andhook.lib.XposedHelpers;


public class MyComponentDelegate implements AppCallback {

    @Override
    public void beforeStartApplication(String packageName, String processName, Context context) {

    }

    @Override
    public void beforeApplicationCreate(String packageName, String processName, Application application) {

    }

    @Override
    public void afterApplicationCreate(String packageName, String processName, Application application) {
        XposedHelpers.findAndHookMethod(WebSettings.class, "getDefaultUserAgent", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                param.setResult("Mozilla/5.0 (Linux; Android 9; STF-AL00 Build/KKKKKKKKK; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.136 Mobile Safari/537.36");
                Log.e("kk", "getDefaultUserAgent:" + param.getResult());
            }
        });
    }

}
