package com.lody.virtual.client;

import android.os.Binder;
import android.os.Build;
import android.os.Process;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.natives.NativeMethods;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VirtualApp Native Project
 */
public class NativeEngine {

    private static final String TAG = NativeEngine.class.getSimpleName();

    private static Map<String, InstalledAppInfo> sDexOverrideMap;

    private static boolean sFlag = false;

    private static final String LIB_NAME = "va++";

    static {
        try {
            System.loadLibrary(LIB_NAME);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    static {
        NativeMethods.init();
    }


    public static void startDexOverride() {
        List<InstalledAppInfo> installedAppInfos = VirtualCore.get().getInstalledApps(0);
        sDexOverrideMap = new HashMap<>(installedAppInfos.size());
        for (InstalledAppInfo info : installedAppInfos) {
            try {
                sDexOverrideMap.put(new File(info.apkPath).getCanonicalPath(), info);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getRedirectedPath(String origPath) {
        try {
            return nativeGetRedirectedPath(origPath);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        return origPath;
    }

    public static String resverseRedirectedPath(String origPath) {
        try {
            return nativeReverseRedirectedPath(origPath);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        return origPath;
    }


    public static void redirectDirectory(String origPath, String newPath) {
        if (!origPath.endsWith("/")) {
            origPath = origPath + "/";
        }
        if (!newPath.endsWith("/")) {
            newPath = newPath + "/";
        }
        try {
            nativeIORedirect(origPath, newPath);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void redirectFile(String origPath, String newPath) {
        if (origPath.endsWith("/")) {
            origPath = origPath.substring(0, origPath.length() - 1);
        }
        if (newPath.endsWith("/")) {
            newPath = newPath.substring(0, newPath.length() - 1);
        }

        try {
            nativeIORedirect(origPath, newPath);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void whitelistFile(String path) {
        try {
            nativeIOWhitelist(path);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void whitelist(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        try {
            nativeIOWhitelist(path);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void forbid(String path, boolean file) {
        if (!file && !path.endsWith("/")) {
            path = path + "/";
        }
        try {
            nativeIOForbid(path);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void enableIORedirect(boolean needDlOpen) {
        try {
            String soPath = new File(VirtualCore.get().getContext().getApplicationInfo().dataDir,
                    "lib/lib" + LIB_NAME + ".so").getAbsolutePath();
            nativeEnableIORedirect(soPath, Build.VERSION.SDK_INT, BuildCompat.getPreviewSDKInt(), needDlOpen);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void launchEngine() {
        if (sFlag) {
            return;
        }
        Method[] methods = {NativeMethods.gOpenDexFileNative, NativeMethods.gCameraNativeSetup, NativeMethods.gAudioRecordNativeCheckPermission, NativeMethods.gCameraStartPreview, NativeMethods.gCameraNativeTakePicture, NativeMethods.gAudioRecordStart,NativeMethods.gMediaRecordPrepare};
        try {
            nativeLaunchEngine(methods, VirtualCore.get().getHostPkg(), VirtualRuntime.isArt(), Build.VERSION.SDK_INT, NativeMethods.gCameraMethodType);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        sFlag = true;
    }

    public static boolean onKillProcess(int pid, int signal) {
        VLog.e(TAG, "killProcess: pid = %d, signal = %d.", pid, signal);
        if (pid == Process.myPid()) {
            VLog.e(TAG, VLog.getStackTraceString(new Throwable()));
        }
        return true;
    }

    public static int onGetCallingUid(int originUid) {
        int callingPid = Binder.getCallingPid();
        if (callingPid == VirtualCore.get().getSystemPid()) {
            return Process.SYSTEM_UID;
        }else if(callingPid == Process.myPid()){
            return VClient.get().getVUid();
        }
        //fix:getCallingUid
        return VClient.get().getVCallingUid();
    }

    public static void onOpenDexFileNative(String[] params) {
        String dexOrJarPath = params[0];
        String outputPath = params[1];
        VLog.d(TAG, "beforeOpenDexFileNative(\"%s\", \"%s\")", dexOrJarPath, outputPath);
        try {
            String canonical = new File(dexOrJarPath).getCanonicalPath();
            InstalledAppInfo info = sDexOverrideMap.get(canonical);
            if (info != null && !info.notCopyApk) {
                outputPath = info.getOdexFile().getPath();
                params[1] = outputPath;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static native void nativeLaunchEngine(Object[] method, String hostPackageName, boolean isArt, int apiLevel, int cameraMethodType);

    private static native void nativeMark();

    private static native String nativeReverseRedirectedPath(String redirectedPath);

    private static native String nativeGetRedirectedPath(String orgPath);

    private static native void nativeIORedirect(String origPath, String newPath);

    private static native void nativeIOWhitelist(String path);

    private static native void nativeIOForbid(String path);

    public static native boolean nativeCloseAllSocket();

    public static native void nativeChangeDecryptState(boolean state);

    public static native boolean nativeGetDecryptState();

    private static native void nativeDlOpenWhitelist(String path);

    private static native void nativeEnableIORedirect(String selfSoPath, int apiLevel, int previewApiLevel, boolean needDlOpen);

    public static int onGetUid(int uid) {
        Thread.currentThread().setContextClassLoader(VClient.class.getClassLoader());
        return VClient.get().getBaseVUid();
    }
}
