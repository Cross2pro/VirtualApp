package com.lody.virtual.server.pm;

import android.content.pm.PackageInfo;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.pm.parser.PackageParserEx;
import com.lody.virtual.server.pm.parser.VPackage;

/**
 * @author Lody
 */

public class PackageCacheManager {

    static final ArrayMap<String, VPackage> PACKAGE_CACHE = new ArrayMap<String, VPackage>() {
        @Override
        public VPackage get(Object key) {
            VPackage p = super.get(key);
            if (p == null) {
                return null;
            }
            PackageSetting ps = (PackageSetting) p.mExtras;
            if (ps.flag != InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK) {
                return p;
            }
            try {
                PackageInfo outsideInfo = VirtualCore.get().getUnHookPackageManager().getPackageInfo(p.packageName, 0);
                if (p.mVersionCode < outsideInfo.versionCode) {
                    VPackage newPackage = VAppManagerService.get().updatePackageCache(outsideInfo.applicationInfo.publicSourceDir, p.packageName);
                    newPackage.mExtras = p.mExtras;
                    PACKAGE_CACHE.put(p.packageName, newPackage);
                    return newPackage;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return p;
        }
    };

    public static int size() {
        synchronized (PACKAGE_CACHE) {
            return PACKAGE_CACHE.size();
        }
    }

    public static void put(VPackage pkg, PackageSetting ps) {
        synchronized (PackageCacheManager.class) {
            PackageParserEx.initApplicationInfoBase(ps, pkg);
            PACKAGE_CACHE.put(pkg.packageName, pkg);
            pkg.mExtras = ps;
            VPackageManagerService.get().analyzePackageLocked(pkg);
        }
    }

    public static VPackage get(String packageName) {
        synchronized (PackageCacheManager.class) {
            return PACKAGE_CACHE.get(packageName);
        }
    }

    public static PackageSetting getSetting(String packageName) {
        synchronized (PackageCacheManager.class) {
            VPackage p = PACKAGE_CACHE.get(packageName);
            if (p != null) {
                return (PackageSetting) p.mExtras;
            }
            return null;
        }
    }

    public static VPackage remove(String packageName) {
        synchronized (PackageCacheManager.class) {
            VPackageManagerService.get().deletePackageLocked(packageName);
            return PACKAGE_CACHE.remove(packageName);
        }
    }
}
