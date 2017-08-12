package com.lody.virtual.client.ipc;

import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.server.location.GpsStatusGenerate;
import com.lody.virtual.server.location.IGpsStatusListener;
import com.lody.virtual.server.location.ILocationListener;
import com.lody.virtual.server.location.ILocationManager;

import java.lang.reflect.Field;
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
            getService().setVirtualLocation(loc, getPackageName(), userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasVirtualLocation(int userId) {
        try {
            return getService().hasVirtualLocation(getPackageName(), userId);
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
            return getService().getVirtualLocation(loc, getPackageName(), userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     *
     * @param loc 有经度和纬度的位置
     */
    public Location makeGpsLocation(Location loc) {
        Location location = new Location(loc);
        location.setProvider(LocationManager.GPS_PROVIDER);
        //30.4770829328,114.6423339844
        if (location.getAccuracy() <= 0 || location.getAccuracy() > 10000) {
            location.setAccuracy(120f);
        }
        Bundle extras = new Bundle();
        if (extras.get("satellites") == null) {
            extras.putInt("satellites", GpsStatusGenerate.DEF_USER_FIX_COUNT);
        }
        location.setExtras(extras);
        return location;
    }

    /***
     *
     * @param lat 纬度 -90~90
     * @param lon 经度 -180~180
     * @param alt 高度
     * @param  accuracy 100-10000
     */
    public Location makeGpsLocation(double lat, double lon) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setAltitude(0);
        //30.4770829328,114.6423339844
        location.setAccuracy(120f);
        location.setLatitude(lat);
        location.setLongitude(lon);
        Bundle extras = new Bundle();
        if (extras.get("satellites") == null) {
            extras.putInt("satellites", GpsStatusGenerate.DEF_USER_FIX_COUNT);
        }
        location.setExtras(extras);
        return location;
    }

    /**
     * 停止全部定位
     */
    public void stopAllLocationRequest() {
        try {
            getService().stopAllLocationRequest();
        } catch (RemoteException e) {
        }
    }

    public String getPackageName() {
        return VClientImpl.get().getCurrentPackage();
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
                ListenerTransport listenerTransport = new ListenerTransport(ListenerTransport,
                        getPackageName(), userId);
                synchronized (mListenerTransportMap) {
                    mListenerTransportMap.put(ListenerTransport, listenerTransport);
                }
                try {
                    getService().addLocationListener(listenerTransport);
                } catch (RemoteException e) {
                    Log.e("tmap", "add", e);
                    return;
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
            if (GpsStatusListenerTransport != null) {
                GpsStatusListenerTransport gpsStatusListenerTransport = new GpsStatusListenerTransport(
                        GpsStatusListenerTransport, getPackageName(), userId);
                synchronized (mObjectListenerMap) {
                    mObjectListenerMap.put(GpsStatusListenerTransport, gpsStatusListenerTransport);
                }
                try {
                    getService().addGpsStatusListener(gpsStatusListenerTransport);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
        //LocationListener mListener
        private LocationListener mLocationListener;
        int userId;
        int pId;
        String packageName;
        private Location mLocation;

        public ListenerTransport(Object listenerTransport, String packageName, int userId) {
            mListenerTransport = listenerTransport;
            this.userId = userId;
            pId = Process.myPid();
            mLocationListener = Reflect.on(listenerTransport).opt("mListener");
            this.packageName = packageName;
        }

        @Override
        public boolean isAlive() {
            try {
                return mListenerTransport != null &&
                        (Boolean) Reflect.on(mListenerTransport)
                                .call("asBinder")
                                .call("isBinderAlive").get();
            }catch (Exception e){
                return false;
            }
//            return mHandler != null;
        }

        @Override
        public void onLocationChanged(Location location) throws RemoteException {
            mLocation = location;
            if (mLocationListener != null) {
                mLocationListener.onLocationChanged(location);
            } else if(isAlive()){
                try {
                    Reflect.on(mListenerTransport).call("onLocationChanged", location);
                } catch (Exception e) {

                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) throws RemoteException {
            if (mLocationListener != null) {
                mLocationListener.onStatusChanged(provider, status, extras);
            } else if(isAlive()) {
                Reflect.on(mListenerTransport).call("onStatusChanged", provider, status, extras);
            }
        }

        @Override
        public void onProviderEnabled(String provider) throws RemoteException {
            if (mLocationListener != null) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    mLocationListener.onProviderEnabled(provider);
                } else {
                    mLocationListener.onProviderDisabled(provider);
                }
            } else if(isAlive()) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    Reflect.on(mListenerTransport).call("onProviderEnabled", provider);
                } else {
                    Reflect.on(mListenerTransport).call("onProviderDisabled", provider);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) throws RemoteException {
            if (mLocationListener != null) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    mLocationListener.onProviderEnabled(provider);
                } else {
                    mLocationListener.onProviderDisabled(provider);
                }
            } else if(isAlive()) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    Reflect.on(mListenerTransport).call("onProviderEnabled", provider);
                } else {
                    Reflect.on(mListenerTransport).call("onProviderDisabled", provider);
                }
            }
        }

        @Override
        public int getUserId() throws RemoteException {
            return userId;
        }

        @Override
        public int getPid() throws RemoteException {
            return pId;
        }

        @Override
        public String getPackageName() {
            return packageName;
        }
    }

    private <T> T findField(Object obj, Class<?> clazz) {
        Field[] fields = obj.getClass().getDeclaredFields();
        if (fields != null) {
            for (Field field : fields) {
                if (clazz.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        return (T) field.get(obj);
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
        return null;
    }

    private class GpsStatusListenerTransport extends IGpsStatusListener.Stub {
        Object mGpsStatusListenerTransport;
        GpsStatus.Listener mListener;
        int userId;
        int pId;
        Handler mHandler;
        String packageName;

        public GpsStatusListenerTransport(Object gpsStatusListenerTransport, String packageName, int userId) {
            mGpsStatusListenerTransport = gpsStatusListenerTransport;
            this.userId = userId;
            mListener = getListener();
            pId = Process.myPid();
            this.packageName = packageName;
//            mHandler = findField(gpsStatusListenerTransport, android.os.Handler.class);
        }

        @Override
        public boolean isAlive() {
            try {
                return Reflect.on(mGpsStatusListenerTransport)
                        .call("asBinder")
                        .call("isBinderAlive").get();
//            return mHandler != null;
            }catch (Exception e){
                return false;
            }
        }

        private GpsStatus.Listener getListener() {
            Field[] fields = mGpsStatusListenerTransport.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == GpsStatus.Listener.class) {
                    field.setAccessible(true);
                    try {
                        return (GpsStatus.Listener) field.get(mGpsStatusListenerTransport);
                    } catch (IllegalAccessException e) {
                    }
                }
            }
            GpsStatus.Listener listener = Reflect.on(mGpsStatusListenerTransport).opt("mListener");
            if (listener == null && Build.VERSION.SDK_INT > 23) {
                listener = Reflect.on(mGpsStatusListenerTransport).opt("mGpsListener");
            }
            return listener;
        }

        @Override
        public void onGpsStarted() throws RemoteException {
            if (mListener != null) {
                mListener.onGpsStatusChanged(GpsStatus.GPS_EVENT_STARTED);
            } else {
                Reflect.on(mGpsStatusListenerTransport).call("onGpsStarted");
            }
        }

        @Override
        public void onGpsStopped() throws RemoteException {
            if (mListener != null) {
                mListener.onGpsStatusChanged(GpsStatus.GPS_EVENT_STOPPED);
            } else if (isAlive()) {
                Reflect.on(mGpsStatusListenerTransport).call("onGpsStopped");
            }
        }

        @Override
        public void onFirstFix(int ttff) throws RemoteException {
            if (mListener != null) {
                mListener.onGpsStatusChanged(GpsStatus.GPS_EVENT_FIRST_FIX);
            } else if (isAlive()) {
                Reflect.on(mGpsStatusListenerTransport).call("onFirstFix", ttff);
            }
        }

        @Override
        public void onSvStatusChanged(int svCount, int[] prns, float[] snrs, float[] elevations, float[] azimuths,
                                      int ephemerisMask, int almanacMask, int usedInFixMask, int[] svidWithFlags) throws RemoteException {
            //check Handler
            if (isAlive()) {
                GpsStatusGenerate.onSvStatusChanged(mGpsStatusListenerTransport,
                        svCount, prns, snrs, elevations, azimuths,
                        ephemerisMask, almanacMask, usedInFixMask, svidWithFlags);
            }
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

        @Override
        public int getPid() throws RemoteException {
            return pId;
        }

        @Override
        public String getPackageName() {
            return packageName;
        }
    }
}
