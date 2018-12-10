package com.lody.virtual.client;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Process;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.natives.NativeMethods;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * VirtualApp Native Project
 */
public class NativeEngine {

    private static final String TAG = NativeEngine.class.getSimpleName();

    private static final List<DexOverride> sDexOverrides = new ArrayList<>();

    private static boolean sFlag = false;
    private static boolean sEnabled = false;
    private static boolean sBypassedP = false;

    private static final String LIB_NAME = "v++";
    private static final String LIB_NAME_64 = "v++_64";

    static {
        try {
            if (VirtualRuntime.is64bit()) {
                System.loadLibrary(LIB_NAME_64);
            } else {
                System.loadLibrary(LIB_NAME);
            }
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }


    public static void startDexOverride() {
        List<InstalledAppInfo> installedApps = VirtualCore.get().getInstalledApps(0);
        for (InstalledAppInfo info : installedApps) {
            if (info.appMode != InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK) {
                String originDexPath = getCanonicalPath(info.getApkPath());
                DexOverride override = new DexOverride(originDexPath, null, null, info.getOdexPath());
                sDexOverrides.add(override);
            }
        }
        for (String framework : StubManifest.REQUIRED_FRAMEWORK) {
            File zipFile = VEnvironment.getFrameworkFile32(framework);
            File odexFile = VEnvironment.getOptimizedFrameworkFile32(framework);
            if (zipFile.exists() && odexFile.exists()) {
                String systemFilePath = "/system/framework/" + framework + ".jar";
                sDexOverrides.add(new DexOverride(systemFilePath, zipFile.getPath(), null, odexFile.getPath()));
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

    public static void readOnlyFile(String path) {
        try {
            nativeIOReadOnly(path);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void readOnly(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        try {
            nativeIOReadOnly(path);
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

    public static void enableIORedirect() {
        if (sEnabled) {
            return;
        }
        ApplicationInfo coreAppInfo;
        try {
            coreAppInfo = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(VirtualCore.getConfig().getHostPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            String soPath = new File(coreAppInfo.nativeLibraryDir, "lib" + LIB_NAME + ".so").getAbsolutePath();
            String soPath64 = new File(coreAppInfo.nativeLibraryDir, "lib" + LIB_NAME_64 + ".so").getAbsolutePath();
            nativeEnableIORedirect(soPath, soPath64, Build.VERSION.SDK_INT, BuildCompat.getPreviewSDKInt());
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        sEnabled = true;
    }

    public static void launchEngine() {
        if (sFlag) {
            return;
        }
        Object[] methods = {NativeMethods.gOpenDexFileNative, NativeMethods.gCameraNativeSetup, NativeMethods.gAudioRecordNativeCheckPermission,
                NativeMethods.gCameraStartPreview, NativeMethods.gCameraNativeTakePicture, NativeMethods.gAudioRecordStart,NativeMethods.gMediaRecordPrepare,
                NativeMethods.gMediaRecorderNativeSetup, NativeMethods.gAudioRecordNativeSetup};
        try {
            nativeLaunchEngine(methods, VirtualCore.get().getHostPkg(), VirtualRuntime.isArt(), Build.VERSION.SDK_INT, NativeMethods.gCameraMethodType, NativeMethods.gAudioRecordMethodType);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        sFlag = true;
    }

    public static void bypassHiddenAPIEnforcementPolicyIfNeeded() {
        if (sBypassedP) {
            return;
        }
        if (BuildCompat.isPie()) {
            try {
                nativeBypassHiddenAPIEnforcementPolicy();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        sBypassedP = true;
    }

    public static boolean onKillProcess(int pid, int signal) {
        VLog.e(TAG, "killProcess: pid = %d, signal = %d.", pid, signal);
        if (pid == Process.myPid()) {
            VLog.e(TAG, VLog.getStackTraceString(new Throwable()));
        }
        return true;
    }

    public static int onGetCallingUid(int originUid) {
        if (!VClient.get().isAppRunning()) {
            return originUid;
        }
        int callingPid = Binder.getCallingPid();
        if (callingPid == Process.myUid()) {
            return VClient.get().getVUid();
        }
        return VActivityManager.get().getUidByPid(callingPid);
    }

    private static DexOverride findDexOverride(String originDexPath) {
        for (DexOverride dexOverride : sDexOverrides) {
            if (dexOverride.originDexPath.equals(originDexPath)) {
                return dexOverride;
            }
        }
        return null;
    }

    public static void onOpenDexFileNative(String[] params) {
        String dexPath = params[0];
        String odexPath = params[1];
        if (dexPath != null) {
            String dexCanonicalPath = getCanonicalPath(dexPath);
            DexOverride override = findDexOverride(dexCanonicalPath);
            if (override != null) {
                if (override.newDexPath != null) {
                    params[0] = override.newDexPath;
                }
                odexPath = override.newDexPath;
                if (override.originOdexPath != null) {
                    String odexCanonicalPath = getCanonicalPath(odexPath);
                    if (odexCanonicalPath.equals(override.originOdexPath)) {
                        params[1] = override.newOdexPath;
                    }
                } else {
                    params[1] = override.newOdexPath;
                }
            }
        }
        VLog.i(TAG, "OpenDexFileNative(\"%s\", \"%s\")", dexPath, odexPath);
    }

    private static final String getCanonicalPath(String path) {
        File file = new File(path);
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    private static native void nativeLaunchEngine(Object[] method, String hostPackageName, boolean isArt, int apiLevel, int cameraMethodType, int audioRecordMethodType);

    private static native void nativeMark();

    private static native String nativeReverseRedirectedPath(String redirectedPath);

    private static native String nativeGetRedirectedPath(String origPath);

    private static native void nativeIORedirect(String origPath, String newPath);

    private static native void nativeIOWhitelist(String path);

    private static native void nativeIOForbid(String path);

    public static native boolean nativeCloseAllSocket();

    public static native void nativeChangeDecryptState(boolean state);

    public static native boolean nativeGetDecryptState();

    private static native void nativeIOReadOnly(String path);

    private static native void nativeEnableIORedirect(String soPath, String soPath64, int apiLevel, int previewApiLevel);

    private static native void nativeBypassHiddenAPIEnforcementPolicy();

    public static int onGetUid(int uid) {
        if (!VClient.get().isAppRunning()) {
            return uid;
        }
        return VClient.get().getBaseVUid();
    }
}
