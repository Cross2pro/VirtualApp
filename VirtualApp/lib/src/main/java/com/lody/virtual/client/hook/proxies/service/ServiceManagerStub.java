package com.lody.virtual.client.hook.proxies.service;

import android.content.Context;
import android.os.IInterface;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.MethodInvocationStub;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.notification.NotificationManagerStub;
import com.lody.virtual.client.hook.proxies.phonesubinfo.PhoneSubInfoStub;
import com.lody.virtual.client.hook.proxies.telephony.TelephonyStub;

import java.lang.reflect.Method;

import mirror.android.os.ServiceManager;

public class ServiceManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {

    public ServiceManagerStub() {
        super(new MethodInvocationStub<>(ServiceManager.getIServiceManager.call()));
    }

    @Override
    public void inject() throws Throwable {
        ServiceManager.sServiceManager.set(getInvocationStub().getProxyInterface());
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("getService") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                String name = (String) args[0];
                if (name.equals(Context.TELEPHONY_SERVICE)) {
                    MethodInvocationStub<IInterface> stub = InvocationStubManager.getInstance().getInvocationStub(TelephonyStub.class);
                    if (stub != null) {
                        return stub.getProxyInterface().asBinder();
                    }
                }
                if (name.equals("iphonesubinfo")) {
                    MethodInvocationStub<IInterface> stub = InvocationStubManager.getInstance().getInvocationStub(PhoneSubInfoStub.class);
                    if (stub != null) {
                        return stub.getProxyInterface().asBinder();
                    }
                }
                if (name.equals(Context.APP_OPS_SERVICE)) {
                    MethodInvocationStub<IInterface> stub = InvocationStubManager.getInstance().getInvocationStub(AppOpsManagerStub.class);
                    if (stub != null) {
                        return stub.getProxyInterface().asBinder();
                    }
                }
                if (name.equals(Context.NOTIFICATION_SERVICE)) {
                    MethodInvocationStub<IInterface> stub = InvocationStubManager.getInstance().getInvocationStub(NotificationManagerStub.class);
                    if (stub != null) {
                        return stub.getProxyInterface().asBinder();
                    }
                }
                return super.call(who, method, args);
            }
        });
    }

    @Override
    public boolean isEnvBad() {
        return ServiceManager.sServiceManager.get() != getInvocationStub().getProxyInterface();
    }
}