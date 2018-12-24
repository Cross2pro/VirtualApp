package com.xdja.zs;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.VLog;

public class VSafekeyCkmsManagerService extends IVSafekeyCkmsManager.Stub {
    private static final String TAG = "CkmsManagerService";
    private IVSKeyCallback mIvsKeyCallback = null;
    private static VSafekeyCkmsManagerService sInstance;
    private static Context mContext;

    public static void systemReady(Context context) {
        mContext = context;
        sInstance = new VSafekeyCkmsManagerService();
    }

    public static VSafekeyCkmsManagerService get() {
        return sInstance;
    }

    @Override
    public byte[] ckmsencryptKey(byte[] key, int keylen) {
        if (mIvsKeyCallback != null) {
            try {
                Log.e(TAG, "ckmsencryptKey");
                return mIvsKeyCallback.encryptKey(key, keylen);
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.e(TAG,"ckmsencryptKey 未注册CallBakc回调");
            byte[] bytes = new byte[keylen];
            for(int i = 0; i < keylen; i++) {
                bytes[i] = key[keylen - i - 1];
            }
            return bytes;

        }
    }

    @Override
    public byte[] ckmsdecryptKey(byte[] seckey, int seckeylen) {
        if (mIvsKeyCallback != null) {
            try {
                Log.e(TAG, "ckmsdecryptKey");
                return mIvsKeyCallback.decryptKey(seckey, seckeylen);
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.e(TAG,"ckmsdecryptKey 未注册CallBakc回调");
            byte[] bytes = new byte[seckeylen];
            for(int i = 0; i < seckeylen; i++) {
                bytes[i] = seckey[seckeylen - i - 1];
            }
            return bytes;
        }
    }

    @Override
    public void registerCallback(IVSKeyCallback ivsKeyCallback) {
        if (ivsKeyCallback != null) {
            mIvsKeyCallback = ivsKeyCallback;
        } else {
            VLog.e(TAG, "VSCkms vsCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterCallback() {
        VLog.e(TAG, "VSCkms unregisterCallback");
        mIvsKeyCallback = null;
    }
}
