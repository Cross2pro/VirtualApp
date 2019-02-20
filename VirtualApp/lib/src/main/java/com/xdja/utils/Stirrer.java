package com.xdja.utils;

import android.content.pm.ProviderInfo;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;

public class Stirrer {
    public static void preInit() {
        {
            ProviderInfo info = VPackageManager.get().resolveContentProvider("media", 0, 0);
            if (info != null) {
                try {
                    VActivityManager.get().acquireProviderClient(0, info);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("xela", "no provider info of \"media\" found");
            }
        }
    }
}
