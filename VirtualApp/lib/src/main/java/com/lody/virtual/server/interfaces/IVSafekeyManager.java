package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

public interface IVSafekeyManager extends IPCInterface {
    boolean checkCardState() throws RemoteException;

    int getPinTryCount() throws RemoteException;

    int encryptKey(byte[] key, int keylen, byte[] seckey, int seckeylen) throws RemoteException;

    int decryptKey(byte[] seckey, int seckeylen, byte[] key, int keylen) throws RemoteException;

    int getRandom(int len, byte[] random) throws RemoteException;

    void registerCallback(IVSCallback vsCallback) throws RemoteException;

    void unregisterCallback() throws RemoteException;

    abstract class Stub implements IVSafekeyManager {
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