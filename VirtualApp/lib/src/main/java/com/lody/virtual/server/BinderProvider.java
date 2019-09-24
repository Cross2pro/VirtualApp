package com.lody.virtual.server;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.KeepAliveService;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.compat.NotificationChannelCompat;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.accounts.VAccountManagerService;
import com.lody.virtual.server.am.BroadcastSystem;
import com.lody.virtual.server.am.VActivityManagerService;
import com.lody.virtual.server.content.VContentService;
import com.lody.virtual.server.device.VDeviceManagerService;
import com.lody.virtual.server.interfaces.IServiceFetcher;
import com.lody.virtual.server.job.VJobSchedulerService;
import com.lody.virtual.server.location.VirtualLocationService;
import com.lody.virtual.server.notification.VNotificationManagerService;
import com.lody.virtual.server.pm.VAppManagerService;
import com.lody.virtual.server.pm.VPackageManagerService;
import com.lody.virtual.server.pm.VUserManagerService;
import com.lody.virtual.server.vs.VirtualStorageService;

import com.xdja.activitycounter.ActivityCounterService;
import com.xdja.call.PhoneCallService;
import com.xdja.zs.VSafekeyCkmsManagerService;
import com.xdja.zs.VSafekeyManagerService;
import com.xdja.zs.VServiceKeepAliveManager;
import com.xdja.zs.VServiceKeepAliveService;
import com.xdja.zs.VWaterMarkService;
import com.xdja.zs.controllerService;

import com.xdja.zs.VAppPermissionManagerService;

/**
 * @author Lody
 */
public final class BinderProvider extends ContentProvider {

    private final ServiceFetcher mServiceFetcher = new ServiceFetcher();
    private static boolean sInitialized = false;

    @Override
    public boolean onCreate() {
        return init();
    }

    public static boolean isInitialized() {
        return sInitialized;
    }

    private boolean init() {
        if (sInitialized) {
            return false;
        }
        Context context = getContext();
        if (context != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.DAEMON_ID, "daemon");
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.DEFAULT_ID, "default");
                NotificationChannelCompat.checkOrCreateChannel(context, NotificationChannelCompat.LIGHT_ID, "light");
            }
            try {
                context.startService(new Intent(context, KeepAliveService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!VirtualCore.get().isStartup()) {
            return false;
        }
        VPackageManagerService.systemReady();
        addService(ServiceManagerNative.PACKAGE, VPackageManagerService.get());
        addService(ServiceManagerNative.ACTIVITY, VActivityManagerService.get());
        addService(ServiceManagerNative.USER, VUserManagerService.get());
        VServiceKeepAliveService.systemReady();
        addService(ServiceManagerNative.KEEPALIVE, VServiceKeepAliveService.get());
        VAppManagerService.systemReady();
        addService(ServiceManagerNative.APP, VAppManagerService.get());
        BroadcastSystem.attach(VActivityManagerService.get(), VAppManagerService.get());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addService(ServiceManagerNative.JOB, VJobSchedulerService.get());
        }
        VNotificationManagerService.systemReady(context);
        addService(ServiceManagerNative.NOTIFICATION, VNotificationManagerService.get());
        VAppManagerService.get().scanApps();
        VAccountManagerService.systemReady();
        VContentService.systemReady();
        addService(ServiceManagerNative.ACCOUNT, VAccountManagerService.get());
        addService(ServiceManagerNative.CONTENT, VContentService.get());
        addService(ServiceManagerNative.VS, VirtualStorageService.get());
        addService(ServiceManagerNative.DEVICE, VDeviceManagerService.get());
        addService(ServiceManagerNative.VIRTUAL_LOC, VirtualLocationService.get());

        /* Start Changed by XDJA */
        VSafekeyManagerService.systemReady(context);
        addService(ServiceManagerNative.SAFEKEY, VSafekeyManagerService.get());
        addService(ServiceManagerNative.CONTROLLER, controllerService.get());
        VAppPermissionManagerService.systemReady();
        addService(ServiceManagerNative.APPPERMISSION, VAppPermissionManagerService.get());
        VWaterMarkService.systemReady();
        addService(ServiceManagerNative.WATERMARK, VWaterMarkService.get());
//        VSafekeyCkmsManagerService.systemReady(context);
//        addService(ServiceManagerNative.CKMSSAFEKEY, VSafekeyCkmsManagerService.get());
        /* End Changed by XDJA */
        addService(ServiceManagerNative.FLOATICONBALL, ActivityCounterService.get());
        sInitialized = true;

        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d("wxd", " start preLaunchApp");
                    VirtualCore.get().preLaunchApp();
                    Log.d("wxd", " end preLaunchApp");
                    sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        return true;
    }

    private void addService(String name, IBinder service) {
        ServiceCache.addService(name, service);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!sInitialized) {
            init();
        }
        if ("@".equals(method)) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, "_VA_|_binder_", mServiceFetcher);
            return bundle;
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private class ServiceFetcher extends IServiceFetcher.Stub {
        @Override
        public IBinder getService(String name) throws RemoteException {
            if (name != null) {
                return ServiceCache.getService(name);
            }
            return null;
        }

        @Override
        public void addService(String name, IBinder service) throws RemoteException {
            if (name != null && service != null) {
                ServiceCache.addService(name, service);
            }
        }

        @Override
        public void removeService(String name) throws RemoteException {
            if (name != null) {
                ServiceCache.removeService(name);
            }
        }
    }
}
