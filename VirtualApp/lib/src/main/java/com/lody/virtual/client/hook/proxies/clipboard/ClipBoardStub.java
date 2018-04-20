package com.lody.virtual.client.hook.proxies.clipboard;

import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.os.IInterface;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.ipc.VAppPermissionManager;
import com.lody.virtual.helper.compat.BuildCompat;

import java.lang.reflect.Method;

import mirror.android.content.ClipboardManager;
import mirror.android.content.ClipboardManagerOreo;

/**
 * @author Lody
 * @see ClipboardManager
 */
public class ClipBoardStub extends BinderInvocationProxy {
    private static final String TAG = ClipBoardStub.class.getSimpleName();

    public ClipBoardStub() {
        super(getInterface(), Context.CLIPBOARD_SERVICE);
    }

    private static IInterface getInterface() {
        if (BuildCompat.isOreo()) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    VirtualCore.get().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            return ClipboardManagerOreo.mService.get(cm);
        } else {
            return ClipboardManager.getService.call();
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ClipBoardMethodProxy("getPrimaryClip"));
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new ClipBoardMethodProxy("setPrimaryClip"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getPrimaryClipDescription"));
            addMethodProxy(new ClipBoardMethodProxy("hasPrimaryClip"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("addPrimaryClipChangedListener"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("removePrimaryClipChangedListener"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("hasClipboardText"));
        }
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        if (BuildCompat.isOreo()) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    VirtualCore.get().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipboardManagerOreo.mService.set(cm, getInvocationStub().getProxyInterface());
        } else {
            ClipboardManager.sService.set(getInvocationStub().getProxyInterface());
        }
    }

    private class ClipBoardMethodProxy extends ReplaceLastPkgMethodProxy {
        public ClipBoardMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String appPkg = getAppPkg();
            String methodName = getMethodName();
            Log.e(TAG, methodName + " appPkg: " + appPkg);
            VAppPermissionManager vAppPermissionManager = VAppPermissionManager.get();
            switch (methodName) {
                case "getPrimaryClip":
                    ClipData data = vAppPermissionManager.getClipData();
                    Log.e(TAG, methodName + " ClipData: " + (data == null ? "cache ClipData is null" : data.toString()));
                    return data;
                case "setPrimaryClip":
                    for (Object arg : args) {
                        if (arg == null) {
                            continue;
                        }
                        if (arg instanceof ClipData) {
                            ClipData clipData = (ClipData) arg;
                            Log.e(TAG, methodName + " cache ClipData: " + clipData.toString());
                            vAppPermissionManager.cacheClipData(clipData);
                        }
                    }
                    return null;
                case "hasPrimaryClip":
                    ClipData clipData = vAppPermissionManager.getClipData();
                    Log.e(TAG, methodName + " hasPrimaryClip: " + (clipData != null));
                    return clipData != null;
                default:
                    return super.call(who, method, args);
            }
        }
    }
}
