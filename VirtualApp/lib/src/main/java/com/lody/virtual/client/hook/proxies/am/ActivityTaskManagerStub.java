package com.lody.virtual.client.hook.proxies.am;

import android.annotation.TargetApi;
import android.os.IBinder;

import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.annotations.LogInvocation;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VActivityManager;

import java.lang.reflect.Method;

import mirror.android.app.IActivityTaskManager;

//
// Created by Swift Gan on 2019/3/18.
//

//Android Q Task Manager
@LogInvocation
@TargetApi(29)
public class ActivityTaskManagerStub extends BinderInvocationProxy {

    public ActivityTaskManagerStub() {
        super(IActivityTaskManager.Stub.asInterface, "activity_task");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new MethodProxies.StartActivity());
        addMethodProxy(new MethodProxies.GetCallingActivity());
        addMethodProxy(new MethodProxies.StartActivities());
        addMethodProxy(new MethodProxies.StartActivityAndWait());
        addMethodProxy(new MethodProxies.StartActivityAsUser());
        addMethodProxy(new MethodProxies.SetTaskDescription());
        addMethodProxy(new MethodProxies.StartActivityWithConfig());
        addMethodProxy(new MethodProxies.StartActivityIntentSender());
        addMethodProxy(new MethodProxies.GetTasks());
        addMethodProxy(new MethodProxies.GetCallingPackage());
        addMethodProxy(new MethodProxies.OverridePendingTransition());
        addMethodProxy(new MethodProxies.GetActivityClassForToken());
        addMethodProxy(new MethodProxies.StartNextMatchingActivity());
        addMethodProxy(new MethodProxies.StartActivityAsCaller());
        addMethodProxy(new MethodProxies.StartVoiceActivity());

        addMethodProxy(new StaticMethodProxy("activityDestroyed") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                IBinder token = (IBinder) args[0];
                VActivityManager.get().onActivityDestroy(token);
                return super.call(who, method, args);
            }
        });
        addMethodProxy(new StaticMethodProxy("activityResumed") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                IBinder token = (IBinder) args[0];
                VActivityManager.get().onActivityResumed(token);
                return super.call(who, method, args);
            }
        });
        addMethodProxy(new StaticMethodProxy("finishActivity") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                IBinder token = (IBinder) args[0];
                VActivityManager.get().onFinishActivity(token);
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
