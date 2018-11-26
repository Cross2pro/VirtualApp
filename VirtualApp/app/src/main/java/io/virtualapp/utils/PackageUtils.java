package io.virtualapp.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;

public class PackageUtils {
    public static PackageInfo getApkPackageInfo(PackageManager pm, String path, int flags) {
        try {
            return pm.getPackageArchiveInfo(path, flags);
        } catch (Throwable e) {
            return null;
        }
    }

    public static int getApkVersion(Context cxt, String path) {
        PackageInfo packageInfo = getApkPackageInfo(cxt.getPackageManager(), path, 0);
        if (packageInfo == null) {
            return -1;
        }
        return packageInfo.versionCode;
    }

    public static int getApkVersion(PackageManager pm, String path) {
        PackageInfo packageInfo = getApkPackageInfo(pm, path, 0);
        if (packageInfo == null) {
            return -1;
        }
        return packageInfo.versionCode;
    }

    public static void checkUpdate(String packageName) {
        InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        if (installedAppInfo == null || !installedAppInfo.notCopyApk) {
            return;
        }
        checkUpdate(installedAppInfo, packageName);
    }

    public static void checkUpdate(InstalledAppInfo installedAppInfo, String packageName) {
        if (!VirtualCore.get().isAppInstalled(packageName)) {
            return;
        }
        PackageInfo outsidePackageInfo = null;
        try {
            outsidePackageInfo = VirtualCore.get().getUnHookPackageManager().getPackageInfo(packageName, 0);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (outsidePackageInfo == null) {
            //uninstall
            return;
        }
        PackageInfo insidePackageInfo = installedAppInfo.getPackageInfo(0);
        //update apk
        if (!new File(installedAppInfo.apkPath).exists()
                || insidePackageInfo == null
                || outsidePackageInfo.versionCode != insidePackageInfo.versionCode) {
            VirtualCore.get().killApp(packageName, VUserHandle.USER_ALL);
            VirtualCore.get().installPackage(outsidePackageInfo.applicationInfo.publicSourceDir,
                    InstallStrategy.UPDATE_IF_EXIST | InstallStrategy.NOT_COPY_APK,
                    result -> {
                        // nothing
                    });
        }
    }
}
