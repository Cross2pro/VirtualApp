package io.virtualapp;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.flurry.android.FlurryAgent;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.utils.VLog;

import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
import io.virtualapp.delegate.MySettingHandler;
import io.virtualapp.delegate.MyTaskDescDelegate;
import jonathanfinerty.once.Once;

/**
 * @author Lody
 */
public class App extends MultiDexApplication {

    private static App gApp;

    public static App getApp() {
        return gApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        VASettings.ENABLE_IO_REDIRECT = true;
        VASettings.ENABLE_INNER_SHORTCUT = false;
        //第一个用户（userid=0)的数据（IMEI)和真机一样，其他随机生成
        VASettings.KEEP_ADMIN_PHONE_INFO = true;
        //禁止va连的app显示前台通知服务
        VASettings.DISABLE_FOREGROUND_SERVICE = true;
        //日志
        VLog.OPEN_LOG = true;

        //外部app访问内部的provider，仅文件
        VASettings.PROVIDER_ONLY_FILE = true;

        //解决google登录后无法返回app
        VASettings.NEW_INTENTSENDER = true;

        //双开的app，根据用户升级，自动升级内部的app，需要监听
        //false则只有va的服务启动才去检查更新
        //如果是游戏，则建议关闭，
        VASettings.CHECK_UPDATE_NOT_COPY_APK = false;

        try {
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        gApp = this;
        super.onCreate();
        VirtualCore virtualCore = VirtualCore.get();
        //special app
        virtualCore.setSettingHandler(new MySettingHandler());
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                //宿主初始化
                Once.initialise(App.this);
                new FlurryAgent.Builder()
                        .withLogEnabled(true)
                        .withListener(() -> {
                            // nothing
                        })
                        .build(App.this, "48RJJP7ZCZZBB6KMMWW5");
            }

            @Override
            public void onVirtualProcess() {
                //listener components
                virtualCore.setComponentDelegate(new MyComponentDelegate());
                //fake task description's icon and title
                virtualCore.setTaskDescriptionDelegate(new MyTaskDescDelegate());
//                SpecialComponentList.addDisableOutsideContentProvider("");
            }

            @Override
            public void onServerProcess() {
                virtualCore.setAppRequestListener(new MyAppRequestListener(App.this));
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

}
