package com.lody.virtual.client.hook.proxies.telephony;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;

import mirror.com.android.internal.telephony.ICarrierConfigLoader;

@TargetApi(Build.VERSION_CODES.M)
public class ICarrierConfigLoaderStub extends BinderInvocationProxy {

    public ICarrierConfigLoaderStub() {
        super(ICarrierConfigLoader.Stub.asInterface, Context.CARRIER_CONFIG_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        if(!VirtualCore.get().hasPermission(android.Manifest.permission.READ_PHONE_STATE)) {
            addMethodProxy(new ResultStaticMethodProxy("getConfigForSubId", new PersistableBundle()));
        }
    }
}
