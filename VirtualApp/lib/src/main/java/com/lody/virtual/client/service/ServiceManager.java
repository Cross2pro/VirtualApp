package com.lody.virtual.client.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.IBinder;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.os.VUserHandle;

import java.util.Map;

/**
 * @author Lody
 */
public class ServiceManager {

    private static final ServiceManager sInstance = new ServiceManager();
    private final Map<ComponentName, ServiceRecord> mServices = new ArrayMap<>();


    private ServiceManager() {
    }

    public static ServiceManager get() {
        return sInstance;
    }

    private ServiceRecord getServiceRecord(ComponentName component) {
        return mServices.get(component);
    }

    private ServiceRecord getOrCreateService(ComponentName componentName, ServiceInfo serviceInfo) {
        ServiceRecord service = getServiceRecord(componentName);
        return service == null ? handleCreateService(serviceInfo) : service;
    }

    private ServiceRecord handleCreateService(ServiceInfo serviceInfo) {
        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.service = VClient.get().createService(serviceInfo, serviceRecord);
        mServices.put(ComponentUtils.toComponentName(serviceInfo), serviceRecord);
        return serviceRecord;
    }

    public int onStartCommand(Intent proxyIntent, int flags) {
        if (proxyIntent == null) {
            return Service.START_NOT_STICKY;
        }
        ServiceInfo serviceInfo = proxyIntent.getParcelableExtra("_VA_|_service_info_");
        Intent intent = proxyIntent.getParcelableExtra("_VA_|_intent_");
        int startId = proxyIntent.getIntExtra("_VA_|_start_id_", -1);
        if (serviceInfo == null || intent == null || startId == -1) {
            return Service.START_NOT_STICKY;
        }
        ComponentName component = ComponentUtils.toComponentName(serviceInfo);
        ServiceRecord record;
        try {
            record = getOrCreateService(component, serviceInfo);
        } catch (Throwable e) {
            throw new RuntimeException("startService fail: " + intent, e);
        }
        if (record == null || record.service == null) {
            return Service.START_NOT_STICKY;
        }
        intent.setExtrasClassLoader(record.service.getClassLoader());
        boolean restartRedeliverIntent = proxyIntent.getBooleanExtra("EXTRA_RESTART_REDELIVER_INTENT", true);
        if (!restartRedeliverIntent) {
            intent = null;
        }
        int startResult = record.service.onStartCommand(intent, flags, startId);
        if (startResult == Service.START_STICKY
                || startResult == Service.START_REDELIVER_INTENT) {
            restartRedeliverIntent = startResult == Service.START_REDELIVER_INTENT;
            proxyIntent.putExtra("EXTRA_RESTART_REDELIVER_INTENT", restartRedeliverIntent);
            VActivityManager.get().onServiceStartCommand(VUserHandle.myUserId(), startId, serviceInfo, intent);
        }
        return startResult;
    }

    public void onDestroy() {
        if (this.mServices.size() > 0) {
            for (ServiceRecord record : mServices.values()) {
                Service service = record.service;
                if (service != null) {
                    service.onDestroy();
                }
            }
        }
        this.mServices.clear();
    }

    public IBinder onBind(Intent proxyIntent) {
        Intent intent = proxyIntent.getParcelableExtra("_VA_|_intent_");
        ServiceInfo serviceInfo = proxyIntent.getParcelableExtra("_VA_|_service_info_");

        if (intent == null || serviceInfo == null) {
            return null;
        }
        ComponentName component = ComponentUtils.toComponentName(serviceInfo);
        ServiceRecord record;
        try {
            record = getOrCreateService(component, serviceInfo);
        } catch (Throwable e) {
            throw new RuntimeException("bindService fail: " + intent, e);
        }
        if (record == null) {
            return null;
        }
        intent.setExtrasClassLoader(record.service.getClassLoader());
        record.increaseConnectionCount(intent);
        if (record.hasBinder(intent)) {
            IBinder iBinder = record.getBinder(intent);
            if (record.shouldRebind(intent)) {
                record.service.onRebind(intent);
            }
            return iBinder;
        }
        IBinder binder = record.service.onBind(intent);
        record.setBinder(intent, binder);
        return binder;
    }

    public void onUnbind(Intent proxyIntent) {
        Intent intent = proxyIntent.getParcelableExtra("_VA_|_intent_");
        ServiceInfo serviceInfo = proxyIntent.getParcelableExtra("_VA_|_service_info_");
        if (intent == null || serviceInfo == null) {
            return;
        }
        ComponentName component = ComponentUtils.toComponentName(serviceInfo);
        ServiceRecord record = getServiceRecord(component);
        if (record == null) {
            return;
        }
        int res = VActivityManager.get().onServiceUnBind(VUserHandle.myUserId(), component);
        boolean destroy = res == 0;
        if (destroy || record.decreaseConnectionCount(intent)) {
            boolean rebind = record.service.onUnbind(intent);
            if (destroy) {
                record.service.onDestroy();
                mServices.remove(component);
                VActivityManager.get().onServiceDestroyed(VUserHandle.myUserId(), component);
                return;
            }
            record.setShouldRebind(intent, rebind);
        }
    }

    public void onLowMemory() {
        for (ServiceRecord record : mServices.values()) {
            Service service = record.service;
            if (service != null) {
                try {
                    service.onLowMemory();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        for (ServiceRecord record : mServices.values()) {
            Service service = record.service;
            if (service != null) {
                try {
                    service.onConfigurationChanged(newConfig);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onTrimMemory(int level) {
        for (ServiceRecord record : mServices.values()) {
            Service service = record.service;
            if (service != null) {
                try {
                    service.onTrimMemory(level);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doStopServiceOnMainThread(ComponentName component) {
        ServiceRecord r = mServices.get(component);
        if (r != null) {
            r.service.onDestroy();
            mServices.remove(component);
        }
    }

    public void stopService(final ComponentName component) {
        VirtualRuntime.getUIHandler().post(new Runnable() {
            public void run() {
                doStopServiceOnMainThread(component);
            }
        });

    }

}
