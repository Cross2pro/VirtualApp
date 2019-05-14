package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;

public class VServiceKeepAliveManager {

    public static final int ACTION_DEL = 1;
    public static final int ACTION_ADD = 2;

    private static final VServiceKeepAliveManager sInstance = new VServiceKeepAliveManager();
    IServiceKeepAlive mService;

    private Object getRemoteInterface() {
        return IServiceKeepAlive.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.KEEPALIVE));
    }

    public IServiceKeepAlive getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IServiceKeepAlive.class, binder);
            }
        }
        return mService;
    }

    public static VServiceKeepAliveManager get() {
        return sInstance;
    }

    public void addKeepAliveServiceName(String pkgName, String serviceName) {
        try {
            get().getService().addKeepAliveServiceName(pkgName, serviceName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void removeKeepAliveServiceName(String name) {
        try {
            get().getService().removeKeepAliveServiceName(name);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void scheduleUpdateKeepAliveList(String pkgName, int action) {
        try {
            get().getService().scheduleUpdateKeepAliveList(pkgName, action);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void scheduleRunKeepAliveService(String pkgName, int userId) {
        try {
            get().getService().scheduleRunKeepAliveService(pkgName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
