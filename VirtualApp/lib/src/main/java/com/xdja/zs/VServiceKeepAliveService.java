package com.xdja.zs;


import android.content.Intent;
import android.os.RemoteException;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.VLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VServiceKeepAliveService extends IServiceKeepAlive.Stub {

    private static final String TAG = "VServiceKeepAliveService";
    private static VServiceKeepAliveService sInstance;
    private static Map<String, String> mKeepAliveServiceList = new HashMap<>();

    static {
        mKeepAliveServiceList.put("com.xdja.emm.InitService", "com.xdja.emm");
    }

    public static void systemReady() {
        sInstance = new VServiceKeepAliveService();

    }

    public static VServiceKeepAliveService get() {
        return sInstance;
    }


    @Override
    public void addKeepAliveServiceName(String pkgName, String serviceName) throws RemoteException {
        if (!mKeepAliveServiceList.containsKey(serviceName)) {
            mKeepAliveServiceList.put(serviceName, pkgName);
        }
    }

    @Override
    public void removeKeepAliveServiceName(String name) throws RemoteException {
        if (mKeepAliveServiceList.containsKey(name)) {
            mKeepAliveServiceList.remove(name);
        }
    }

    private boolean hasKeepAliveService(String pkgName) {
        Iterator entries = mKeepAliveServiceList.entrySet().iterator();
        if (entries.hasNext()) {
            do {
                Map.Entry entry = (Map.Entry) entries.next();
                if (entry.getValue().equals(pkgName)) {
                    return true;
                }
            } while (entries.hasNext());
        }
        return false;
    }

    @Override
    public void runKeepAliveService(String pkgName, int userId) {
        Iterator entries = mKeepAliveServiceList.entrySet().iterator();
        if (entries.hasNext()) {
            do {
                Map.Entry entry = (Map.Entry) entries.next();
                if (entry.getValue().equals(pkgName)) {
                    Intent intent = new Intent();
                    intent.setClassName(pkgName, (String) entry.getKey());
                    VLog.d(TAG, "service:" + entry.getKey());
                    VActivityManager.get().startService(userId, intent);
                }
            } while (entries.hasNext());
        }
    }
}
