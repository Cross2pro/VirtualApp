package com.lody.virtual.client.ipc;

import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.remote.VDeviceInfo;
import com.lody.virtual.server.IDeviceInfoManager;

/**
 * @author Lody
 */

public class VDeviceManager {

    private static final VDeviceManager sInstance = new VDeviceManager();
    private IDeviceInfoManager mRemote;


    public static VDeviceManager get() {
        return sInstance;
    }


    public IDeviceInfoManager getRemote() {
        if (mRemote == null || !isAlive()) {
            synchronized (this) {
                Object remote = getRemoteInterface();
                mRemote = LocalProxyUtils.genProxy(IDeviceInfoManager.class, remote);
            }
        }
        return mRemote;
    }

    private boolean isAlive(){
        if(mRemote==null){
            return false;
        }
        if(VirtualCore.get().isMainProcess()){
            return mRemote.asBinder().pingBinder();
        }else if(VirtualCore.get().isVAppProcess()){
            return true;
        }else{
            return mRemote.asBinder().isBinderAlive();
        }
    }

    private Object getRemoteInterface() {
        final IBinder binder = ServiceManagerNative.getService(ServiceManagerNative.DEVICE);
        return IDeviceInfoManager.Stub.asInterface(binder);
    }

    public VDeviceInfo getDeviceInfo(int userId) {
        try {
            return getRemote().getDeviceInfo(userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void updateDeviceInfo(int userId, VDeviceInfo dinfo) {
        try {
            getRemote().updateDeviceInfo(userId, dinfo);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
}
