package io.virtualapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatDelegate;

import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
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
            return false;
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
                return VServiceKeepAliveManager.get().inKeepAliveServiceList(packageName);
            } else if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action)) {
                return "com.android.providers.media".equals(packageName);
            }
            return false;
        }

        @Override
        public void startPreviewActivity(int userId, ActivityInfo info, VirtualCore.UiCallback callBack){
            super.startPreviewActivity(userId, info, callBack);
            //如果需要自定义，要注释super.startPreviewActivity，并且启动一个类似WindowPreviewActivity
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
            }
        });
    }

}
