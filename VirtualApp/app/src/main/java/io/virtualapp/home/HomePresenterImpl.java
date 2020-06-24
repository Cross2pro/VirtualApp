package io.virtualapp.home;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.PermissionCompat;
import com.lody.virtual.open.MultiAppHelper;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.bit64.V64BitHelper;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.AppRepository;
import io.virtualapp.home.repo.PackageAppDataStorage;
import jonathanfinerty.once.Once;

import static io.virtualapp.VCommends.REQUEST_PERMISSION;

/**
 * @author Lody
 */
class HomePresenterImpl implements HomeContract.HomePresenter {

    private HomeContract.HomeView mView;
    private Activity mActivity;
    private AppRepository mRepo;


    HomePresenterImpl(HomeContract.HomeView view) {
        mView = view;
        mActivity = view.getActivity();
        mRepo = new AppRepository(mActivity);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        dataChanged();
        if (!Once.beenDone(VCommends.TAG_SHOW_ADD_APP_GUIDE)) {
            mView.showGuide();
            Once.markDone(VCommends.TAG_SHOW_ADD_APP_GUIDE);
        }
    }

    @Override
    public String getLabel(String packageName) {
        return mRepo.getLabel(packageName);
    }

    @Override
    public boolean check64bitEnginePermission() {
        if (VirtualCore.get().is64BitEngineInstalled()) {
            if (!V64BitHelper.has64BitEngineStartPermission()) {
                mView.showPermissionDialog();
                return true;
            }
        }
        return false;
    }

    @Override
    public void launchApp(AppData data) {
        try {
            int userId = data.getUserId();
            String packageName = data.getPackageName();
            if (userId != -1 && packageName != null) {
                boolean runAppNow = true;
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(packageName, userId);
                    ApplicationInfo applicationInfo = info.getApplicationInfo(userId);
                    boolean is64bit = VirtualCore.get().isRun64BitProcess(info.packageName);
                    if (is64bit) {
                        if (check64bitEnginePermission()) {
                            return;
                        }
                    }
                    if (PermissionCompat.isCheckPermissionRequired(applicationInfo.targetSdkVersion)) {
                        String[] permissions = VPackageManager.get().getDangrousPermissions(info.packageName);
                        if (!PermissionCompat.checkPermissions(permissions, is64bit)) {
                            runAppNow = false;
                            PermissionRequestActivity.requestPermission(mActivity, permissions, data.getName(), userId, packageName, REQUEST_PERMISSION);
                        }
                    }
                }
                if (runAppNow) {
                    data.isFirstOpen = false;
                    launchApp(userId, packageName);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void launchApp(int userId, String packageName) {
        if (VirtualCore.get().isRun64BitProcess(packageName)) {
            if (!VirtualCore.get().is64BitEngineInstalled()) {
                Toast.makeText(mActivity, "Please install 64bit engine.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!V64BitHelper.has64BitEngineStartPermission()) {
                Toast.makeText(mActivity, "No Permission to start 64bit engine.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        long lastTime = VActivityManager.get().getLastBackHomeTime();
        Log.e("kk-test", "getLastBackHomeTime="+lastTime);
        long time = System.currentTimeMillis() - lastTime;
        if (lastTime > 0 && time <= 6000 && "com.xdja.HDSafeEMailClient".equals(packageName)) {
            Log.e("kk-test", "stat app delay "+time);
            VUiKit.postDelayed(Math.max(2000, time), () -> {
                VActivityManager.get().launchApp(userId, packageName);
            });
        } else {
            Log.e("kk-test", "stat app");
            VActivityManager.get().launchApp(userId, packageName);
        }
    }


    @Override
    public void dataChanged() {
        mView.showLoading();
        mRepo.getVirtualApps().done(mView::loadFinish).fail(mView::loadError);
    }

    @Override
    public void addApp(AppInfoLite info) {
        class AddResult {
            private PackageAppData appData;
            private int userId;
        }
        AddResult addResult = new AddResult();
        ProgressDialog dialog = ProgressDialog.show(mActivity, null, mActivity.getString(R.string.tip_add_apps));
        VUiKit.defer().when(() -> {
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
            if (installedAppInfo != null) {
                addResult.userId = MultiAppHelper.installExistedPackage(installedAppInfo);
            } else {
                InstallResult res = mRepo.addVirtualApp(info);
                if (!res.isSuccess) {
                    throw new IllegalStateException();
                }
            }
        }).then((res) -> {
            addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
        }).fail((e) -> {
            dialog.dismiss();
        }).done(res -> {
            if (addResult.userId == 0) {
                PackageAppData data = addResult.appData;
                data.isLoading = true;
                mView.addAppToLauncher(data);
                handleLoadingApp(data);
            } else {
                MultiplePackageAppData data = new MultiplePackageAppData(addResult.appData, addResult.userId);
                data.isLoading = true;
                mView.addAppToLauncher(data);
                handleLoadingApp(data);
            }
            dialog.dismiss();
        });
    }


    private void handleLoadingApp(AppData data) {
        VUiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            time = System.currentTimeMillis() - time;
            if (time < 1500L) {
                try {
                    Thread.sleep(1500L - time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).done((res) -> {
            if (data instanceof PackageAppData) {
                ((PackageAppData) data).isLoading = false;
                ((PackageAppData) data).isFirstOpen = true;
            } else if (data instanceof MultiplePackageAppData) {
                ((MultiplePackageAppData) data).isLoading = false;
                ((MultiplePackageAppData) data).isFirstOpen = true;
            }
            mView.refreshLauncherItem(data);
        });
    }

    @Override
    public void deleteApp(AppData data) {
        mView.removeAppToLauncher(data);
        ProgressDialog dialog = ProgressDialog.show(mActivity, mActivity.getString(R.string.tip_delete), data.getName());
        VUiKit.defer().when(() -> {
            mRepo.removeVirtualApp(data.getPackageName(), data.getUserId());
        }).fail((e) -> {
            dialog.dismiss();
        }).done((rs) -> {
            dialog.dismiss();
        });
    }

    @Override
    public void enterAppSetting(AppData data) {
        AppSettingActivity.enterAppSetting(mActivity, data.getPackageName(), data.getUserId());
    }

    @Override
    public int getAppCount() {
        return VirtualCore.get().getInstalledApps(0).size();
    }
}
