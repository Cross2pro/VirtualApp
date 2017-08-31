package com.lody.virtual.client.hook.proxies.location;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.Inject;
import com.lody.virtual.client.hook.base.LogInvocation;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.client.stub.VASettings;

import java.lang.reflect.Method;

import mirror.android.location.ILocationManager;

/**
 * @author Lody
 * @see android.location.LocationManager
 */
@LogInvocation(LogInvocation.Condition.ALWAYS)
@Inject(MethodProxies.class)
public class LocationManagerStub extends BinderInvocationProxy {
    public LocationManagerStub() {
        super(ILocationManager.Stub.asInterface, Context.LOCATION_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addMethodProxy(new ReplaceLastPkgMethodProxy("addTestProvider"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("removeTestProvider"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("setTestProviderLocation"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("clearTestProviderLocation"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("setTestProviderEnabled"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("clearTestProviderEnabled"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("setTestProviderStatus"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("clearTestProviderStatus"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addGpsMeasurementListener", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addGpsNavigationMessageListener", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGpsMeasurementListener", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGpsNavigationMessageListener", true));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("requestGeofence", 0));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGeofence", 0));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addGpsStatusListener", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeGpsStatusListener", 0));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("registerGnssStatusCallback", true));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("unregisterGnssStatusCallback", 0));
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN
                && TextUtils.equals(Build.VERSION.RELEASE, "4.1.2")) {
            addMethodProxy(new ReplaceLastPkgMethodProxy("requestLocationUpdatesPI"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("removeUpdatesPI"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("addProximityAlert"));
        }
        addMethodProxy(new MethodProxies.IsProviderEnabled());
        addMethodProxy(new MethodProxies.GetLastKnownLocation());
        addMethodProxy(new MethodProxies.GetBestProvider());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new MethodProxies.RequestLocationUpdates());
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeUpdates", 0));
            addMethodProxy(new MethodProxies.GetLastLocation());
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("addNmeaListener", 0));
            addMethodProxy(new FakeReplaceLastPkgMethodProxy("removeNmeaListener", 0));
        }
    }

    private static class FakeReplaceLastPkgMethodProxy extends ReplaceLastPkgMethodProxy {
        private Object mDefValue;

        private FakeReplaceLastPkgMethodProxy(String name, Object def) {
            super(name);
            mDefValue = def;
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    return mDefValue;
                }
            }
            return super.call(who, method, args);
        }
    }

}
