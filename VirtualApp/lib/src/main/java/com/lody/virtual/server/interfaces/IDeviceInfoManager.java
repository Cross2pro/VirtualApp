package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

import com.lody.virtual.remote.VDeviceInfo;

/**
 * @author Lody
 */
public interface IDeviceInfoManager extends IPCInterface {

    VDeviceInfo getDeviceInfo(int userId) throws RemoteException;

    void updateDeviceInfo(int userId, VDeviceInfo info) throws RemoteException;

    abstract class Stub implements IDeviceInfoManager {
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
