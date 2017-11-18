package com.lody.virtual.client.hook.proxies.location;

import android.location.ILocationListener;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.SkipInject;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.VirtualLocationManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.SchedulerTask;
import com.lody.virtual.remote.vloc.VLocation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import mirror.android.location.LocationRequestL;

/**
 * @author Lody
 */
@SuppressWarnings("ALL")
public class MethodProxies {

    private static Map<IBinder, UpdateLocationTask> tasks = new HashMap<>();

    private static void fixLocationRequest(LocationRequest request) {
        if (request != null) {
            if (LocationRequestL.mHideFromAppOps != null) {
                LocationRequestL.mHideFromAppOps.set(request, false);
            }
            if (LocationRequestL.mWorkSource != null) {
                LocationRequestL.mWorkSource.set(request, null);
            }
        }
    }

    @SkipInject
    static class AddGpsStatusListener extends ReplaceLastPkgMethodProxy {
        public AddGpsStatusListener() {
            super("addGpsStatusListener");
        }

        public AddGpsStatusListener(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    VLocationManager.get().addGpsStatusListener(getAppUserId(), args);
                    return true;
                }
            } else {
                if (isFakeLocationEnable()) {
                    return false;
                }
            }
            return super.call(who, method, args);
        }
    }

    private static class UpdateLocationTask extends SchedulerTask {

        private ILocationListener listener;

        public UpdateLocationTask(ILocationListener listener, long interval) {
            super(VirtualRuntime.getUIHandler(), interval);
            this.listener = listener;
        }

        @Override
        public void run() {
            final VLocation vLocation = VirtualLocationManager.get().getLocation(MethodProxy.getAppUserId(), MethodProxy.getAppPkg());
            if (vLocation != null) {
                Location location = createLocation(vLocation);
                if (listener.asBinder().isBinderAlive()) {
                    try {
                        listener.onLocationChanged(location);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @SkipInject
    static class RequestLocationUpdates extends ReplaceLastPkgMethodProxy {

        public RequestLocationUpdates() {
            super("requestLocationUpdates");
        }

        public RequestLocationUpdates(String name) {
            super(name);
        }

        @Override
        public Object call(final Object who, Method method, Object... args) throws Throwable {

            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    VLocationManager.get().requestLocationUpdates(getAppUserId(), args);
                    return 0;
                }
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    LocationRequest request = (LocationRequest) args[0];
                    fixLocationRequest(request);
                }
                if (isFakeLocationEnable()) {
                    //TODO update location interval
                    long interval;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        try {
                            interval = Reflect.on(args[0]).get("mInterval");
                        } catch (Exception e) {
                            interval = 60 * 60 * 1000;
                        }
                    } else {
                        interval = MethodParameterUtils.getFirstParam(args, Long.class);
                    }

                    int index = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN ? 1 : args.length - 1;
                    final ILocationListener listener = (ILocationListener) args[index];
                    UpdateLocationTask task = new UpdateLocationTask(listener, interval);
                    tasks.put(listener.asBinder(), task);
                    task.schedule();
                    return 0;
                }
            }
            return super.call(who, method, args);
        }
    }

    @SkipInject
    static class RemoveUpdates extends ReplaceLastPkgMethodProxy {

        public RemoveUpdates() {
            super("removeUpdates");
        }

        public RemoveUpdates(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    VLocationManager.get().removeUpdates(getAppUserId(), args);
                    return 0;
                }
            } else {
                if (isFakeLocationEnable()) {
                    IBinder binder = (IBinder) args[0];
                    UpdateLocationTask task = tasks.get(binder);
                    if (task != null) {
                        task.cancel();
                    }
                    return 0;
                }
            }
            return super.call(who, method, args);
        }
    }

    @SkipInject
    static class GetLastLocation extends ReplaceLastPkgMethodProxy {

        public GetLastLocation() {
            super("getLastLocation");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (!(args[0] instanceof String)) {
                LocationRequest request = (LocationRequest) args[0];
                fixLocationRequest(request);
            }
            if (isFakeLocationEnable()) {
                VLocation loc = VirtualLocationManager.get().getLocation(getAppUserId(), getAppPkg());
                if (loc != null) {
                    return createLocation(loc);
                } else {
                    return null;
                }
            } else {
                if (isFakeLocationEnable()) {
                    LocationRequest request = (LocationRequest) args[0];
                    fixLocationRequest(request);
                    VLocation loc = VirtualLocationManager.get().getLocation(getAppUserId(), getAppPkg());
                    if (loc != null) {
                        return createLocation(loc);
                    } else {
                        return null;
                    }
                }
            }
            return super.call(who, method, args);
        }
    }

    @SkipInject
    static class GetLastKnownLocation extends GetLastLocation {
        @Override
        public String getMethodName() {
            return "getLastKnownLocation";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                Location old = (Location) super.call(who, method, args);
                return VLocationManager.get().getVirtualLocation(args[0], old, getAppUserId());
            }
            return super.call(who, method, args);
        }
    }

    @SkipInject
    static class IsProviderEnabled extends MethodProxy {
        @Override
        public String getMethodName() {
            return "isProviderEnabled";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    if (args[0] instanceof String) {
                        return VLocationManager.get().isProviderEnabled((String) args[0]);
                    }
                }
            } else {
                if (isFakeLocationEnable()) {
                    String provider = (String) args[0];
                    if (LocationManager.GPS_PROVIDER.equals(provider)) {
                        return true;
                    }
                }
            }
            return super.call(who, method, args);
        }
    }

    private static class getAllProviders extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getAllProviders";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (isFakeLocationEnable()) {
                return Arrays.asList(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER);
            }
            return super.call(who, method, args);
        }
    }

    @SkipInject
    static class GetBestProvider extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getBestProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    return LocationManager.GPS_PROVIDER;
                }
            } else {
                if (isFakeLocationEnable()) {
                    return LocationManager.NETWORK_PROVIDER;
                }
            }
            return super.call(who, method, args);
        }
    }


    public static Location createLocation(VLocation loc) {
        Location sysLoc = new Location(LocationManager.GPS_PROVIDER);
        if (loc.accuracy == 0f) {
            sysLoc.setAccuracy(8f);
        } else {
            sysLoc.setAccuracy(loc.accuracy);
        }
        Bundle extraBundle = new Bundle();
        extraBundle.putInt("satellites", 23);
        sysLoc.setBearing(loc.bearing);
        Reflect.on(sysLoc).call("setIsFromMockProvider", false);
        sysLoc.setLatitude(loc.latitude);
        sysLoc.setLongitude(loc.longitude);
        sysLoc.setSpeed(loc.speed);
        sysLoc.setTime(System.currentTimeMillis());
        sysLoc.setExtras(extraBundle);
        Reflect.on(sysLoc).call("setElapsedRealtimeNanos", SystemClock.elapsedRealtime());
        return sysLoc;
    }

    @SkipInject
    static class RemoveGpsStatusListener extends ReplaceLastPkgMethodProxy {
        public RemoveGpsStatusListener() {
            super("removeGpsStatusListener");
        }

        public RemoveGpsStatusListener(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                VLocationManager.get().removeGpsStatusListener(getAppUserId(), args);
                return 0;
            }
            return super.call(who, method, args);
        }
    }

    @SkipInject
    static class RequestLocationUpdatesPI extends RequestLocationUpdates {
        public RequestLocationUpdatesPI() {
            super("requestLocationUpdatesPI");
        }
    }

    @SkipInject
    static class RemoveUpdatesPI extends RemoveUpdates {
        public RemoveUpdatesPI() {
            super("removeUpdatesPI");
        }
    }

    @SkipInject
    static class UnregisterGnssStatusCallback extends RemoveGpsStatusListener {
        public UnregisterGnssStatusCallback() {
            super("unregisterGnssStatusCallback");
        }
    }

    @SkipInject
    static class RegisterGnssStatusCallback extends AddGpsStatusListener {
        public RegisterGnssStatusCallback() {
            super("registerGnssStatusCallback");
        }
    }


    static class sendExtraCommand extends MethodProxy {

        @Override
        public String getMethodName() {
            return "sendExtraCommand";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    return true;
                }
            } else if (isFakeLocationEnable()) {
                return true;
            }
            return super.call(who, method, args);
        }
    }

    static class getProviderProperties extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getProviderProperties";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    try {
                        Reflect.on(result).set("mRequiresNetwork", false);
                        Reflect.on(result).set("mRequiresCell", false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return result;
                }
            } else if (isFakeLocationEnable()) {
                try {
                    Reflect.on(result).set("mRequiresNetwork", false);
                    Reflect.on(result).set("mRequiresCell", false);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return result;
            }
            return super.afterCall(who, method, args, result);
        }
    }

    static class locationCallbackFinished extends MethodProxy {

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.VIRTUAL_LOCATION) {
                if (VLocationManager.get().hasVirtualLocation(getAppUserId())) {
                    return true;
                }
            } else if (isFakeLocationEnable()) {
                return true;
            }
            return super.call(who, method, args);
        }

        @Override
        public String getMethodName() {
            return "locationCallbackFinished";
        }
    }
}
