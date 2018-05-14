package com.example.hooktest;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.lody.hooklib.art.vposed.VposedBridge;
import com.lody.hooklib.art.vposed.XC_MethodHook;
import com.lody.hooklib.art.vposed.XC_MethodReplacement;

/**
 * Created by weishu on 17/10/31.
 */
public class MainApplication extends Application {

    private static Context sContext;

    public static Context getAppContext() {
        return sContext;
    }

    public static class Name {
        private String text;

        public Name(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
        VposedBridge.hookAllConstructors(Name.class,
                new XC_MethodReplacement() {

                    @Override
                    protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param)
                            throws Throwable {
                        return VposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }
                });
        Name name = new Name("Hello world");
        Log.e("Name", "name = " + name);
    }

}
