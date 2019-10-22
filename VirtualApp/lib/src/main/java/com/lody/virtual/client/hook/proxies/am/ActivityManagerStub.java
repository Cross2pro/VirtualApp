package com.lody.virtual.client.hook.proxies.am;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.MethodInvocationStub;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.hook.providers.DocumentHook;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.os.VUserHandle;

import java.lang.reflect.Method;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityManagerOreo;
import mirror.android.app.IActivityManager;
import mirror.android.os.ServiceManager;
import mirror.android.util.Singleton;

/**
 * @author Lody
 * @see IActivityManager
 * @see android.app.ActivityManager
 */
@Inject(MethodProxies.class)
public class ActivityManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {

    public ActivityManagerStub() {
        super(new MethodInvocationStub<>(ActivityManagerNative.getDefault.call()));
    }

    @Override
    public void inject() {
        if (BuildCompat.isOreo()) {
            //Android Oreo(8.X)
            Object singleton = ActivityManagerOreo.IActivityManagerSingleton.get();
            Singleton.mInstance.set(singleton, getInvocationStub().getProxyInterface());
        } else {
            if (ActivityManagerNative.gDefault.type() == IActivityManager.TYPE) {
                ActivityManagerNative.gDefault.set(getInvocationStub().getProxyInterface());
            } else if (ActivityManagerNative.gDefault.type() == Singleton.TYPE) {
                Object gDefault = ActivityManagerNative.gDefault.get();
                Singleton.mInstance.set(gDefault, getInvocationStub().getProxyInterface());
            }
        }
        BinderInvocationStub hookAMBinder = new BinderInvocationStub(getInvocationStub().getBaseInterface());
        hookAMBinder.copyMethodProxies(getInvocationStub());
        ServiceManager.sCache.get().put(Context.ACTIVITY_SERVICE, hookAMBinder);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        if (VirtualCore.get().isVAppProcess()) {
            addMethodProxy(new StaticMethodProxy("setRequestedOrientation") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    try {
                        return super.call(who, method, args);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return 0;
                }
            });
            addMethodProxy(new ResultStaticMethodProxy("registerUidObserver", 0));
            addMethodProxy(new ResultStaticMethodProxy("unregisterUidObserver", 0));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getAppStartMode"));
            addMethodProxy(new ResultStaticMethodProxy("updateConfiguration", 0));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("setAppLockedVerifying"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("reportJunkFromApp"));
            addMethodProxy(new StaticMethodProxy("activityResumed") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    IBinder token = (IBinder) args[0];
                    VActivityManager.get().onActivityResumed(token);
                    return super.call(who, method, args);
                }
            });
            addMethodProxy(new StaticMethodProxy("activityDestroyed") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    IBinder token = (IBinder) args[0];
                    VActivityManager.get().onActivityDestroy(token);
                    return super.call(who, method, args);
                }
            });
            addMethodProxy(new StaticMethodProxy("checkUriPermission") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    return PackageManager.PERMISSION_GRANTED;
                }
            });
            addMethodProxy(new StaticMethodProxy("finishActivity") {
                // add by lml@xdja.com
                @Override
                public boolean beforeCall(Object who, Method method, Object... args) {
                    // if (!VActivityManager.get().isAppPid(VBinder.getCallingPid()))
                    {
                        for (Object o:args) {
                            if (o instanceof Intent) {
                                Intent intent = (Intent)o;
                                {
                                    Uri uri = intent.getData();
                                    if (uri == null || "com.android.externalstorage.documents".equals(uri.getAuthority())) {
                                        continue;
                                    }
                                }
                                ComponentUtils.processOutsideIntent(VUserHandle.myUserId(), VirtualCore.get().is64BitEngine(), intent);
                            }
                        }
                    }

                    int intentIndex = MethodParameterUtils.getIndex(args, Intent.class);
                    if (intentIndex >= 0) {
                        Intent intent = (Intent) args[intentIndex];
                        if (intent != null && intent.getData() != null) {
                            Uri uri = intent.getData();
                            Uri newUri = DocumentHook.getOutsideUri(uri);
                            if (uri != newUri) {
                                intent.setDataAndType(newUri, intent.getType());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                                }
                            }
                        }
                    }
                    return super.beforeCall(who, method, args);
                }

                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    IBinder token = (IBinder) args[0];
                    VActivityManager.get().onFinishActivity(token);
                    if (VActivityManager.get().includeExcludeFromRecentsFlag(token)) {
                        //FINISH_TASK_WITH_ROOT_ACTIVITY
                        args[3] = 1;
                    }
                    return super.call(who, method, args);
                }

                @Override
                public boolean isEnable() {
                    return isAppProcess();
                }
            });
            addMethodProxy(new StaticMethodProxy("finishActivityAffinity") {
                @Override
                public Object call(Object who, Method method, Object... args) {
                    IBinder token = (IBinder) args[0];
                    return VActivityManager.get().finishActivityAffinity(getAppUserId(), token);
                }

                @Override
                public boolean isEnable() {
                    return isAppProcess();
                }
            });
        }
    }

    @Override
    public boolean isEnvBad() {
        return ActivityManagerNative.getDefault.call() != getInvocationStub().getProxyInterface();
    }

}
