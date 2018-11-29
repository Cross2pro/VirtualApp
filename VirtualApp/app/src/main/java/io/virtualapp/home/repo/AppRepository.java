package io.virtualapp.home.repo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

import org.jdeferred.Promise;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.utils.PackageUtils;

/**
 * @author Lody
 */
public class AppRepository implements AppDataSource {

    private static final Collator COLLATOR = Collator.getInstance(Locale.CHINA);
    private final Map<String, String> mLabels = new HashMap<>();
    private static final List<String> SCAN_PATH_LIST = Arrays.asList(
            ".",
            "backups/apps",
            "wandoujia/app",
            "tencent/tassistant/apk",
            "BaiduAsa9103056",
            "360Download",
            "pp/downloader",
            "pp/downloader/apk",
            "pp/downloader/silent/apk");

    private Context mContext;

    public AppRepository(Context context) {
        mContext = context;
    }

    private static boolean isSystemApplication(PackageInfo packageInfo) {
        if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_PERSISTENT) != 0){
            return true;
    }
        if(packageInfo.applicationInfo.uid <= 1000){
            return true;
        }
        if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0){
            if(packageInfo.applicationInfo.labelRes == 0 && TextUtils.isEmpty(packageInfo.applicationInfo.nonLocalizedLabel)){
                return true;
            }
            if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0){
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public Promise<List<AppData>, Throwable, Void> getVirtualApps() {

        return VUiKit.defer().when(() -> {
            List<AppData> models = new ArrayList<>();
            //TODO multi app list by users
//            List<VUserInfo> vUserInfos = VUserManager.get().getUsers();
//            if (vUserInfos == null) {
//                vUserInfos = new ArrayList<>();
//            }
//            if (vUserInfos.size() == 0) {
//                vUserInfos.add(new VUserInfo(0));
//            }
//            for(VUserInfo vUserInfo:vUserInfos){
////                VirtualCore.GET_HIDDEN_APP 全部app
//                final int userId = vUserInfo.id;
//                List<InstalledAppInfo> infos = VirtualCore.get().getInstalledAppsAsUser(userId, 0);
//                for(InstalledAppInfo info : infos){
//                    PackageAppData data = new PackageAppData(mContext, info);
//                    if (userId == 0) {
//                        if (VirtualCore.get().isAppInstalledAsUser(0, info.packageName)) {
//                            models.add(data);
//                        }
//                    } else {
//                        models.add(new MultiplePackageAppData(data, userId));
//                    }
//                }
//                if(!VirtualCore.get().isAppInstalledAsUser(userId, "com.tencent.mobileqq")){
//                    if (userId != 0) {
//                        //从来没安装过QQ
//                        String path = "apk";
//                        int flags = 0;
//                        VirtualCore.get().installPackage(path, flags);
//                  } else {
//                        //安装过QQ
//                        VirtualCore.get().installPackageAsUser(userId, "com.tencent.mobileqq");
//                    }
//                }
//            }
            //TODO load multi app list by pkg
            List<InstalledAppInfo> infos = VirtualCore.get().getInstalledApps(0);
            for (InstalledAppInfo info : infos) {
                if (!VirtualCore.get().isPackageLaunchable(info.packageName)) {
                    continue;
                }
                //fix:无法获取名字和图标，有时候x进程没死，收不到升级广播
                if(info.notCopyApk){
                    PackageUtils.checkUpdate(info, info.packageName);
                }
                PackageAppData data = new PackageAppData(mContext, info);
                if (VirtualCore.get().isAppInstalledAsUser(0, info.packageName)) {
                    models.add(data);
                }
                mLabels.put(info.packageName, data.name);
                int[] userIds = info.getInstalledUsers();
                for (int userId : userIds) {
                    if (userId != 0) {
                        models.add(new MultiplePackageAppData(data, userId));
                    }
                }
            }
            return models;
        });
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getInstalledApps(Context context) {
        return VUiKit.defer().when(() -> convertPackageInfoToAppData(context, context.getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS), true, true));
    }

    @Override
    public Promise<List<AppInfo>, Throwable, Void> getStorageApps(Context context, File rootDir) {
        return VUiKit.defer().when(() -> convertPackageInfoToAppData(context, findAndParseAPKs(context, rootDir, SCAN_PATH_LIST), false, false));
    }

    private List<PackageInfo> findAndParseAPKs(Context context, File rootDir, List<String> paths) {
        List<PackageInfo> packageList = new ArrayList<>();
        if (paths == null)
            return packageList;
        for (String path : paths) {
            File[] dirFiles = new File(rootDir, path).listFiles();
            if (dirFiles == null)
                continue;
            for (File f : dirFiles) {
                if (!f.getName().toLowerCase().endsWith(".apk"))
                    continue;
                PackageInfo pkgInfo = null;
                try {
                    pkgInfo = context.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), PackageManager.GET_PERMISSIONS);
                    pkgInfo.applicationInfo.sourceDir = f.getAbsolutePath();
                    pkgInfo.applicationInfo.publicSourceDir = f.getAbsolutePath();
                } catch (Exception e) {
                    // Ignore
                }
                if (pkgInfo != null)
                    packageList.add(pkgInfo);
            }
        }
        return packageList;
    }

    private final Map<String, Boolean> mCache = new HashMap<>();

    private boolean isDisableNotCopyApk(PackageInfo pkg) {
        synchronized (mCache) {
            Boolean rs = mCache.get(pkg.packageName);
            if (rs != null && rs) {
                return true;
            }
        }
        //check
        File apk = new File(pkg.applicationInfo.publicSourceDir);
        Set<String> soList = NativeLibraryHelperCompat.getSoListFromApk(apk);
        if (soList == null || soList.contains("libme_unipay.so")) {
            synchronized (mCache) {
                mCache.put(pkg.packageName, true);
            }
            return true;
        }
        for (String so : soList) {
            if (so.startsWith("libshella-")) {
                synchronized (mCache) {
                    mCache.put(pkg.packageName, true);
                }
                return true;
            }
        }
        synchronized (mCache) {
            mCache.put(pkg.packageName, false);
        }
        return false;
    }
    private boolean isGameFramework(String pkg){
        return "com.huawei.hwid".equals(pkg);
    }

    private List<AppInfo> convertPackageInfoToAppData(Context context, List<PackageInfo> pkgList,
                                                      boolean notCopyApk, boolean hideGApps) {
        PackageManager pm = context.getPackageManager();
        List<AppInfo> list = new ArrayList<>(pkgList.size());
        for (PackageInfo pkg : pkgList) {
            // ignore the host package
            if (StubManifest.isHostPackageName(pkg.packageName)) {
                continue;
            }
            if (hideGApps && GmsSupport.isGoogleAppOrService(pkg.packageName)) {
                continue;
            }
            // ignore the System package
            if (notCopyApk) {
                if(isSystemApplication(pkg)){
                    continue;
                }
                if (!isGameFramework(pkg.packageName) && pm.getLaunchIntentForPackage(pkg.packageName) == null) {
                    continue;
                }
            }
            if (notCopyApk && !GmsSupport.hasDex(pkg.applicationInfo.publicSourceDir)) {
                continue;
            }
            //TODO all can add,but lunch need check 64lib

            ApplicationInfo ai = pkg.applicationInfo;
            String path = ai.publicSourceDir != null ? ai.publicSourceDir : ai.sourceDir;
            if (path == null) {
                continue;
            }
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(pkg.packageName, 0);
            AppInfo info = new AppInfo();
            info.packageName = pkg.packageName;
            info.fastOpen = notCopyApk;
            info.path = path;
            info.icon = ai.loadIcon(pm);
            info.name = ai.loadLabel(pm);
            info.targetSdkVersion = pkg.applicationInfo.targetSdkVersion;
            info.requestedPermissions = pkg.requestedPermissions;
            if (installedAppInfo != null) {
                info.path = installedAppInfo.getApkPath();
                info.cloneCount = installedAppInfo.getInstalledUsers().length;
            }
            list.add(info);
        }
        return list;
    }

    @Override
    public InstallResult addVirtualApp(AppInfoLite info) {
        int flags = InstallStrategy.COMPARE_VERSION;
        if (info.notCopyApk) {
            flags |= InstallStrategy.NOT_COPY_APK;
        }
        return VirtualCore.get().installPackageSync(info.path, flags);
    }

    @Override
    public boolean removeVirtualApp(String packageName, int userId) {
        return VirtualCore.get().uninstallPackageAsUser(packageName, userId);
    }

    @Override
    public String getLabel(String packageName) {
        String label = mLabels.get(packageName);
        if (label == null) {
            return packageName;
        }
        return label;
    }
}
