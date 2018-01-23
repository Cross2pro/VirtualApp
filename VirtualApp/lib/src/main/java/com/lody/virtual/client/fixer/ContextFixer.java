package com.lody.virtual.client.fixer;

import android.content.Context;
import android.content.ContextWrapper;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.DropBoxManager;
import android.util.Log;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
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
        String hostPkg = VirtualCore.get().getHostPkg();
        ContextImpl.mBasePackageName.set(context, hostPkg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContextImplKitkat.mOpPackageName.set(context, hostPkg);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ContentResolverJBMR2.mPackageName.set(context.getContentResolver(), hostPkg);
        }
        //第一次的gps状态伪装
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        VLocationManager.get().setLocationManager(locationManager);

        if (context.getApplicationInfo().targetSdkVersion < 10) {
            try {
                Class<?> cAsyncTask = context.getClassLoader().loadClass(AsyncTask.class.getName());
                Reflect ref = Reflect.on(cAsyncTask);
                //AsyncTask.THREAD_POOL_EXECUTOR
                Executor THREAD_POOL_EXECUTOR = ref.get("THREAD_POOL_EXECUTOR");
                ref.call("setDefaultExecutor", THREAD_POOL_EXECUTOR);
            }catch (Throwable e){
                Log.w(TAG, "setDefaultExecutor",e);
            }
        }
    }

}
