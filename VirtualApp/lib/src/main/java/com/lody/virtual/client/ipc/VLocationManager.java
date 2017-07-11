package com.lody.virtual.client.ipc;

import android.content.Context;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.server.location.GpsStatus;
import com.lody.virtual.server.location.ILocationManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final static boolean DEBUG = true;
    private final Map<Object, android.location.GpsStatus.Listener> mObjectListenerMap = new ConcurrentHashMap<>();

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
        if (DEBUG) return true;
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
        if (DEBUG) {
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
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setAltitude(0);
            //30.4770829328,114.6423339844
            location.setAccuracy(100f);
            location.setLatitude(30.477082d);
            location.setLongitude(114.642333d);

            Bundle extras = new Bundle();
            if (extras.get("satellites") == null) {
                extras.putInt("satellites", 5);
            }
            extras.putDouble("lat", location.getLatitude());
            extras.putDouble("lng", location.getLongitude());
            location.setExtras(extras);
            location.setTime(System.currentTimeMillis());
            Reflect.on(location).call("makeComplete");
            return location;
        }
        try {
            return getService().getVirtualLocation(loc, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * gps状态
     */
    public GpsStatus getGpsStatus(int userId) {
        try {
            getService().getGpsStatus(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * gps状态
     */
    public void setGpsStatus(GpsStatus gpsStatus, int userId) {
        try {
            getService().setGpsStatus(gpsStatus, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param method
     * @param userId
     * @param args
     */
    public Object replaceParams(String method, int userId, Object[] args) {
        if ("removeUpdates".equals(method)) {
        } else if ("requestLocationUpdates".equals(method)) {
            //15-16 last
            try {
                final int index;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    index = 1;
                } else {
                    index = args.length - 1;
                }
                final Object ListenerTransport = args[index];
                final LocationListener old = Reflect.on(ListenerTransport).get("mListener");
                if (old != null) {
                    Log.i("tmap", "LocationListenerProxy:ok");
                    final Location loc = getVirtualLocation(null, null, userId);
                    if (loc != null) {
                        old.onProviderEnabled(loc.getProvider());
                        old.onStatusChanged(loc.getProvider(), LocationProvider.AVAILABLE, new Bundle());
                        final Random random = new Random(System.currentTimeMillis());
                        synchronized (mObjectListenerMap) {
                            for (Map.Entry<Object, android.location.GpsStatus.Listener> e : mObjectListenerMap.entrySet()) {
                                final android.location.GpsStatus.Listener listener = e.getValue();
                                if (listener != null) {
                                    fakeGpsStatus(e.getKey());
                                    Log.i("tmap", "LocationListenerProxy:onGpsStatusChanged");
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.onGpsStatusChanged(android.location.GpsStatus.GPS_EVENT_STARTED);
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e1) {
                                                e1.printStackTrace();
                                            }
                                            int c = 10 + random.nextInt(60);
                                            int report = random.nextInt(c/3);
                                            for (int i = 0; i < c; i++) {
                                                listener.onGpsStatusChanged(android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS);
                                                try {
                                                    Thread.sleep(1000);
                                                } catch (InterruptedException e1) {
                                                    e1.printStackTrace();
                                                }
                                                if (i == report) {
                                                    listener.onGpsStatusChanged(android.location.GpsStatus.GPS_EVENT_FIRST_FIX);
                                                    Reflect.on(ListenerTransport).call("onLocationChanged", loc);
                                                }
                                            }
                                            Reflect.on(ListenerTransport).call("onLocationChanged", loc);
                                        }
                                    }).start();
                                }
                            }
                        }
                        Log.i("tmap", "LocationListenerProxy:onLocationChanged:" + loc);
//                        old.onLocationChanged(loc);
                    }
                    Reflect.on(args[index]).set("mListener", new LocationListenerProxy(old));
                } else {
                    Log.e("tmap", "ListenerTransport:" + ListenerTransport);
                }
            } catch (Exception e) {
                Log.e("tmap", "ListenerTransport", e);
            }
        } else if ("addGpsStatusListener".equals(method)) {
            Object GpsStatusListenerTransport = args[0];
            fakeGpsStatus(GpsStatusListenerTransport);
            android.location.GpsStatus.Listener old = Reflect.on(GpsStatusListenerTransport).get("mListener");
            if (old != null) {
                Log.i("tmap", "GpsStatusListenerProxy:ok");
//                Reflect.on(GpsStatusListenerTransport).set("mListener", new GpsStatusListenerProxy(GpsStatusListenerTransport, old));
            } else {
                Log.e("tmap", "ListenerTransport:" + GpsStatusListenerTransport);
            }
            synchronized (mObjectListenerMap) {
                mObjectListenerMap.put(args[0], old);
            }
            return true;
        } else if ("removeGpsStatusListener".equals(method)) {
            synchronized (mObjectListenerMap) {
                mObjectListenerMap.remove(args[0]);
            }
        }
        return null;
    }

    private class LocationListenerProxy implements LocationListener {
        LocationListener mListener;

        public LocationListenerProxy(LocationListener listener) {
            mListener = listener;
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.i("tmap", "onLocationChanged:" + location);
            if (mListener != null) {
                mListener.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i("tmap", "onStatusChanged:" + provider + ",status=" + status);
            if (mListener != null) {
                mListener.onStatusChanged(provider, status, extras);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (mListener != null) {
                if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
                    mListener.onProviderDisabled(provider);
                } else {
                    mListener.onProviderEnabled(provider);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (mListener != null) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    mListener.onProviderEnabled(provider);
                } else {
                    mListener.onProviderDisabled(provider);
                }
            }
        }
    }

    public void fakeGpsStatus(LocationManager locationManager) {
        if (locationManager == null) {
            Log.w("tmap", "locationManager==null");
            return;
        }
    }

    private LocationManager getLocation(Object GpsStatusListenerTransport) {
        if (GpsStatusListenerTransport == null) return null;
        Field[] fields = GpsStatusListenerTransport.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == LocationManager.class) {
                field.setAccessible(true);
                try {
                    return (LocationManager) field.get(GpsStatusListenerTransport);
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    public static final int SVID_SHIFT_WIDTH = 7;
    public static final int CONSTELLATION_TYPE_SHIFT_WIDTH = 3;
    /**
     * Unknown constellation type.
     */
    public static final int CONSTELLATION_UNKNOWN = 0;
    /**
     * Constellation type constant for GPS.
     */
    public static final int CONSTELLATION_GPS = 1;
    /**
     * Constellation type constant for SBAS.
     */
    public static final int CONSTELLATION_SBAS = 2;
    /**
     * Constellation type constant for Glonass.
     */
    public static final int CONSTELLATION_GLONASS = 3;
    /**
     * Constellation type constant for QZSS.
     */
    public static final int CONSTELLATION_QZSS = 4;
    /**
     * Constellation type constant for Beidou.
     */
    public static final int CONSTELLATION_BEIDOU = 5;
    /**
     * Constellation type constant for Galileo.
     */
    public static final int CONSTELLATION_GALILEO = 6;
    /**
     * @hide
     */
    public static final int CONSTELLATION_TYPE_MASK = 0xf;

    private static final int GLONASS_SVID_OFFSET = 64;
    private static final int BEIDOU_SVID_OFFSET = 200;
    private static final int SBAS_SVID_OFFSET = -87;

    /**
     * @hide
     */
    public static final int GNSS_SV_FLAGS_NONE = 0;
    /**
     * @hide
     */
    public static final int GNSS_SV_FLAGS_HAS_EPHEMERIS_DATA = (1 << 0);
    /**
     * @hide
     */
    public static final int GNSS_SV_FLAGS_HAS_ALMANAC_DATA = (1 << 1);
    /**
     * @hide
     */
    public static final int GNSS_SV_FLAGS_USED_IN_FIX = (1 << 2);

    /**
     * 24 GnssStatusListenerTransport
     *
     * @param mGpsStatusListenerTransport
     */
    public void fakeGpsStatus(Object mGpsStatusListenerTransport) {
        if (mGpsStatusListenerTransport == null) {
            return;
        }
        final int svCount = 16;
        int[] prns = new int[svCount];
        int[] svidWithFlags = new int[svCount];
        float[] snrs = new float[svCount];
        float[] elevations = new float[svCount];
        float[] azimuths = new float[svCount];
        int ephemerisMask = 0, almanacMask = 0, usedInFixMask = 0;
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < svCount; i++) {
            int prn = (1 + i);
            int prnShift = (1 << (prn - 1));
            int constellationType = CONSTELLATION_GLONASS;
            svidWithFlags[i] = (prn << SVID_SHIFT_WIDTH + GLONASS_SVID_OFFSET)
                    | (constellationType << CONSTELLATION_TYPE_SHIFT_WIDTH);
            prns[i] = prn;
            snrs[i] = 30f + random.nextInt(20) + random.nextInt(10);
            elevations[i] = random.nextInt(25) + random.nextInt(15) + 1;
            azimuths[i] = random.nextInt(90) + random.nextInt(20) + random.nextInt(20) + 5;
            if (i < 5) {
                ephemerisMask |= prnShift;
                almanacMask |= prnShift;
                usedInFixMask |= prnShift;
                svidWithFlags[i] |= GNSS_SV_FLAGS_HAS_EPHEMERIS_DATA;
                svidWithFlags[i] |= GNSS_SV_FLAGS_HAS_ALMANAC_DATA;
                svidWithFlags[i] |= GNSS_SV_FLAGS_USED_IN_FIX;
            }
        }
        //15-23 public void onSvStatusChanged(int svCount, int[] prns, float[] snrs,float[] elevations, float[] azimuths, int ephemerisMask,int almanacMask, int usedInFixMask)
        //24 public void onSvStatusChanged(int svCount, int[] prns, float[] snrs, float[] elevations, float[] azimuths)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Reflect.on(mGpsStatusListenerTransport).call("onSvStatusChanged",
                    svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
            //15-23 public void onSvStatusChanged(int svCount, int[] prns, float[] snrs,float[] elevations, float[] azimuths, int ephemerisMask,int almanacMask, int usedInFixMask)
        } else {
            Reflect.on(mGpsStatusListenerTransport).call("onSvStatusChanged",
                    svCount, svidWithFlags, snrs, elevations, azimuths);
            //24 public void onSvStatusChanged(int svCount, int[] svidWithFlags, float[] snrs, float[] elevations, float[] azimuths)
        }
    }

    private class GpsStatusListenerProxy implements android.location.GpsStatus.Listener {
        private android.location.GpsStatus.Listener mListener;
        private Object mGpsStatusListenerTransport;
        private boolean isFaking = false;

        public GpsStatusListenerProxy(Object GpsStatusListenerTransport,
                                      android.location.GpsStatus.Listener listener) {
            mListener = listener;
            mGpsStatusListenerTransport = GpsStatusListenerTransport;
        }

        @Override
        public void onGpsStatusChanged(int event) {
            if (mListener != null) {
                if (event == android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                    if (!isFaking) {
                        isFaking = true;
                        fakeGpsStatus(mGpsStatusListenerTransport);
                        isFaking = false;
                        return;
                    }
                }
                mListener.onGpsStatusChanged(event);
            }
        }
    }
}
