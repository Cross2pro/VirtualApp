package com.lody.virtual.client.fixer;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.DropBoxManager;
import android.util.Log;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.proxies.alarm.AlarmManagerStub;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
import com.lody.virtual.client.hook.proxies.wifi.WifiManagerStub;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.ReflectException;

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
        DropBoxManager dm = (DropBoxManager) context.getSystemService(Context.DROPBOX_SERVICE);
        BinderInvocationStub boxBinder = InvocationStubManager.getInstance().getInvocationStub(DropBoxManagerStub.class);
        if (boxBinder != null) {
            try {
                Reflect.on(dm).set("mService", boxBinder.getProxyInterface());
            } catch (ReflectException e) {
                e.printStackTrace();
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            BinderInvocationStub appopsBinder = InvocationStubManager.getInstance().getInvocationStub(AppOpsManagerStub.class);
            if (appopsBinder != null) {
                try {
                    Reflect.on(appOpsManager).set("mService", appopsBinder.getProxyInterface());
                } catch (ReflectException e) {
                    e.printStackTrace();
                }
            }
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        BinderInvocationStub alarmBinder = InvocationStubManager.getInstance().getInvocationStub(AlarmManagerStub.class);
        if (alarmBinder != null) {
            try {
                Reflect.on(alarmManager).set("mService", alarmBinder.getProxyInterface());
            } catch (ReflectException e) {
                //ignore
            }
        }
        try {
            Reflect.on(alarmManager).set("mTargetSdkVersion", context.getApplicationInfo().targetSdkVersion);
        }catch (Throwable e){
            //ignore
        }
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        BinderInvocationStub wifiBinder = InvocationStubManager.getInstance().getInvocationStub(WifiManagerStub.class);
        if (wifiBinder != null) {
            try {
                Reflect.on(wifiManager).set("mService", wifiBinder.getProxyInterface());
            } catch (ReflectException e) {
                //ignore
            }
        }
        String hostPkg = VirtualCore.get().getHostPkg();
        ContextImpl.mBasePackageName.set(context, hostPkg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContextImplKitkat.mOpPackageName.set(context, hostPkg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ContentResolverJBMR2.mPackageName.set(context.getContentResolver(), hostPkg);
        }
        if(!fixed) {
            final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            VLocationManager.get().setLocationManager(locationManager);
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
        }
        fixed = true;
    }

    private static boolean fixed = false;
}
