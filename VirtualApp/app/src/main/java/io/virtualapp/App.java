package io.virtualapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import com.lody.virtual.client.core.SettingRule;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.utils.VLog;

import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
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
        //google 支持（beta）
        VASettings.ENABLE_GMS = true;
        //禁止va连的app显示前台通知服务
        VASettings.DISABLE_FOREGROUND_SERVICE = true;
        //日志
        VLog.OPEN_LOG = BuildConfig.DEBUG;

        //外部app访问内部的provider，仅文件
        VASettings.PROVIDER_ONLY_FILE = true;

        //解决google登录后无法返回app
        VASettings.NEW_INTENTSENDER = true;

        //双开的app，根据用户升级，自动升级内部的app，需要监听
        //false则只有va的服务启动才去检查更新,需要用户监听升级广播和启动得时候检查
        VASettings.CHECK_UPDATE_NOT_COPY_APK = true;

        //内部文件权限
        VASettings.FILE_ISOLATION = false;

        try {
            //
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
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                //宿主初始化
                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
                Once.initialise(App.this);
                //某些rom做了限制，比如vivo
                IntentFilter filter = new IntentFilter(Constants.ACTION_PROCESS_ERROR);
                filter.addAction(Constants.ACTION_NEED_PERMISSION);
                filter.addAction(Constants.ACTION_PROCESS_ERROR);
                App.this.registerReceiver(new VAppReceiver(), filter);
            }

            @Override
            public void onVirtualProcess() {
                //listener components
                virtualCore.setAppCallback(new MyComponentDelegate());
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
                /**
                 * 下面代码可以在启动后添加，需要杀死目标app让设置生效，建议用killAllApps
                 * @see VirtualCore#killAllApps()
                 * @see VirtualCore#killApp(String, int)
                 */
                virtualCore.addSettingRule(SettingRule.UseRealDataDir, "com.tencent.tmgp.pubgmhd");
                virtualCore.addSettingRule(SettingRule.DisableDlOpen, "com.facebook.katana");
                virtualCore.addSettingRule(SettingRule.DisableDlOpen, "jianghu2.lanjing.com*");
                virtualCore.addSettingRule(SettingRule.UseOutsideLibraryFiles, "com*.fgo.*");
                virtualCore.addSettingRule(SettingRule.UseOutsideLibraryFiles, "com*.fatego*");
                virtualCore.addSettingRule(SettingRule.UseOutsideLibraryFiles, "com.izhaohe.heroes*");
                virtualCore.addSettingRule(SettingRule.UseOutsideLibraryFiles, "com.tencent.tmgp.pubgmhd*");
                virtualCore.addSettingRule(SettingRule.UseOutsideLibraryFiles, "com.*.dwrg*");

                //test code
                virtualCore.addSettingRule(SettingRule.UseRealDataDir, "com.kk.vatest2");
                //other regex
                virtualCore.addSettingRuleRegex(SettingRule.UseOutsideLibraryFiles, "com.kk.demo[|.360|.huawei]");
            }
        });
    }

    private class VAppReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Constants.ACTION_NEED_PERMISSION.equals(intent.getAction())){
                String season = intent.getStringExtra(Constants.EXTRA_SEASON);
                //
                String error = intent.getStringExtra(Constants.EXTRA_ERROR);
                if("startActivityForBg".equals(season)){
                    //TODO vivo start activity by service
                    //跳到vivo的后台弹activity权限
                }
            }else if(Constants.ACTION_PROCESS_ERROR.equals(intent.getAction())){
                String season = intent.getStringExtra(Constants.EXTRA_SEASON);
                //
                String error = intent.getStringExtra(Constants.EXTRA_ERROR);
                if ("makeApplication".equals(season)) {

                }else if("callApplicationOnCreate".equals(season)){

                }
            }
        }
    };
}
