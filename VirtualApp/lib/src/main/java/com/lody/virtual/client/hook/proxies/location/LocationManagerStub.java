package com.lody.virtual.client.hook.proxies.location;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VLocationManager;

import java.lang.reflect.Method;
import java.util.Arrays;

import mirror.android.location.ILocationManager;
import mirror.android.location.LocationRequestL;

/**
 * @author Lody
 * @see android.location.LocationManager
 */
public class LocationManagerStub extends BinderInvocationProxy {
    public LocationManagerStub() {
        super(ILocationManager.Stub.asInterface, Context.LOCATION_SERVICE);
    }

    private static class BaseMethodProxy extends ReplaceLastPkgMethodProxy {

        public BaseMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args.length > 0) {
                Object request = args[0];
                if (LocationRequestL.mHideFromAppOps != null) {
                    LocationRequestL.mHideFromAppOps.set(request, false);
                }
                if (LocationRequestL.mWorkSource != null) {
                    LocationRequestL.mWorkSource.set(request, null);
                }
            }
            return super.call(who, method, args);
        }
    }

    private static class ReplaceLastPkgMethodProxy2 extends ReplaceLastPkgMethodProxy {
        ReplaceLastPkgMethodProxy2(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                Object rs = VLocationManager.get().replaceParams(getMethodName(), getAppUserId(), args);
                if (rs != null) {
                    return rs;
                }
            }
            return super.call(who, method, args);
        }
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
            addMethodProxy(new ReplaceLastPkgMethodProxy("addGpsMeasurementsListener"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("addGpsNavigationMessageListener"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            addMethodProxy(new ReplaceLastPkgMethodProxy2("addGpsStatusListener"));
            addMethodProxy(new ReplaceLastPkgMethodProxy2("removeGpsStatusListener"));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addMethodProxy(new ReplaceLastPkgMethodProxy2("registerGnssStatusCallback"));
            addMethodProxy(new ReplaceLastPkgMethodProxy2("unregisterGnssStatusCallback"));
        }
        addMethodProxy(new StaticMethodProxy("isProviderEnabled") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    if (args[0] instanceof String) {
                        return VLocationManager.get().isProviderEnabled((String) args[0]);
                    }else{
                        Log.w("tmap","args="+ Arrays.toString(args));
                    }
                }
                return super.call(who, method, args);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            addMethodProxy(new BaseMethodProxy("requestLocationUpdates") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                        Object rx = VLocationManager.get().replaceParams(getMethodName(), getAppUserId(), args);
                        if (rx != null) {
                            return rx;
                        }
                    }
                    return super.call(who, method, args);
                }
            });
            addMethodProxy(new ReplaceLastPkgMethodProxy2("removeUpdates"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("requestGeofence"){
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                        return 0;
                    }
                    return super.call(who, method, args);
                }
            });
            addMethodProxy(new ReplaceLastPkgMethodProxy("removeGeofence"){
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                        return 0;
                    }
                    return super.call(who, method, args);
                }
            });
            addMethodProxy(new BaseMethodProxy("getLastLocation") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                        Location old = (Location) super.call(who, method, args);
                        return VLocationManager.get().getVirtualLocation(args[0], old, getAppUserId());
                    }
                    return super.call(who, method, args);
                }
            });
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN
                && TextUtils.equals(Build.VERSION.RELEASE, "4.1.2")) {
            addMethodProxy(new ReplaceLastPkgMethodProxy2("requestLocationUpdates"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("requestLocationUpdatesPI"));
            addMethodProxy(new ReplaceLastPkgMethodProxy2("removeUpdates"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("removeUpdatesPI"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("addProximityAlert"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getLastKnownLocation") {
                @Override
                public Object call(Object who, Method method, Object... args) throws Throwable {
                    if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                        Location old = (Location) super.call(who, method, args);
                        return VLocationManager.get().getVirtualLocation(args[0], old, getAppUserId());
                    }
                    return super.call(who, method, args);
                }
            });
        }
    }
}
