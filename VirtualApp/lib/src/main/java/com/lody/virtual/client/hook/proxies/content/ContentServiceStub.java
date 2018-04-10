package com.lody.virtual.client.hook.proxies.content;

import android.content.pm.ProviderInfo;
import android.net.Uri;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.os.VUserHandle;

import java.lang.reflect.Method;

import mirror.android.content.IContentService;

/**
 * @author Lody
 * @see IContentService
 */

public class ContentServiceStub extends BinderInvocationProxy {
    private static final String tAG = ContentServiceStub.class.getSimpleName();

    public ContentServiceStub() {
        super(IContentService.Stub.asInterface, "content");
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceUriMethodProxy("registerContentObserver"));
        addMethodProxy(new ReplaceUriMethodProxy("notifyChange"));
    }

    private static class ReplaceUriMethodProxy extends StaticMethodProxy {
        int index = -1;

        ReplaceUriMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (index < 0) {
                index = MethodParameterUtils.getIndex(args, Uri.class);
            }
            if (index >= 0) {
                args[index] = fakeUri((Uri) args[index]);
            }
            return super.call(who, method, args);
        }

        private Uri fakeUri(Uri uri) {
            String name = uri.getAuthority();
            final int userId = VUserHandle.myUserId();
            ProviderInfo info = VPackageManager.get().resolveContentProvider(name, 0, userId);
            if (info != null && info.enabled && isAppPkg(info.packageName)) {
                int targetVPid = VActivityManager.get().initProcess(info.packageName, info.processName, userId);
                if (targetVPid != -1) {
                    String targetAuthority = VASettings.getStubAuthority(targetVPid);
                    return uri.buildUpon().authority(targetAuthority).build();
                }
            }
            return uri;
        }
    }
}
