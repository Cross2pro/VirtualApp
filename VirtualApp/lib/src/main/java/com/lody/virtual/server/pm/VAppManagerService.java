package com.lody.virtual.server.pm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.ArtDexOptimizer;
import com.lody.virtual.helper.collection.IntArray;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.NativeLibraryHelperCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.accounts.VAccountManagerService;
import com.lody.virtual.server.am.BroadcastSystem;
import com.lody.virtual.server.am.UidSystem;
import com.lody.virtual.server.am.VActivityManagerService;
import com.lody.virtual.server.bit64.V64BitHelper;
import com.lody.virtual.server.interfaces.IAppManager;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.lody.virtual.server.interfaces.IPackageObserver;
import com.lody.virtual.server.notification.VNotificationManagerService;
import com.lody.virtual.server.pm.parser.PackageParserEx;
import com.lody.virtual.server.pm.parser.VPackage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dalvik.system.DexFile;

import static com.lody.virtual.remote.InstalledAppInfo.MODE_APP_COPY_APK;
import static com.lody.virtual.remote.InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK;


/**
 * @author Lody
 */
public class VAppManagerService extends IAppManager.Stub {

    private static final String TAG = VAppManagerService.class.getSimpleName();
    private static final Singleton<VAppManagerService> sService = new Singleton<VAppManagerService>() {
        @Override
        protected VAppManagerService create() {
            return new VAppManagerService();
        }
    };
    private final UidSystem mUidSystem = new UidSystem();
    private final PackagePersistenceLayer mPersistenceLayer = new PackagePersistenceLayer(this);
    private final Set<String> mVisibleOutsidePackages = new HashSet<>();
    private boolean mBooting;
    private RemoteCallbackList<IPackageObserver> mRemoteCallbackList = new RemoteCallbackList<>();
    private IAppRequestListener mAppRequestListener;


    public static VAppManagerService get() {
        return sService.get();
    }

    public static void systemReady() {
        VEnvironment.systemReady();
        if (!BuildCompat.isPie()) {
            get().extractRequiredFrameworks();
        }
        get().mUidSystem.initUidList();
    }

    public boolean isBooting() {
        return mBooting;
    }

    private void extractRequiredFrameworks() {
        for (String framework : StubManifest.REQUIRED_FRAMEWORK) {
            File zipFile = VEnvironment.getFrameworkFile32(framework);
            File odexFile = VEnvironment.getOptimizedFrameworkFile32(framework);
            if (!odexFile.exists()) {
                OatHelper.extractFrameworkFor32Bit(framework, zipFile, odexFile);
            }
        }
    }

    @Override
    public void scanApps() {
        if (mBooting) {
            return;
        }
        synchronized (this) {
            mBooting = true;
            mPersistenceLayer.read();
            if (mPersistenceLayer.changed) {
                mPersistenceLayer.changed = false;
                mPersistenceLayer.save();
                VLog.w(TAG, "Package PersistenceLayer updated.");
            }
            PrivilegeAppOptimizer.get().performOptimizeAllApps();
            mBooting = false;
        }
    }

    private void cleanUpResidualFiles(PackageSetting ps) {
        VLog.e(TAG, "cleanup residual files for : %s", ps.packageName);
        uninstallPackageFully(ps, false);
    }


    public void onUserCreated(VUserInfo userInfo) {
        VEnvironment.getUserDataDirectory(userInfo.id).mkdirs();
    }


    synchronized boolean loadPackage(PackageSetting setting) {
        if (!loadPackageInnerLocked(setting)) {
            cleanUpResidualFiles(setting);
            return false;
        }
        return true;
    }

    private boolean loadPackageInnerLocked(PackageSetting ps) {
        if (ps.appMode == InstalledAppInfo.MODE_APP_USE_OUTSIDE_APK) {
            if (!VirtualCore.get().isOutsideInstalled(ps.packageName)) {
                return false;
            }

        }
        File cacheFile = VEnvironment.getPackageCacheFile(ps.packageName);
        VPackage pkg = null;
        try {
            pkg = PackageParserEx.readPackageCache(ps.packageName);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (pkg == null || pkg.packageName == null) {
            return false;
        }
        VEnvironment.chmodPackageDictionary(cacheFile);
        PackageCacheManager.put(pkg, ps);
        BroadcastSystem.get().startApp(pkg);
        return true;
    }

    @Override
    public boolean isOutsidePackageVisible(String pkg) {
        return pkg != null && mVisibleOutsidePackages.contains(pkg);
    }

    @Override
    public void addVisibleOutsidePackage(String pkg) {
        if (pkg != null) {
            mVisibleOutsidePackages.add(pkg);
        }
    }

    @Override
    public void removeVisibleOutsidePackage(String pkg) {
        if (pkg != null) {
            mVisibleOutsidePackages.remove(pkg);
        }
    }

    @Override
    public void installPackage(String path, int flags, ResultReceiver receiver) {
        InstallResult res;
        synchronized (this) {
            res = installPackageImpl(path, flags, true);
        }
        if (receiver != null) {
            android.os.Bundle data = new Bundle();
            data.putParcelable("result", res);
            receiver.send(0, data);
        }
    }

    @Override
    public void requestCopyPackage64(String packageName) {
        /**
         * Lock VAMS avoid two process invoke this method Simultaneously.
         */
        synchronized (VActivityManagerService.get()) {
            PackageSetting ps = PackageCacheManager.getSetting(packageName);
            if (ps != null && ps.appMode == MODE_APP_USE_OUTSIDE_APK) {
                V64BitHelper.copyPackage64(ps.getApkPath(false), packageName);
            }
        }
    }

    public InstallResult installPackage(String path, int flags, boolean notify) {
        synchronized (this) {
            return installPackageImpl(path, flags, notify);
        }
    }


    private InstallResult installPackageImpl(String path, int flags, boolean notify) {
        long installTime = System.currentTimeMillis();
        if (path == null) {
            return InstallResult.makeFailure("path = NULL");
        }
        File packageFile = new File(path);
        if (!packageFile.exists() || !packageFile.isFile()) {
            return InstallResult.makeFailure("Package File is not exist.");
        }
        VPackage pkg = null;
        try {
            pkg = PackageParserEx.parsePackage(packageFile);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (pkg == null || pkg.packageName == null) {
            return InstallResult.makeFailure("Unable to parse the package.");
        }
        InstallResult res = new InstallResult();
        res.packageName = pkg.packageName;
        // PackageCache holds all packages, try to check if we need to update.
        VPackage existOne = PackageCacheManager.get(pkg.packageName);
        PackageSetting existSetting = existOne != null ? (PackageSetting) existOne.mExtras : null;
        if (existOne != null) {
            if ((flags & InstallStrategy.IGNORE_NEW_VERSION) != 0) {
                res.isUpdate = true;
                return res;
            }
            if (!canUpdate(existOne, pkg, flags)) {
                return InstallResult.makeFailure("Not allowed to update the package.");
            }
            res.isUpdate = true;
        }
        boolean notCopyApk = (flags & InstallStrategy.NOT_COPY_APK) != 0;

        if (existSetting != null && existSetting.appMode == MODE_APP_USE_OUTSIDE_APK) {
            notCopyApk = false;
        }
        if (existOne != null) {
            PackageCacheManager.remove(pkg.packageName);
        }
        PackageSetting ps;
        if (existSetting != null) {
            ps = existSetting;
        } else {
            ps = new PackageSetting();
        }

        Set<String> abiList = NativeLibraryHelperCompat.getSupportAbiList(packageFile.getPath());
        boolean support64bit = false, support32bit = false;
        if (abiList.isEmpty()) {
            support32bit = true;
        } else {
            if (NativeLibraryHelperCompat.contain64bitAbi(abiList)) {
                support64bit = true;
            }
            if (NativeLibraryHelperCompat.contain32BitAbi(abiList)) {
                support32bit = true;
            }
        }
        if (support32bit) {
            if (support64bit) {
                ps.flag = PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT;
            } else {
                ps.flag = PackageSetting.FLAG_RUN_32BIT;
            }
        } else {
            ps.flag = PackageSetting.FLAG_RUN_64BIT;
        }
        NativeLibraryHelperCompat.copyNativeBinaries(packageFile, VEnvironment.getAppLibDirectory(pkg.packageName));

        if (!notCopyApk) {
            File privatePackageFile = VEnvironment.getPackageResourcePath(pkg.packageName);
            try {
                FileUtils.copyFile(packageFile, privatePackageFile);
            } catch (IOException e) {
                privatePackageFile.delete();
                return InstallResult.makeFailure("Unable to copy the package file.");
            }
            packageFile = privatePackageFile;
            VEnvironment.chmodPackageDictionary(packageFile);
        }

        if (support64bit && !notCopyApk) {
            V64BitHelper.copyPackage64(packageFile.getPath(), pkg.packageName);
        }

        ps.appMode = notCopyApk ? MODE_APP_USE_OUTSIDE_APK : MODE_APP_COPY_APK;
        ps.packageName = pkg.packageName;
        ps.appId = VUserHandle.getAppId(mUidSystem.getOrCreateUid(pkg));
        if (res.isUpdate) {
            ps.lastUpdateTime = installTime;
        } else {
            ps.firstInstallTime = installTime;
            ps.lastUpdateTime = installTime;
            for (int userId : VUserManagerService.get().getUserIds()) {
                boolean installed = userId == 0;
                ps.setUserState(userId, false/*launched*/, false/*hidden*/, installed);
            }
        }
        PackageParserEx.savePackageCache(pkg);
        PackageCacheManager.put(pkg, ps);
        mPersistenceLayer.save();
        if (support32bit && !notCopyApk) {
            if (VirtualRuntime.isArt()) {
                try {
                    ArtDexOptimizer.interpretDex2Oat(packageFile.getPath(), VEnvironment.getOdexFile(ps.packageName).getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    DexFile.loadDex(packageFile.getPath(), VEnvironment.getOdexFile(ps.packageName).getPath(), 0).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        BroadcastSystem.get().startApp(pkg);
        if (notify) {
            notifyAppInstalled(ps, -1);
        }
        res.isSuccess = true;
        return res;
    }


    @Override
    public synchronized boolean installPackageAsUser(int userId, String packageName) {
        if (VUserManagerService.get().exists(userId)) {
            PackageSetting ps = PackageCacheManager.getSetting(packageName);
            if (ps != null) {
                if (!ps.isInstalled(userId)) {
                    ps.setInstalled(userId, true);
                    notifyAppInstalled(ps, userId);
                    mPersistenceLayer.save();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canUpdate(VPackage existOne, VPackage newOne, int flags) {
        if ((flags & InstallStrategy.FORCE_UPDATE) != 0) {
            return true;
        }
        if ((flags & InstallStrategy.COMPARE_VERSION) != 0) {
            if (existOne.mVersionCode < newOne.mVersionCode) {
                return true;
            }
        }
        if ((flags & InstallStrategy.TERMINATE_IF_EXIST) != 0) {
            return false;
        }
        return false;
    }


    @Override
    public synchronized boolean uninstallPackage(String packageName) {
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        if (ps != null) {
            uninstallPackageFully(ps, true);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean uninstallPackageAsUser(String packageName, int userId) {
        if (!VUserManagerService.get().exists(userId)) {
            return false;
        }
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        if (ps != null) {
            int[] userIds = getPackageInstalledUsers(packageName);
            if (!ArrayUtils.contains(userIds, userId)) {
                return false;
            }
            if (userIds.length == 1) {
                uninstallPackageFully(ps, true);
            } else {
                // Just hidden it
                VActivityManagerService.get().killAppByPkg(packageName, userId);
                ps.setInstalled(userId, false);
                mPersistenceLayer.save();
                deletePackageDataAsUser(userId, ps);
                notifyAppUninstalled(ps, userId);
            }
            return true;
        }
        return false;
    }

    private boolean isPackageSupport32Bit(PackageSetting ps) {
        return ps.flag == PackageSetting.FLAG_RUN_32BIT
                || ps.flag == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT;
    }

    private boolean isPackageSupport64Bit(PackageSetting ps) {
        return ps.flag == PackageSetting.FLAG_RUN_64BIT
                || ps.flag == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT;
    }

    private void deletePackageDataAsUser(int userId, PackageSetting ps) {
        if (isPackageSupport32Bit(ps)) {
            FileUtils.deleteDir(VEnvironment.getDataUserPackageDirectory(userId, ps.packageName));
        }
        if (isPackageSupport64Bit(ps)) {
            V64BitHelper.uninstallPackageData64(userId, ps.packageName);
        }
        VNotificationManagerService.get().cancelAllNotification(ps.packageName, userId);
    }

    private void uninstallPackageFully(PackageSetting ps, boolean notify) {
        String packageName = ps.packageName;
        try {
            BroadcastSystem.get().stopApp(packageName);
            VActivityManagerService.get().killAppByPkg(packageName, VUserHandle.USER_ALL);
            if (isPackageSupport32Bit(ps)) {
                VEnvironment.getPackageResourcePath(packageName).delete();
                FileUtils.deleteDir(VEnvironment.getDataAppPackageDirectory(packageName));
                VEnvironment.getOdexFile(packageName).delete();
                for (int id : VUserManagerService.get().getUserIds()) {
                    deletePackageDataAsUser(id, ps);
                }
            }
            if (isPackageSupport64Bit(ps)) {
                V64BitHelper.uninstallPackageData64(-1, packageName);
            }
            PackageCacheManager.remove(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (notify) {
                notifyAppUninstalled(ps, -1);
            }
        }
    }

    @Override
    public int[] getPackageInstalledUsers(String packageName) {
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        if (ps != null) {
            IntArray installedUsers = new IntArray(5);
            int[] userIds = VUserManagerService.get().getUserIds();
            for (int userId : userIds) {
                if (ps.readUserState(userId).installed) {
                    installedUsers.add(userId);
                }
            }
            return installedUsers.getAll();
        }
        return new int[0];
    }

    @Override
    public List<InstalledAppInfo> getInstalledApps(int flags) {
        List<InstalledAppInfo> infoList = new ArrayList<>(getInstalledAppCount());
        for (VPackage p : PackageCacheManager.PACKAGE_CACHE.values()) {
            PackageSetting setting = (PackageSetting) p.mExtras;
            infoList.add(setting.getAppInfo());
        }
        return infoList;
    }

    @Override
    public List<InstalledAppInfo> getInstalledAppsAsUser(int userId, int flags) {
        List<InstalledAppInfo> infoList = new ArrayList<>(getInstalledAppCount());
        for (VPackage p : PackageCacheManager.PACKAGE_CACHE.values()) {
            PackageSetting setting = (PackageSetting) p.mExtras;
            boolean visible = setting.isInstalled(userId);
            if ((flags & VirtualCore.GET_HIDDEN_APP) == 0 && setting.isHidden(userId)) {
                visible = false;
            }
            if (visible) {
                infoList.add(setting.getAppInfo());
            }
        }
        return infoList;
    }

    @Override
    public int getInstalledAppCount() {
        return PackageCacheManager.PACKAGE_CACHE.size();
    }

    @Override
    public boolean isAppInstalled(String packageName) {
        return packageName != null && PackageCacheManager.PACKAGE_CACHE.containsKey(packageName);
    }

    @Override
    public boolean isAppInstalledAsUser(int userId, String packageName) {
        if (packageName == null || !VUserManagerService.get().exists(userId)) {
            return false;
        }
        PackageSetting setting = PackageCacheManager.getSetting(packageName);
        if (setting == null) {
            return false;
        }
        return setting.isInstalled(userId);
    }

    private void notifyAppInstalled(PackageSetting setting, int userId) {
        final String pkg = setting.packageName;
        int N = mRemoteCallbackList.beginBroadcast();
        while (N-- > 0) {
            try {
                if (userId == -1) {
                    mRemoteCallbackList.getBroadcastItem(N).onPackageInstalled(pkg);
                    mRemoteCallbackList.getBroadcastItem(N).onPackageInstalledAsUser(0, pkg);

                } else {
                    mRemoteCallbackList.getBroadcastItem(N).onPackageInstalledAsUser(userId, pkg);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        sendInstalledBroadcast(pkg, new VUserHandle(userId));
        mRemoteCallbackList.finishBroadcast();
        VAccountManagerService.get().refreshAuthenticatorCache(null);
    }

    private void notifyAppUninstalled(PackageSetting setting, int userId) {
        final String pkg = setting.packageName;
        int N = mRemoteCallbackList.beginBroadcast();
        while (N-- > 0) {
            try {
                if (userId == -1) {
                    mRemoteCallbackList.getBroadcastItem(N).onPackageUninstalled(pkg);
                    mRemoteCallbackList.getBroadcastItem(N).onPackageUninstalledAsUser(0, pkg);
                } else {
                    mRemoteCallbackList.getBroadcastItem(N).onPackageUninstalledAsUser(userId, pkg);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        sendUninstalledBroadcast(pkg, new VUserHandle(userId));
        mRemoteCallbackList.finishBroadcast();
        VAccountManagerService.get().refreshAuthenticatorCache(null);
    }


    public void sendInstalledBroadcast(String packageName, VUserHandle user) {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_ADDED);
        intent.setData(Uri.parse("package:" + packageName));
        VActivityManagerService.get().sendBroadcastAsUser(intent, user);
    }

    public void sendUninstalledBroadcast(String packageName, VUserHandle user) {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_REMOVED);
        intent.setData(Uri.parse("package:" + packageName));
        VActivityManagerService.get().sendBroadcastAsUser(intent, user);
    }

    public void sendUpdateBroadcast(String packageName, VUserHandle user) {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_REPLACED);
        intent.setData(Uri.parse("package:" + packageName));
        VActivityManagerService.get().sendBroadcastAsUser(intent, user);
    }

    @Override
    public void registerObserver(IPackageObserver observer) {
        try {
            mRemoteCallbackList.register(observer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregisterObserver(IPackageObserver observer) {
        try {
            mRemoteCallbackList.unregister(observer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public IAppRequestListener getAppRequestListener() {
        return mAppRequestListener;
    }

    @Override
    public void setAppRequestListener(final IAppRequestListener listener) {
        this.mAppRequestListener = listener;
        if (listener != null) {
            try {
                listener.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        listener.asBinder().unlinkToDeath(this, 0);
                        VAppManagerService.this.mAppRequestListener = null;
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clearAppRequestListener() {
        this.mAppRequestListener = null;
    }

    @Override
    public InstalledAppInfo getInstalledAppInfo(String packageName, int flags) {
        synchronized (PackageCacheManager.class) {
            if (packageName != null) {
                PackageSetting setting = PackageCacheManager.getSetting(packageName);
                if (setting != null) {
                    return setting.getAppInfo();
                }
            }
            return null;
        }
    }

    @Override
    public boolean isRun64BitProcess(String packageName) {
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        return ps != null && ps.isRunOn64BitProcess();
    }

    @Override
    public synchronized boolean isIORelocateWork() {
        return true;
    }


    public boolean isPackageLaunched(int userId, String packageName) {
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        return ps != null && ps.isLaunched(userId);
    }

    public void setPackageHidden(int userId, String packageName, boolean hidden) {
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        if (ps != null && VUserManagerService.get().exists(userId)) {
            ps.setHidden(userId, hidden);
            mPersistenceLayer.save();
        }
    }

    public int getAppId(String packageName) {
        PackageSetting setting = PackageCacheManager.getSetting(packageName);
        return setting != null ? setting.appId : -1;
    }


    void restoreFactoryState() {
        VLog.w(TAG, "Warning: Restore the factory state...");
        VEnvironment.getDalvikCacheDirectory().delete();
        VEnvironment.getUserSystemDirectory().delete();
        VEnvironment.getDataAppDirectory().delete();
    }

    public void savePersistenceData() {
        mPersistenceLayer.save();
    }


    public InstallResult upgradePackage(String pkg, String path, int flag) {
        return installPackageImpl(path, flag, false);
    }

    public VPackage updatePackageCache(String newPath, String packageName) throws Throwable {
        VPackage newPackage = PackageParserEx.parsePackage(new File(newPath));
        PackageParserEx.savePackageCache(newPackage);
        return newPackage;
    }
}
