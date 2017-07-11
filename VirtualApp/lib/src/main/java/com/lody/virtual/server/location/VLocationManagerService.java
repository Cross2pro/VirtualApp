package com.lody.virtual.server.location;


import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class VLocationManagerService extends ILocationManager.Stub {
    private final SparseArray<Location> mLocations = new SparseArray<>();
    private final SparseArray<GpsStatus> mGpsStatuss = new SparseArray<>();
    private Context mContext;
    private static final AtomicReference<VLocationManagerService> gService = new AtomicReference<>();
    private final Map<Object, GpsListener> mGpsStatusListenerList = new ConcurrentHashMap<>();
    private final Map<Object, ILocationListener> mLocationListenerList = new ConcurrentHashMap<>();
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    public static void systemReady(Context context) {
        VLocationManagerService instance = new VLocationManagerService(context);
        gService.set(instance);
    }

    public static VLocationManagerService get() {
        return gService.get();
    }

    private VLocationManagerService(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void setVirtualLocation(Location loc, int userId) {
        synchronized (mLocations) {
            mLocations.put(userId, loc);
        }
    }

    @Override
    public boolean hasVirtualLocation(int userId) {
        synchronized (mLocations) {
            return mLocations.get(userId) != null;
        }
    }

    @Override
    public Location getVirtualLocation(Location loc, int userId) {
        synchronized (mLocations) {
            return mLocations.get(userId);
        }
    }

    @Override
    public GpsStatus getGpsStatus(int userId) {
        synchronized (mGpsStatuss) {
            return mGpsStatuss.get(userId);
        }
    }

    @Override
    public void setGpsStatus(GpsStatus gpsStatus, int userId) {
        synchronized (mGpsStatuss) {
            mGpsStatuss.put(userId, gpsStatus);
        }
    }

    @Override
    public void addGpsStatusListener(IGpsStatusListener listener) {
        GpsListener proxy = new GpsListener(listener);
        synchronized (mGpsStatusListenerList) {
            mGpsStatusListenerList.put(listener, proxy);
            //TODO 如果是系统的监听，则复制gps状态
        }
        mLocationManager.addGpsStatusListener(proxy);
    }

    @Override
    public void removeGpsStatusListener(IGpsStatusListener listener) {
        GpsListener proxy;
        synchronized (mGpsStatusListenerList) {
            proxy = mGpsStatusListenerList.remove(listener);
        }
        if (proxy != null) {
            mLocationManager.removeGpsStatusListener(proxy);
        }
    }

    //LocationRequest, ListenerTransport
    @Override
    public void addLocationListener(ILocationListener listener) {
//        LocationListenerProxy proxy = new LocationListenerProxy(listener);
//        synchronized (mLocationListenerList) {
//            mLocationListenerList.add(listener);
//        }
        //LocationRequest
    }

    @Override
    public void removeLocationListener(ILocationListener listener) {
        synchronized (mLocationListenerList) {
            mLocationListenerList.remove(listener);
        }
    }

    private class GpsListener implements android.location.GpsStatus.Listener {
        //GpsStatusListenerTransport
        private Object mObject;

        private GpsListener(Object old) {
            mObject = old;
        }

        @Override
        public void onGpsStatusChanged(int event) {

        }
    }

    private class LocationListenerProxy implements LocationListener {
        //ListenerTransport
        private Object mObject;

        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
