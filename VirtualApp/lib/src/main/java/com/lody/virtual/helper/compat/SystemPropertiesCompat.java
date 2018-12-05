package com.lody.virtual.helper.compat;

import com.lody.virtual.helper.utils.Reflect;

public class SystemPropertiesCompat {

    public static String get(String key, String defaultValue) {
        try {
            return (String) Reflect.on("android.os.SystemProperties").call("get", key, defaultValue).get();
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

}
