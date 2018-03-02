package com.lody.virtual.client.ipc;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.server.interfaces.IVSCallback;
import com.lody.virtual.server.interfaces.IVSafekeyManager;

/**
 * Created by wxudong on 18-1-23.
 */

public class VSafekeyManager {
    private static final VSafekeyManager sInstance = new VSafekeyManager();
    private IPCSingleton<IVSafekeyManager> singleton = new IPCSingleton<>(IVSafekeyManager.class);

    public static VSafekeyManager get() {
        return sInstance;
    }

    public IVSafekeyManager getService() {
        return singleton.get();
    }


    public boolean checkCardState() {
        try {
            return getService().checkCardState();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int getPinTryCount() {
        try {
            return getService().getPinTryCount();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void registerCallback(IVSCallback vsCallback) {
        try {
            getService().registerCallback(vsCallback);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterCallback() {
        try {
            getService().unregisterCallback();
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }


    public static int encryptKey(byte[] key, int keylen, byte[] seckey, int seckeylen) {
        try {
            return get().getService().encryptKey(key, keylen, seckey, seckeylen);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }


    public static int decryptKey(byte[] seckey, int seckeylen, byte[] key, int keylen) {
        try {
            return get().getService().decryptKey(seckey, seckeylen, key, keylen);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public static int getRandom(int len, byte[] random) {
        try {
            return get().getService().getRandom(len, random);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }
}