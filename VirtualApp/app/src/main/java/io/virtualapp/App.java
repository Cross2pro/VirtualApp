package io.virtualapp;

import android.app.Application;
import android.app.IWallpaperManagerCallback;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.AppDefaultConfig;
import com.lody.virtual.client.core.SettingConfig;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.helper.utils.VLog;
import com.xdja.zs.BoxProvider;
import com.xdja.zs.VServiceKeepAliveManager;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
import io.virtualapp.delegate.MyTaskDescDelegate;
import io.virtualapp.home.BackHomeActivity;
import jonathanfinerty.once.Once;
import mirror.android.app.LoadedApk;
import mirror.android.content.res.CompatibilityInfo;

import static android.os.ParcelFileDescriptor.*;

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
        public String getPluginEnginePackageName() {
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
            if(originIntent != null && originIntent.getExtras() != null) {
                intent.putExtras(originIntent.getExtras());
            }
            return intent;
        }

        @Override
        public boolean isUseRealDataDir(String packageName) {
            return false;
        }

        @Override
        public AppLibConfig getAppLibConfig(String packageName) {
            return AppLibConfig.UseRealLib;
        }

        @Override
        public boolean isAllowCreateShortcut() {
            return false;
        }

        @Override
        public boolean isAllowStartByReceiver(String packageName, int userId, String action) {
            if(!BoxProvider.isCurrentSpace()){
                //非工作域，都禁止自动启动
                return false;
            }
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

        @Override
        public void onFirstInstall(String packageName, boolean isClearData) {
            //running in server process.
            AppDefaultConfig.setDefaultData(packageName);
        }

        @Override
        public boolean onHandleView(Intent intent, String packageName, int userId) {
            if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getType() != null) {
                if (intent.getType().startsWith("image/")) {
                    if (VirtualCore.get().isAppInstalled(InstallerSetting.GALLERY_PKG)) {
                        intent.setPackage(InstallerSetting.GALLERY_PKG);
                        return false;
                    }
                } else if (intent.getType().startsWith("video/")) {
                    if (VirtualCore.get().isAppInstalled(InstallerSetting.VIDEO_PLAYER_PKG)) {
                        intent.setPackage(InstallerSetting.VIDEO_PLAYER_PKG);
                        return false;
                    }
                }
            }
            return super.onHandleView(intent, packageName, userId);
        }

        @Override
        public Intent getChooserIntent(Intent orgIntent, IBinder resultTo, String resultWho, int requestCode, Bundle options, int userId) {
            //上层可以重写ChooserActivity
            return super.getChooserIntent(orgIntent, resultTo, resultWho, requestCode, options, userId);
        }

        @Override
        public boolean isClearInvalidProcess() {
            //盒内上次退出，部分进程会重启
            return true;
        }

        @Override
        public boolean isFloatOnLockScreen(String className) {
            //锁屏界面需要显示的
            return "com.tencent.av.ui.VideoInviteActivity".equals(className) || super.isFloatOnLockScreen(className);
        }

        @Override
        public int getWallpaperHeightHint(String packageName, int userId) {
            //指定壁纸的高度
            return Resources.getSystem().getDisplayMetrics().heightPixels;
        }

        @Override
        public int getWallpaperWidthHint(String packageName, int userId) {
            //指定壁纸的宽度
            return Resources.getSystem().getDisplayMetrics().widthPixels;
        }

        /**
         *
         * @return 是否打断调用
         */
        @Override
        public WallpaperResult onSetWallpaper(String packageName, int userId, String name, Rect cropHint, int which, IWallpaperManagerCallback callback) {
            //null,则由系统处理外部桌面响应
            //WallpaperResult#wallpaperFile为null，则无法设置桌面
            //WallpaperResult#wallpaperFile为自己创建的文件，由当前app取写入
            File file = new File(Environment.getExternalStorageDirectory(), ".wallpaper/"+System.currentTimeMillis()+".png");
            File dir = file.getParentFile();
            WallpaperResult result = new WallpaperResult();
            try {
                if(!dir.exists()){
                    dir.mkdirs();
                }
                if(file.exists()){
                    file.delete();
                }
                String realPath = NativeEngine.getRedirectedPath(dir.getAbsolutePath());
                result.wallpaperFile = ParcelFileDescriptor.open(file,
                        MODE_CREATE | MODE_READ_WRITE | MODE_TRUNCATE//);
                , VUiKit.getUiHandler(), e -> {
                    //系统源码是用FileObserver，如果目标app不ParcelFileDescriptor#close，可能不会响应
                            try {
                                callback.onWallpaperChanged();
                                VirtualCore.get().getContext().sendBroadcast(new Intent(Constants.ACTION_WALLPAPER_CHANGED)
                                        .putExtra(Intent.EXTRA_STREAM, new File(realPath, file.getName()).getPath()));
                            } catch (Throwable ex) {
                                //ignore
                            }
                        });
//                //文件写完后，需要回调
//                Log.e("MethodInvocationStub", "IWallpaperManager:listen:" + realPath);
//                //FileObserver需要hook，注册真实路径
//                new FileObserver(realPath) {
//                    @Override
//                    public void onEvent(int event, @Nullable String path) {
//                        if (event == FileObserver.CLOSE_WRITE) {
//                            if(file.getName().equals(path)){
//                                try {
//                                    callback.onWallpaperChanged();
//                                } catch (Throwable e) {
//                                }
//                                VirtualCore.get().getContext().sendBroadcast(new Intent(Constants.ACTION_WALLPAPER_CHANGED)
//                                        .putExtra(Intent.EXTRA_STREAM, new File(realPath, path).getPath()));
//                            }
//                        }
//                    }
//                }.startWatching();
                return result;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean isNeedRealRequestInstall(String packageName) {
            //如果需要给该应用未知来源安装真实的判断，返回true
            return super.isNeedRealRequestInstall(packageName);
        }

        @Override
        public boolean isHideForegroundNotification() {
            //如果需要自定义通知栏
            //切换到工作域调用VirtualCore.get().startForeground(); 显示前台通知
            //切换生活域调用VirtualCore.get().stopForeground(); 隐藏前台通知
            //true=取消自带前台通知
            return true;
        }

        @Override
        public Notification getForegroundNotification() {
            //TODO 这里可以自定义前台通知样式
            return super.getForegroundNotification();
        }

        @Override
        public boolean isAllowServiceStartForeground(String packageName) {
            if(!BoxProvider.isCurrentSpace()){
                //非工作域，都禁止自动启动
                return false;
            }
            //可以禁用某个应用的前台通知
            return super.isAllowServiceStartForeground(packageName);
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
