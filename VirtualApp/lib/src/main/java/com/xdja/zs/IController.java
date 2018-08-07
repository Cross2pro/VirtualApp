package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.server.interfaces.IControllerServiceCallback;
import com.lody.virtual.server.interfaces.IPCInterface;

/**
 * Created by zhangsong on 18-1-23.
 */

public interface IController extends IPCInterface {

    boolean isNetworkEnable(String packageName) throws RemoteException;
    boolean isCameraEnable(String packageName) throws RemoteException;
    boolean isGatewayEnable(String packageName) throws RemoteException;
    boolean isChangeConnect(String packageName, int port, String ip) throws RemoteException;
    boolean isSoundRecordEnable(String packageName) throws RemoteException;

    boolean getActivitySwitch() throws RemoteException;
    void setActivitySwitch(boolean switchFlag) throws RemoteException;

    void registerCallback(IControllerServiceCallback csCallback) throws RemoteException;
    void unregisterCallback() throws RemoteException;

    void appStart(String packageName) throws RemoteException;
    void appStop(String packageName) throws RemoteException;
    void appProcessStart(String packageName, String processName, int pid) throws RemoteException;
    void appProcessStop(String packageName, String processName, int pid) throws RemoteException;

    abstract class Stub implements IController {
        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }
    }
}
