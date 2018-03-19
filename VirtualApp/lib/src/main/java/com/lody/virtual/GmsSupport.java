package com.lody.virtual;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * @author Lody
 */
public class GmsSupport {
    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();

    static {
        GOOGLE_APP.add("com.android.vending");
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");
        GOOGLE_SERVICE.add("com.google.android.gsf");
        GOOGLE_SERVICE.add("com.google.android.gms");
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
    }

    public static boolean isGoogleFrameworkInstalled() {
        return VirtualCore.get().isAppInstalled("com.google.android.gms");
    }

    public static boolean isGoogleService(String packageName) {
        return GOOGLE_SERVICE.contains(packageName);
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str);
    }

    public static boolean isOutsideGoogleFrameworkExist() {
        return VirtualCore.get().isOutsideInstalled("com.google.android.gms");
    }

    private static void installPackages(Set<String> list, int userId) {
        VirtualCore core = VirtualCore.get();
        for (String packageName : list) {
            if (core.isAppInstalledAsUser(userId, packageName)) {
                continue;
            }
            ApplicationInfo info = null;
            try {
                info = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Ignore
            }
            if (info == null || info.sourceDir == null || !hasDex(info.sourceDir)) {
                continue;
            }
            if (userId == 0) {
                core.installPackage(info.sourceDir, InstallStrategy.DEPEND_SYSTEM_IF_EXIST);
            } else {
                core.installPackageAsUser(userId, packageName);
            }
        }
    }

    public static void installGApps(int userId) {
        installPackages(GOOGLE_SERVICE, userId);
        installPackages(GOOGLE_APP, userId);
        if (!VirtualCore.get().isAppInstalled("com.google.android.gsf")) {
            remove("com.google.android.gsf");
        }
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
    }

    public static boolean isInstalledGoogleService() {
        return VirtualCore.get().isAppInstalled("com.google.android.gms");
    }

    public static boolean hasDex(String apkPath) {
        boolean hasDex = false;
        if (apkPath != null) {
            if (!apkPath.contains("/system/app") && !apkPath.startsWith("/system/priv-app")) {
                return true;
            }

            try {
                ZipFile zipfile = new ZipFile(apkPath);
                if (zipfile.getEntry("classes.dex") != null) {
                    hasDex = true;
                }
                zipfile.close();
            } catch (Throwable e) {
                VLog.logbug("GmsSupport", "Error when find dex for path: " + apkPath);
                VLog.logbug("GmsSupport", VLog.getStackTraceString(e));
            }
            VLog.logbug("GmsSupport", "apk : " + apkPath + " hasDex: " + hasDex);
        }
        return hasDex;
    }
}