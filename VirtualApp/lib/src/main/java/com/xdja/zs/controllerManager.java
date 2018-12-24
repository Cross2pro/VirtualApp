package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.ipc.LocalProxyUtils;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.xdja.zs.IController;
/**
 * Created by zhangsong on 18-1-23.
 */

public class controllerManager {
    private static final controllerManager sInstance = new controllerManager();
    public static controllerManager get() { return sInstance; }

    private IController mService;

    private Object getRemoteInterface() {
        return IController.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.CONTROLLER));
    }
    public IController getService() {

        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IController.class, binder);
            }
        }
        return mService;
    }

    public static boolean isNetworkEnable()
    {
        boolean ret = false;
        try{
            ret = controllerManager.get().getService().isNetworkEnable(VirtualRuntime.getInitialPackageName());
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }

        return ret;
    }

    public static boolean isCameraEnable()
    {
        boolean ret = false;
        try{
            ret = controllerManager.get().getService().isCameraEnable(VirtualRuntime.getInitialPackageName());
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }

        return ret;
    }

    public static boolean isGatewayEnable()
    {
        boolean ret = false;
        try{
            ret = controllerManager.get().getService().isGatewayEnable(VirtualRuntime.getInitialPackageName());
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }

        return ret;
    }

    public static boolean isChangeConnect(int port, String ip)
    {
        boolean ret = false;
        try{
            ret = controllerManager.get().getService().isChangeConnect(VirtualRuntime.getInitialPackageName(), port, ip);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }

        return ret;
    }

    public static boolean isSoundRecordEnable()
    {
        boolean ret = false;
        try{
            ret = controllerManager.get().getService().isSoundRecordEnable(VirtualRuntime.getInitialPackageName());
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }

        return ret;
    }

    public static boolean getActivitySwitch()
    {
        boolean ret = false;
        try{
            ret = controllerManager.get().getService().getActivitySwitch();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
        return ret;
    }

    public static void setActivitySwitch(boolean flag)
    {
        try{
            controllerManager.get().getService().setActivitySwitch(flag);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void appStart(String packageName) throws RemoteException {
        try{
            controllerManager.get().getService().appStart(packageName);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void appStop(String packageName) throws RemoteException {
        try{
            controllerManager.get().getService().appStop(packageName);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void appProcessStart(String packageName, String processName, int pid) throws RemoteException {
        try{
            controllerManager.get().getService().appProcessStart(packageName, processName, pid);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void appProcessStop(String packageName, String processName, int pid) throws RemoteException {
        try{
            controllerManager.get().getService().appProcessStop(packageName, processName, pid);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }
}
