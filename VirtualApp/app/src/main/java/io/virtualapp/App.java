package io.virtualapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatDelegate;

import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.helper.utils.VLog;

import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
import io.virtualapp.delegate.MyTaskDescDelegate;
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
            return BuildConfig.ARM64_PACKAGE_NAME;
        }

        @Override
        public boolean isUseRealDataDir(String packageName) {
            //使用真实data目录
            return "com.tencent.tmgp.pubgmhd".equals(packageName);
        }

        @Override
        public AppLibConfig getAppLibConfig(String packageName) {
            if("com.bilibili.fgo.qihoo".equals(packageName)){
                return AppLibConfig.UseRealLib;
            }
            return super.getAppLibConfig(packageName);
        }

        @Override
        public boolean isDisableNotCopyApk(String pacakgeName) {
            return super.isDisableNotCopyApk(pacakgeName);
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
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                //宿主初始化
                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
                Once.initialise(App.this);
                //某些rom做了限制，比如vivo
                IntentFilter filter = new IntentFilter();
                filter.addAction(Constants.ACTION_NEED_PERMISSION);
                App.this.registerReceiver(new VAppReceiver(), filter);
            }

            @Override
            public void onVirtualProcess() {
//                new ANRWatchDog().start();
                //listener components
                virtualCore.setAppCallback(new MyComponentDelegate());
                //fake task description's icon and title
                virtualCore.setTaskDescriptionDelegate(new MyTaskDescDelegate());
//                SpecialComponentList.addDisableOutsideContentProvider("");
            }

            @Override
            public void onServerProcess() {
                //内部安装，不调用系统的安装，而是自己处理（参考MyAppRequestListener），默认是静默安装在va里面。
                virtualCore.setAppRequestListener(new MyAppRequestListener(App.this));
                //外部安装了下面应用，但是内部没有安装（双开），内部应用在调用下面应用的时候，会调用外面的应用，如果没用addVisibleOutsidePackage，则会相当于没有安装
                // 比如：内部微信调用QQ分享，但是内部没有QQ，如果没用addVisibleOutsidePackage，那么提示没有安装QQ，如果用了addVisibleOutsidePackage，则启动外部的QQ
                // 注：应用调用的校验越来越严，与外部的调用可能会失败，这时候就需要都安装在va内部。
                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqq");
                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqqi");
                virtualCore.addVisibleOutsidePackage("com.tencent.minihd.qq");
                virtualCore.addVisibleOutsidePackage("com.tencent.qqlite");
                virtualCore.addVisibleOutsidePackage("com.facebook.katana");
                virtualCore.addVisibleOutsidePackage("com.whatsapp");
                virtualCore.addVisibleOutsidePackage("com.tencent.mm");
                virtualCore.addVisibleOutsidePackage("com.immomo.momo");
            }
        });
    }

    private class VAppReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String season = intent.getStringExtra(Constants.EXTRA_SEASON);
            //
            String error = intent.getStringExtra(Constants.EXTRA_ERROR);
            if (Constants.ACTION_NEED_PERMISSION.equals(intent.getAction())) {
                if ("startActivityForBg".equals(season)) {
                    //TODO vivo start activity by service
                    //跳到vivo的后台弹activity权限
                }
            }else if(Constants.ACTION_PROCESS_ERROR.equals(intent.getAction())){
                if("requestPermissions".equals(season)){
                    //user cancel
                }
            }
        }
    }
}
