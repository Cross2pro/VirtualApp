package com.lody.virtual.client.hook.providers;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IInterface;
import android.provider.DocumentsContract;
import android.util.Log;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import mirror.android.content.IContentProvider;

public class DocumentHook implements InvocationHandler {

    private static DocumentHook mInstance;
    private static Object mBase;
    private static final String TAG = DocumentHook.class.getSimpleName();

    public DocumentHook(Object base) {
        this.mBase = base;
    }

    public static IInterface createProxy(IInterface provider) {
        if (provider instanceof Proxy && Proxy.getInvocationHandler(provider) instanceof DocumentHook) {
            return provider;
        }

        mInstance = new DocumentHook(provider);

        return (IInterface) Proxy.newProxyInstance(provider.getClass().getClassLoader(), new Class[]{
                IContentProvider.TYPE,
        }, mInstance);
    }

    protected void processArgs(Method method, Object... args) {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String pkg = (String) args[0];
            if (VirtualCore.get().isAppInstalled(pkg)) {
                args[0] = VirtualCore.get().getHostPkg();
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            processArgs(method, args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        String name = method.getName();
        int start = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? 1 : 0;
        if (name.equals("query") || name.equals("openTypedAssetFile") || name.equals("openAssetFile")) {
            Uri uri = (Uri)args[start];
            List<String> paths = uri.getPathSegments();
            String auth = uri.getAuthority();
            String  scm = uri.getScheme();
            Uri.Builder newUri = new Uri.Builder().authority(auth).scheme(scm);
            for (String path:paths) {
                if (path.startsWith("secondary")) {
                    path = path.substring("secondary".length());
                }
                newUri.appendPath(path);
            }
            args[start] = newUri.build();
            Log.d(TAG, "document uri:" + args[start]);
        } else if (name.equals("call")) {
            String methodName = (String)args[start];
            Log.d(TAG, "call:methodName:" + methodName);
            if (methodName.equals("android:createDocument")
                    || methodName.equals("android:renameDocument")
                    || methodName.equals("android:deleteDocument")) {
                Bundle extras = (Bundle)args[start+2];
                Uri rootUri = extras.getParcelable("uri");
                List<String> paths = rootUri.getPathSegments();
                String auth = rootUri.getAuthority();
                String  scm = rootUri.getScheme();
                Uri.Builder newUri = new Uri.Builder().authority(auth).scheme(scm);
                for (String path:paths) {
                    if (path.startsWith("secondary")) {
                        path = path.substring("secondary".length());
                    }
                    newUri.appendPath(path);
                }
                extras.putParcelable("uri", newUri.build());
                args[start+2] = extras;
            }
        }
        return method.invoke(mBase, args);
    }

}
