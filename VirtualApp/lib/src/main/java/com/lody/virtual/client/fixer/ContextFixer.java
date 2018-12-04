package com.lody.virtual.client.fixer;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.IInterface;
import android.os.health.SystemHealthManager;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.proxies.alarm.AlarmManagerStub;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.battery_stats.BatteryStatsHub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
import com.lody.virtual.client.hook.proxies.wifi.WifiManagerStub;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.helper.compat.StrictModeCompat;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.util.concurrent.Executor;

import mirror.android.app.ContextImpl;
import mirror.android.app.ContextImplKitkat;
import mirror.android.content.ContentResolverJBMR2;

/**
 * @author Lody
 */
public class ContextFixer {

    private static final String TAG = ContextFixer.class.getSimpleName();

    /**
     * Fuck AppOps
     *
     * @param context Context
     */
    public static void fixContext(Context context) {
        try {
            context.getPackageName();
        } catch (Throwable e) {
            return;
        }
        InvocationStubManager.getInstance().checkEnv(GraphicsStatsStub.class);
        int deep = 0;
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
            deep++;
            if (deep >= 10) {
                return;
            }
        }
        ContextImpl.mPackageManager.set(context, null);
        try {
            context.getPackageManager();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (!VirtualCore.get().isVAppProcess()) {
            return;
        }

        //fix member binder
        fixBinders(context);

        String hostPkg = VirtualCore.get().getHostPkg();
        ContextImpl.mBasePackageName.set(context, hostPkg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContextImplKitkat.mOpPackageName.set(context, hostPkg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ContentResolverJBMR2.mPackageName.set(context.getContentResolver(), hostPkg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (VirtualCore.get().getTargetSdkVersion() >= Build.VERSION_CODES.N
                    && VClient.get().getCurrentTargetSdkVersion() < Build.VERSION_CODES.N) {
                //fix file
                StrictModeCompat.disableDeathOnFileUriExposure();
            }
        }
        if (context.getApplicationInfo().targetSdkVersion < 10) {
            try {
                Class<?> cAsyncTask = context.getClassLoader().loadClass(AsyncTask.class.getName());
                Reflect ref = Reflect.on(cAsyncTask);
                //AsyncTask.THREAD_POOL_EXECUTOR
                Executor THREAD_POOL_EXECUTOR = ref.get("THREAD_POOL_EXECUTOR");
                ref.call("setDefaultExecutor", THREAD_POOL_EXECUTOR);
            } catch (Throwable e) {
                Log.w(TAG, "setDefaultExecutor", e);
            }
        }
        //fake gps's location
        VLocationManager.get().setLocationManager(context);
    }

    private static <T extends IInjector> IInterface getIInterface(Class<T> injectorClass) {
        BinderInvocationStub injector = InvocationStubManager.getInstance().getInvocationStub(injectorClass);
        return injector == null ? null : injector.getProxyInterface();
    }

    private static boolean CONTEXT_BINDER_FIXED = false;

    private static void fixBinders(Context context) {
        if (CONTEXT_BINDER_FIXED) return;
        CONTEXT_BINDER_FIXED = true;
        final String TAG = "fixBinders";
        //dropbox
        IInterface binder = getIInterface(DropBoxManagerStub.class);
        if (binder != null) {
            if (mirror.android.os.DropBoxManager.mService != null) {
                DropBoxManager dm = (DropBoxManager) context.getSystemService(Context.DROPBOX_SERVICE);
                try {
                    mirror.android.os.DropBoxManager.mService.set(dm, binder);
//                    VLog.i(TAG, "DropBoxManager:mService:ok");
                } catch (Exception e) {
                    VLog.w(TAG, "DropBoxManager:mService:%s", Log.getStackTraceString(e));
                }
            }
        }
        //appops
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            binder = getIInterface(AppOpsManagerStub.class);
            if (binder != null) {
                if (mirror.android.app.AppOpsManager.mService != null) {
                    AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                    try {
                        mirror.android.app.AppOpsManager.mService.set(appOpsManager, binder);
//                        VLog.i(TAG, "AppOpsManager:mService:ok");
                    } catch (Exception e) {
                        VLog.w(TAG, "AppOpsManager:mService:%s", Log.getStackTraceString(e));
                    }
                }
            }
        }
        //alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        binder = getIInterface(AlarmManagerStub.class);
        if (binder != null) {
            if (mirror.android.app.AlarmManager.mService != null) {
                try {
                    mirror.android.app.AlarmManager.mService.set(alarmManager, binder);
//                    VLog.i(TAG, "AlarmManager:mService:ok");
                } catch (Exception e) {
                    VLog.w(TAG, "AlarmManager:mService:%s", Log.getStackTraceString(e));
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mirror.android.app.AlarmManager.mTargetSdkVersion != null) {
                try {
                    mirror.android.app.AlarmManager.mTargetSdkVersion.set(alarmManager, context.getApplicationInfo().targetSdkVersion);
                } catch (Exception e) {
                    VLog.w(TAG, "AlarmManager:mTargetSdkVersion:%s", Log.getStackTraceString(e));
                }
            }
        }
        //wifi
        binder = getIInterface(WifiManagerStub.class);
        if (binder != null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (mirror.android.net.wifi.WifiManager.mService != null) {
                //other
                try {
                    mirror.android.net.wifi.WifiManager.mService.set(wifiManager, binder);
//                    VLog.i(TAG, "WifiManager:mService:ok");
                } catch (Exception e) {
                    VLog.w(TAG, "WifiManager:mService:%s", Log.getStackTraceString(e));
                }
            } else if (mirror.android.net.wifi.WifiManager.sService != null) {
                //sumsung
                try {
                    mirror.android.net.wifi.WifiManager.sService.set(binder);
//                    VLog.i(TAG, "WifiManager:sService:ok");
                } catch (Exception e) {
                    VLog.w(TAG, "WifiManager:sService:%s", Log.getStackTraceString(e));
                }
            }
        }
        //BatteryStats
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (mirror.android.os.health.SystemHealthManager.mBatteryStats != null) {
                binder = getIInterface(BatteryStatsHub.class);
                if (binder != null) {
                    SystemHealthManager manager = (SystemHealthManager) context.getSystemService(Context.SYSTEM_HEALTH_SERVICE);
                    try {
                        mirror.android.os.health.SystemHealthManager.mBatteryStats.set(manager, binder);
                    } catch (Throwable e) {
                        VLog.w(TAG, "SystemHealthManager:mBatteryStats:%s", Log.getStackTraceString(e));
                    }
                }
            }
        }
    }
}
