package com.lody.virtual.client.natives;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

    public static Method gAudioRecordNativeSetup;

    public static Method gMediaRecorderNativeSetup;

    public static Method gCameraStartPreview;
    public static Method gCameraNativeTakePicture;
    public static Method gAudioRecordStart;
    public static Method gMediaRecordPrepare;

    @SuppressLint("PrivateApi")
    public static void init() {
        try {
            gMediaRecorderNativeSetup = MediaRecorder.class.getDeclaredMethod("native_setup",
                    Object.class, String.class, String.class);
        } catch (NoSuchMethodException e) {
            try {
                gMediaRecorderNativeSetup = MediaRecorder.class.getDeclaredMethod("native_setup",
                        Object.class, String.class);
            } catch (NoSuchMethodException e1) {
                //ignore
            }
        }
        try {
            gAudioRecordNativeSetup = AudioRecord.class.getDeclaredMethod("native_setup",
                    Object.class, Object.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, int[].class, String.class);
        } catch (NoSuchMethodException e) {
            try {
                gAudioRecordNativeSetup = AudioRecord.class.getDeclaredMethod("native_setup",
                        Object.class, Object.class, int[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, int[].class, String.class, Long.TYPE);
            } catch (NoSuchMethodException e1) {
                //ignore
            }
        }

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

        try {
            gCameraStartPreview = Camera.class.getDeclaredMethod("startPreview");
            gCameraStartPreview.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        try {
            gCameraNativeTakePicture = Camera.class.getDeclaredMethod("native_takePicture", int.class);
            gCameraNativeTakePicture.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            gAudioRecordStart = AudioRecord.class.getDeclaredMethod("native_start", int.class,int.class);
            gAudioRecordStart.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            gMediaRecordPrepare = MediaRecorder.class.getDeclaredMethod("_prepare");
            gMediaRecordPrepare.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
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
