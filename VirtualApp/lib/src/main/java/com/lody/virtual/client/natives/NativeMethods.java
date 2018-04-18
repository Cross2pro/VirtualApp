package com.lody.virtual.client.natives;

import android.hardware.Camera;
import android.media.AudioRecord;
import android.os.Build;

import com.lody.virtual.client.hook.utils.MethodParameterUtils;

import java.lang.reflect.Method;

import dalvik.system.DexFile;

/**
 * @author Lody
 */
public class NativeMethods {

    public static int gCameraMethodType;

    public static Method gCameraNativeSetup;

    public static Method gOpenDexFileNative;

    public static Method gAudioRecordNativeCheckPermission;

    public static void init() {
        String methodName =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? "openDexFileNative" : "openDexFile";
        for (Method method : DexFile.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                gOpenDexFileNative = method;
                break;
            }
        }
        if (gOpenDexFileNative == null) {
            throw new RuntimeException("Unable to find method : " + methodName);
        }
        gOpenDexFileNative.setAccessible(true);

        gCameraMethodType = -1;
        Method method = getCameraNativeSetup();
        if (method != null) {
            int index = MethodParameterUtils.getParamsIndex(
                    method.getParameterTypes(), String.class);
            gCameraNativeSetup = method;
            gCameraMethodType = 0x10 + index;
        }

        for (Method mth : AudioRecord.class.getDeclaredMethods()) {
            if (mth.getName().equals("native_check_permission")
                    && mth.getParameterTypes().length == 1
                    && mth.getParameterTypes()[0] == String.class) {
                gAudioRecordNativeCheckPermission = mth;
                mth.setAccessible(true);
                break;
            }
        }
    }

    private static Method getCameraNativeSetup() {
        Method[] methods = Camera.class.getDeclaredMethods();
        if (methods != null) {
            for (Method method : methods) {
                if ("native_setup".equals(method.getName())) {
                    return method;
                }
            }
        }
        return null;
    }

}
