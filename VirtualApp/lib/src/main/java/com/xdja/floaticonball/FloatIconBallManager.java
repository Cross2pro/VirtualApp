package com.xdja.floaticonball;

import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;

/**
 * @Date 18-11-28 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class FloatIconBallManager {
    private static final FloatIconBallManager sInstance = new FloatIconBallManager();
    public static FloatIconBallManager get() { return sInstance; }

    private IFloatIconBallService mRemote;
    public IFloatIconBallService getRemote() {
        if (mRemote == null ||
                (!mRemote.asBinder().isBinderAlive() && !VirtualCore.get().isVAppProcess())) {
            synchronized (FloatIconBallManager.class) {
                Object remote = getStubInterface();
                mRemote = LocalProxyUtils.genProxy(IFloatIconBallService.class, remote);
            }
        }
        return mRemote;
    }
    private Object getStubInterface() {
        return IFloatIconBallService.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.FLOATICONBALL));
    }
    public void activityCountAdd(String pkg){
        try {
            getRemote().activityCountAdd(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
    public void activityCountReduce(String pkg){
        try {
            getRemote().activityCountReduce(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
    public boolean isForeGroundApp(String pkg){
        try {
            return getRemote().isForeGroundApp(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
        return false;
    }
    public void registerCallback(IFloatIconBallCallback fibCallback) {
        try {
            getRemote().registerCallback(fibCallback);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterCallback() {
        try {
            getRemote().unregisterCallback();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
}
