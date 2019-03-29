package com.xdja.utils;

import android.content.ContentProviderClient;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.IInterface;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;

import mirror.android.content.ContentProviderClientICS;
import mirror.android.content.ContentProviderClientJB;

public class Stirrer {
    public static void preInit() {
        {
            getConentProvider("media");
        }
    }

    public static ContentProviderClient getConentProvider(String authority) {
        ContentProviderClient contentProviderClient = null;
        ProviderInfo info = VPackageManager.get().resolveContentProvider(authority, 0, 0);
        if (info != null) {
            try {
                IInterface provider = VActivityManager.get().acquireProviderClient(0, info);
                if (provider != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        contentProviderClient = ContentProviderClientJB.ctor.newInstance(VirtualCore.get().getContext().getContentResolver(), provider, true);
                    } else {
                        contentProviderClient = ContentProviderClientICS.ctor.newInstance(VirtualCore.get().getContext().getContentResolver(), provider);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("xela", "no provider info of \"" + authority + "\" found");
        }

        return contentProviderClient;
    }
}
