package com.lody.virtual.client.ipc;

import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.server.location.GpsStatusGenerate;
import com.lody.virtual.server.location.IGpsStatusListener;
import com.lody.virtual.server.location.ILocationListener;
import com.lody.virtual.server.location.ILocationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see android.location.LocationManager
 * <p>
 * 实现代码多，资源回收不及时：拦截gps状态，定位请求，并且交给虚拟定位服务，虚拟服务根据一样的条件，再次向系统定位服务请求
 * LocationManager.addgpslistener
 * LocationManager.request
 * <p>
 * 实现代码少：GpsStatusListenerTransport、ListenerTransport这2个对象，hook里面的方法，修改参数，都是binder
 */
public class VLocationManager {
    private static final VLocationManager sInstance = new VLocationManager();
    private ILocationManager mRemote;
    private final Map<Object, GpsStatusListenerTransport> mObjectListenerMap = new ConcurrentHashMap<>();
    private final Map<Object, ListenerTransport> mListenerTransportMap = new ConcurrentHashMap<>();

    private VLocationManager() {
    }

    public static VLocationManager get() {
        return sInstance;
    }

    public ILocationManager getService() {
        if (mRemote == null || !mRemote.asBinder().isBinderAlive()) {
            synchronized (VLocationManager.class) {
                final IBinder pmBinder = ServiceManagerNative.getService(ServiceManagerNative.LOCATION);
                mRemote = ILocationManager.Stub.asInterface(pmBinder);
            }
        }
        return mRemote;
    }

    public void setVirtualLocation(Location loc, int userId) {
        try {
            getService().setVirtualLocation(loc, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasVirtualLocation(int userId) {
        try {
            return getService().hasVirtualLocation(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isProviderEnabled(String provider) {
        return LocationManager.GPS_PROVIDER.equals(provider);
    }

    public Location getVirtualLocation(Object LocationRequest, Location loc, int userId) {
        //// test code
        String provider = LocationManager.GPS_PROVIDER;
        if (LocationRequest != null) {
            try {
                provider = Reflect.on(LocationRequest).call("getProvider").get();
            } catch (Exception e) {
                //ignore
                Log.e("tmap", "get provider", e);
            }
        }
        if (!(LocationManager.GPS_PROVIDER.equals(provider)
                || LocationManager.NETWORK_PROVIDER.equals(provider)
                || LocationManager.PASSIVE_PROVIDER.equals(provider))) {
            return loc;
        }
        //////
        try {
            return getService().getVirtualLocation(loc, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param method
     * @param userId
     * @param args
     */
    public void replaceParams(String method, int userId, Object[] args) {
        if ("removeUpdates".equals(method)) {
            synchronized (mListenerTransportMap) {
                mListenerTransportMap.remove(args[0]);
            }
        } else if ("requestLocationUpdates".equals(method)) {
            Log.i("tmap", "requestLocationUpdates:start");
            //15-16 last
            final int index;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                index = 1;
            } else {
                index = args.length - 1;
            }
            final Object ListenerTransport = args[index];
            if (ListenerTransport == null) {
                Log.e("tmap", "ListenerTransport:null");
            } else {
                Log.i("tmap", "requestLocationUpdates:attch listener");

                ListenerTransport listenerTransport = new ListenerTransport(ListenerTransport, userId);
                synchronized (mListenerTransportMap) {
                    mListenerTransportMap.put(ListenerTransport, listenerTransport);
                }
                try {
                    getService().addLocationListener(listenerTransport);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Location loc = getVirtualLocation(null, null, userId);
                if (loc != null) {
                    Log.d("tmap", "onLocationChanged1:" + loc);
                    try {
                        listenerTransport.onLocationChanged(loc);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if ("addGpsStatusListener".equals(method) ||
                "registerGnssStatusCallback".equals(method)) {
            Object GpsStatusListenerTransport = args[0];
            GpsStatusGenerate.fakeGpsStatus(GpsStatusListenerTransport);
            GpsStatusListenerTransport gpsStatusListenerTransport = new GpsStatusListenerTransport(GpsStatusListenerTransport, userId);
            synchronized (mObjectListenerMap) {
                mObjectListenerMap.put(GpsStatusListenerTransport, gpsStatusListenerTransport);
            }
            try {
                getService().addGpsStatusListener(gpsStatusListenerTransport);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if ("removeGpsStatusListener".equals(method)
                || "unregisterGnssStatusCallback".equals(method)) {
            synchronized (mObjectListenerMap) {
                mObjectListenerMap.remove(args[0]);
            }
        }
    }

    private class ListenerTransport extends ILocationListener.Stub {
        Object mListenerTransport;
        int userId;

        public ListenerTransport(Object listenerTransport, int userId) {
            mListenerTransport = listenerTransport;
            this.userId = userId;
        }

        @Override
        public void onLocationChanged(Location location) throws RemoteException {
            Reflect.on(mListenerTransport).call("onLocationChanged", location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) throws RemoteException {
            Reflect.on(mListenerTransport).call("onStatusChanged", provider, status, extras);
        }

        @Override
        public void onProviderEnabled(String provider) throws RemoteException {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                Reflect.on(mListenerTransport).call("onProviderEnabled", provider);
            } else {
                Reflect.on(mListenerTransport).call("onProviderDisabled", provider);
            }
        }

        @Override
        public void onProviderDisabled(String provider) throws RemoteException {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                Reflect.on(mListenerTransport).call("onProviderEnabled", provider);
            } else {
                Reflect.on(mListenerTransport).call("onProviderDisabled", provider);
            }
        }

        @Override
        public int getUserId() throws RemoteException {
            return userId;
        }
    }

    private class GpsStatusListenerTransport extends IGpsStatusListener.Stub {
        Object mGpsStatusListenerTransport;
        GpsStatus.Listener mListener;
        int userId;

        public GpsStatusListenerTransport(Object gpsStatusListenerTransport, int userId) {
            mGpsStatusListenerTransport = gpsStatusListenerTransport;
            this.userId = userId;
            mListener = Reflect.on(gpsStatusListenerTransport).opt("mListener");
            if (mListener == null) {
                mListener = Reflect.on(gpsStatusListenerTransport).opt("mGpsListener");
            }
        }

        @Override
        public void onGpsStarted() throws RemoteException {
            Reflect.on(mGpsStatusListenerTransport).call("onGpsStarted");
        }

        @Override
        public void onGpsStopped() throws RemoteException {
            Reflect.on(mGpsStatusListenerTransport).call("onGpsStopped");
        }

        @Override
        public void onFirstFix(int ttff) throws RemoteException {
            Reflect.on(mGpsStatusListenerTransport).call("onFirstFix", ttff);
        }

        @Override
        public void onSvStatusChanged(int svCount, int[] prns, float[] snrs, float[] elevations, float[] azimuths,
                                      int ephemerisMask, int almanacMask, int usedInFixMask, int[] svidWithFlags) throws RemoteException {
            GpsStatusGenerate.onSvStatusChanged(mGpsStatusListenerTransport,
                    svCount, prns, snrs, elevations, azimuths,
                    ephemerisMask, almanacMask, usedInFixMask, svidWithFlags);
        }

        @Override
        public void onGpsStatusChanged() throws RemoteException {
            if (mListener != null) {
                mListener.onGpsStatusChanged(android.location.GpsStatus.GPS_EVENT_STARTED);
            }
        }

        @Override
        public int getUserId() throws RemoteException {
            return userId;
        }
    }
}
