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

    public static boolean checkUpdate(Context context, String packageName) {
        InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        if (installedAppInfo == null || !installedAppInfo.dependSystem) {
            return false;
        }
        return checkUpdate(context, installedAppInfo, packageName);
    }

    public static boolean checkUpdate(Context context, InstalledAppInfo installedAppInfo, String packageName) {
        if (!VirtualCore.get().isAppInstalled(packageName)) {
            return false;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = VirtualCore.get().getUnHookPackageManager().getPackageInfo(packageName, 0);
        } catch (Throwable e) {

        }
        if (packageInfo == null) {
            //uninstall
            return false;
        }
        PackageInfo vpackageInfo = installedAppInfo.getPackageInfo(0);
        //update apk
        if (!new File(installedAppInfo.apkPath).exists() || vpackageInfo == null || packageInfo.versionCode != vpackageInfo.versionCode) {
            VirtualCore.get().killApp(packageName, VUserHandle.USER_ALL);
            InstallResult result = VirtualCore.get().installPackage(packageInfo.applicationInfo.publicSourceDir,
                    InstallStrategy.UPDATE_IF_EXIST | InstallStrategy.DEPEND_SYSTEM_IF_EXIST);
            if (!result.isSuccess) {
                return false;
            }
        }
        return true;
    }
}
