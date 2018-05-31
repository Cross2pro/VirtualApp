package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

public interface IVSafekeyManager extends IPCInterface {
    boolean checkCardState() throws RemoteException;

    String getCardId() throws RemoteException;

    int getPinTryCount() throws RemoteException;

    byte[] encryptKey(byte[] key, int keylen) throws RemoteException;

    byte[] decryptKey(byte[] seckey, int seckeylen) throws RemoteException;

    byte[] getRandom(int len) throws RemoteException;

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