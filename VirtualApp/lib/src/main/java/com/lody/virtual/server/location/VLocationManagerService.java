package com.lody.virtual.server.location;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VLocationManager;
import com.lody.virtual.helper.utils.Reflect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class VLocationManagerService extends ILocationManager.Stub {
    private final SparseArray<Location> mLocations = new SparseArray<>();
    private Context mContext;
    private static final AtomicReference<VLocationManagerService> gService = new AtomicReference<>();
    private final List<IGpsStatusListener> mGpsStatusListeners = new ArrayList<>();
    private final List<ILocationListener> mLocationListeners = new ArrayList<>();
    private final static boolean DEBUG = false;
    //屏幕关闭不发送位置信息
    private final static boolean CONFIG_SCREEN = true;
    //每次定位的发送信息有效时间
    private final static boolean CONFIG_LOCATION_TIME = true;

    private final static int MSG_HANDLE_LOCATION = 1;
    private long mLastGPS, mLastLocation;
    private long mHandleStartTime;
    private static final boolean REPORT_LOCATION_WITH_GPS_STATUS = false;
    final Random mRandom = new Random(System.currentTimeMillis());

    private final static int HANDLE_TIME_GPS = 5 * 1000;
    /**
     * 报告位置时间间隔
     */
    private final static int HANDLE_TIME = 10 * 1000;
    /***
     * 多少时间报告一次
     */
    private final static int HANDLE_TIME_LOCATION = 30 * 1000;
    /***
     * 发送定位信息，多少时间后停止，为了减少没必要的浪费
     */
    private final static long HANDLE_TIME_MAX = 60 * 60 * 1000;

    //屏幕关闭的时候不发送位置
    private boolean mScreeLock = false;

    private SharedPreferences mSharedPreferences;
    private final HandlerThread mHandlerThread;

    private class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HANDLE_LOCATION:
                    if (mScreeLock) {
                        return;
                    }
                    handLocationChanged(false, false);
                    startHandleTask();
                    return;

            }
            super.handleMessage(msg);
        }
    }

    private final WorkHandler mHandler;

    public static void systemReady(Context context) {
        VLocationManagerService instance = new VLocationManagerService(context);
        gService.set(instance);
    }

    public static VLocationManagerService get() {
        return gService.get();
    }

    private VLocationManagerService(Context context) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences("virtual_location", Context.MODE_PRIVATE);
        mHandlerThread = new HandlerThread("location_work");
        mHandlerThread.start();
        mHandler = new WorkHandler(mHandlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        if (CONFIG_SCREEN) {
            context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //mScreeLock
                    if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        mScreeLock = true;
                    } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                        mScreeLock = false;
                    }
                }
            }, intentFilter);
        } else {
            mScreeLock = false;
        }
        //
//        mSharedPreferences.edit().putString("user_" + userId, loc.getLatitude() + ":" + loc.getLongitude()).apply();
        Set<String> keys = mSharedPreferences.getAll().keySet();
        if (keys != null) {
            for (String key : keys) {
                if (key.startsWith("user_")) {
                    int userId = Integer.parseInt(key.replace("user_", ""));
                    String val = mSharedPreferences.getString(key, null);
                    if (val != null) {
                        String[] vars = val.split(":");
                        if (vars.length > 1) {
                            double lat = Double.parseDouble(vars[0]);
                            double lon = Double.parseDouble(vars[1]);
                            mLocations.put(userId, VLocationManager.makeGpsLocation(lat, lon));
                        }
                    }

                }
            }
        }
    }

    @Override
    public void setVirtualLocation(Location loc, String packageName, int userId) {
        synchronized (mLocations) {
            mLocations.put(userId, loc);
            mSharedPreferences.edit().putString("user_" + userId, loc.getLatitude() + ":" + loc.getLongitude()).apply();
        }
        handLocationChanged(false, true);
    }

    @Override
    public boolean hasVirtualLocation(String packageName, int userId) {
        if (DEBUG) {
            return true;
        }
        synchronized (mLocations) {
            return mLocations.get(userId) != null;
        }
    }

    @Override
    public Location getVirtualLocation(Location loc, String packageName, int userId) {
        Location location;
        if (DEBUG) {
            location = new Location(LocationManager.GPS_PROVIDER);
            location.setAltitude(5.1f);
            //30.4770829328,114.6423339844
            location.setAccuracy(120f);
            location.setLatitude(30.479449d);
            location.setLongitude(114.66834d);

            Bundle extras = new Bundle();
            if (extras.get("satellites") == null) {
                extras.putInt("satellites", 5);
            }
            location.setExtras(extras);
        } else {
            synchronized (mLocations) {
                location = mLocations.get(userId);
            }
        }
        if (location != null) {
            location.setTime(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= 17) {
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                Reflect.on(location).call("makeComplete");
            }
        }
        return location;
    }

    @Override
    public void addGpsStatusListener(IGpsStatusListener listener) {
        synchronized (mGpsStatusListeners) {
            if (!mGpsStatusListeners.contains(listener)) {
                mGpsStatusListeners.add(listener);
            }
        }
    }

    @Override
    public void removeGpsStatusListener(IGpsStatusListener listener) {
        synchronized (mGpsStatusListeners) {
            mGpsStatusListeners.remove(listener);
        }
    }

    //LocationRequest, ListenerTransport
    @Override
    public void addLocationListener(ILocationListener listener) {
        synchronized (mLocationListeners) {
            if (mLocationListeners.contains(listener)) {
                handLocationChanged(true, false);
                return;
            }
            mLocationListeners.add(listener);
        }
        mHandleStartTime = System.currentTimeMillis();
        handLocationChanged(true, false);
        startHandleTask();
    }

    @Override
    public void stopAllLocationRequest() {
        synchronized (mGpsStatusListeners) {
            mGpsStatusListeners.clear();
        }
        synchronized (mLocationListeners) {
            mLocationListeners.clear();
        }
    }

    @Override
    public void removeLocationListener(ILocationListener listener) {
        synchronized (mLocationListeners) {
            mLocationListeners.remove(listener);
        }
    }

    private void startHandleTask() {
        if (mScreeLock) return;
        synchronized (mLocationListeners) {
            if (mLocationListeners.size() == 0) {
                return;
            }
        }
        if (CONFIG_LOCATION_TIME) {
            if (System.currentTimeMillis() - mHandleStartTime >= HANDLE_TIME_MAX) {
                return;
            }
        }
        mHandler.removeMessages(MSG_HANDLE_LOCATION);
        mHandler.sendEmptyMessageDelayed(MSG_HANDLE_LOCATION, HANDLE_TIME + mRandom.nextInt(1000));
    }

    private void handLocationChanged(boolean start, boolean force) {
//        if (!start) {
//            if (System.currentTimeMillis() - mLastGPS >= HANDLE_TIME_GPS) {
//                reportGps = true;
//                mLastGPS = System.currentTimeMillis();
//            }
//        } else {
//            mLastGPS = System.currentTimeMillis();
//        }
        if (mScreeLock) return;
        if (REPORT_LOCATION_WITH_GPS_STATUS) {
            synchronized (mGpsStatusListeners) {
                for (int i = mGpsStatusListeners.size() - 1; i >= 0; i--) {
                    IGpsStatusListener listener = mGpsStatusListeners.get(i);
                    try {
                        if (listener.asBinder().isBinderAlive()
                                && listener.isAlive()) {
                            if (DEBUG)
                                Log.d("tmap", listener.getPackageName() + ":IGpsStatusListener");

                            if (start) {
                                listener.onGpsStarted();
                                GpsStatusGenerate.fakeGpsStatus(listener);
                            }
                        } else {
                            mGpsStatusListeners.remove(i);
                            if (DEBUG)
                                Log.d("tmap", "remove GpsStatusListener:");
                        }
                    } catch (Throwable e) {
                        mGpsStatusListeners.remove(i);
                        //
                    }
                }
            }
        }
//        if (!start) {
//            if (System.currentTimeMillis() - mLastLocation < HANDLE_TIME_LOCATION) {
//                return;
//            }
//            mLastLocation = System.currentTimeMillis();
//        } else {
//            mLastLocation = System.currentTimeMillis();
//        }
        synchronized (mLocationListeners) {
            for (int i = mLocationListeners.size() - 1; i >= 0; i--) {
                ILocationListener listener = mLocationListeners.get(i);
                try {
                    if (listener.asBinder().isBinderAlive() && listener.isAlive()) {
                        if (!start) {
                            Location loc = getVirtualLocation(null, listener.getPackageName(), listener.getUserId());
                            if (DEBUG)
                                Log.d("tmap", listener.getPackageName() + ":onLocationChanged:" + loc);
                            listener.onLocationChanged(loc);
                        } else {
                            listener.onProviderEnabled(LocationManager.GPS_PROVIDER);
                        }
                    } else {
                        mLocationListeners.remove(listener);
                        if (DEBUG)
                            Log.d("tmap", "remove LocationListener:");
                    }
                } catch (Throwable e) {
                    //ignore
                    mLocationListeners.remove(i);
                    if (DEBUG)
                        Log.w("tmap", "onLocationChanged", e);
                }
            }
        }
    }
}
