package com.xdja.zs;

import android.os.RemoteException;

import com.lody.virtual.server.interfaces.IPCInterface;

/**
 * Created by zhangsong on 18-1-23.
 */

public interface IController extends IPCInterface {

    boolean isNetworkEnable(String packageName) throws RemoteException;
    boolean isGatewayEnable(String packageName) throws RemoteException;
    boolean isChangeConnect(String packageName, int port, String ip) throws RemoteException;

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
