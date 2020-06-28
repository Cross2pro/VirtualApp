package io.virtualapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.lody.virtual.client.core.AppDefaultConfig;
import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.utils.VLog;
import com.xdja.zs.VServiceKeepAliveManager;

import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
import io.virtualapp.delegate.MyTaskDescDelegate;
import io.virtualapp.home.BackHomeActivity;
import jonathanfinerty.once.Once;

/**
 * @author Lody
 */
public class App extends Application {

    private static App gApp;

    private SettingConfig mConfig = new SettingConfig() {
        @Override
        public String getHostPackageName() {
            return BuildConfig.APPLICATION_ID;
        }

        @Override
        public String get64bitEnginePackageName() {
            return null;
        }

        @Override
        public boolean isEnableIORedirect() {
            return true;
        }

        @Override
        public Intent onHandleLauncherIntent(Intent originIntent) {
            Intent intent = new Intent(VirtualCore.get().getContext(), BackHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }

        @Override
        public boolean isUseRealDataDir(String packageName) {
            return true;
        }

        @Override
        public AppLibConfig getAppLibConfig(String packageName) {
            return AppLibConfig.UseOwnLib;
        }

        @Override
        public boolean isAllowCreateShortcut() {
            return false;
        }

        @Override
        public boolean isAllowStartByReceiver(String packageName, int userId, String action) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                return VServiceKeepAliveManager.get().inKeepAliveServiceList(packageName)
                        || "com.android.providers.media".equals(packageName);//扫描铃声
            }
            return "com.example.demo2".equals(packageName) || InstallerSetting.privApps.contains(packageName);
        }

        @Override
        public void startPreviewActivity(int userId, ActivityInfo info, VirtualCore.UiCallback callBack) {
            super.startPreviewActivity(userId, info, callBack);
            //如果需要自定义，要注释super.startPreviewActivity，并且启动一个类似WindowPreviewActivity
        }

        @Override
        public boolean isForceVmSafeMode(String packageName) {
            return "com.tencent.mm".equals(packageName);
        }

        @Override
        public boolean IsServiceCanRestart(ServiceInfo serviceInfo) {
            //方案2
            return "com.xdja.swbg".equals(serviceInfo.packageName);
        }

        @Override
        public void onPreLunchApp() {
            //x进程启动
            if (VirtualCore.get().shouldLaunchApp("com.xdja.actoma")) {
                //TODO 启动安通+
            }
        }

        @Override
        public boolean isClearInvalidTask() {
            //不清理残留的任务记录（安全盒保活的情况，无法清理，反而弹出提示
            return false;
        }

        @Override
        public boolean isCanShowNotification(String packageName, boolean currentSpace) {
            //无论哪个域，都显示NFC通知栏
            return "com.android.nfc".equals(packageName);
        }

        @Override
        public void onDarkModeChange(boolean isDarkMode) {
            //加密微信处理是微信重启，安全盒应该是最上层应用重启
            String pkg = "com.tencent.mm";
            Log.e("kk-test", "change darkMode="+isDarkMode);
            boolean needStartWeixin = false;
            if(VActivityManager.get().isAppRunning("com.tencent.mm", 0, true)){
                needStartWeixin = true;
            }
            VActivityManager.get().finishAllActivities();
            if(needStartWeixin) {
                VActivityManager.get().startActivity(VirtualCore.get().getLaunchIntent(pkg, 0), 0);
            }
        }

        public void onFirstInstall(String packageName, boolean isClearData) {
            //running in server process.
            AppDefaultConfig.setDefaultData(packageName);
        }
    };

    public static App getApp() {
        return gApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //demo always is debug
        VLog.OPEN_LOG = true;
        try {
            VirtualCore.get().startup(base, mConfig);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        gApp = this;
        super.onCreate();
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.registerActivityLifecycleCallbacks(this);
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
                Once.initialise(App.this);

            }

            @Override
            public void onVirtualProcess() {
                //listener components
                virtualCore.setAppCallback(new MyComponentDelegate());
                //fake task description's icon and title
                virtualCore.setTaskDescriptionDelegate(new MyTaskDescDelegate());
                //内部安装，不调用系统的安装，而是自己处理（参考MyAppRequestListener），默认是静默安装在va里面。
                virtualCore.setAppRequestListener(new MyAppRequestListener(App.this));
            }

            @Override
            public void onServerProcess() {
//                 外部安装了下面应用，但是内部没有安装（双开），内部应用在调用下面应用的时候，会调用外面的应用，如果没用addVisibleOutsidePackage，则会相当于没有安装
//                 比如：内部微信调用QQ分享，但是内部没有QQ，如果没用addVisibleOutsidePackage，那么提示没有安装QQ，如果用了addVisibleOutsidePackage，则启动外部的QQ
//                 注：应用调用的校验越来越严，与外部的调用可能会失败，这时候就需要都安装在va内部。
//                 2018: 调用外部QQ登录将提示非正版应用

//                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqq");
//                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqqi");
//                virtualCore.addVisibleOutsidePackage("com.tencent.minihd.qq");
//                virtualCore.addVisibleOutsidePackage("com.tencent.qqlite");
//                virtualCore.addVisibleOutsidePackage("com.facebook.katana");
//                virtualCore.addVisibleOutsidePackage("com.whatsapp");
//                virtualCore.addVisibleOutsidePackage("com.tencent.mm");
//                virtualCore.addVisibleOutsidePackage("com.immomo.momo");
                virtualCore.addVisibleOutsidePackage("com.xdja.safekeyservice");
            }

        });
    }

}
