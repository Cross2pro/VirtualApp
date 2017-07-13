package com.lody.virtual.server.location;

import android.location.*;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.Reflect;

import java.util.Random;

public class GpsStatusGenerate {
    private static final int SVID_SHIFT_WIDTH = 7;
    private static final int CONSTELLATION_TYPE_SHIFT_WIDTH = 3;
    /**
     * Unknown constellation type.
     */
    private static final int CONSTELLATION_UNKNOWN = 0;
    /**
     * Constellation type constant for GPS.
     */
    private static final int CONSTELLATION_GPS = 1;
    /**
     * Constellation type constant for SBAS.
     */
    private static final int CONSTELLATION_SBAS = 2;
    /**
     * Constellation type constant for Glonass.
     */
    private static final int CONSTELLATION_GLONASS = 3;
    /**
     * Constellation type constant for QZSS.
     */
    private static final int CONSTELLATION_QZSS = 4;
    /**
     * Constellation type constant for Beidou.
     */
    private static final int CONSTELLATION_BEIDOU = 5;
    /**
     * Constellation type constant for Galileo.
     */
    private static final int CONSTELLATION_GALILEO = 6;
    /**
     * @hide
     */
    private static final int CONSTELLATION_TYPE_MASK = 0xf;

    private static final int GLONASS_SVID_OFFSET = 64;
    private static final int BEIDOU_SVID_OFFSET = 200;
    private static final int SBAS_SVID_OFFSET = -87;

    /**
     * @hide
     */
    private static final int GNSS_SV_FLAGS_NONE = 0;
    /**
     * @hide
     */
    private static final int GNSS_SV_FLAGS_HAS_EPHEMERIS_DATA = (1 << 0);
    /**
     * @hide
     */
    private static final int GNSS_SV_FLAGS_HAS_ALMANAC_DATA = (1 << 1);
    /**
     * @hide
     */
    private static final int GNSS_SV_FLAGS_USED_IN_FIX = (1 << 2);

    public static void onSvStatusChanged(Object mGpsStatusListenerTransport,
                                         int svCount,
                                         int[] prns, float[] snrs, float[] elevations, float[] azimuths,
                                         int ephemerisMask, int almanacMask, int usedInFixMask,
                                         int[] svidWithFlags){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            try {
                Reflect.on(mGpsStatusListenerTransport).call("onSvStatusChanged",
                        svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
            } catch (Exception e) {
                //TODO 上报方法参数
            }
            //15-23 public void onSvStatusChanged(int svCount, int[] prns, float[] snrs,float[] elevations, float[] azimuths, int ephemerisMask,int almanacMask, int usedInFixMask)
        } else {
            try {
                Reflect.on(mGpsStatusListenerTransport).call("onSvStatusChanged",
                        svCount, svidWithFlags, snrs, elevations, azimuths);
            } catch (Exception e) {
                //TODO 上报方法参数
            }
            //24 public void onSvStatusChanged(int svCount, int[] svidWithFlags, float[] snrs, float[] elevations, float[] azimuths)
        }
    }
    public static void fakeGpsStatus(LocationManager locationManager) {
        try {
            String clazz = Build.VERSION.SDK_INT < 24 ? "GpsStatusListenerTransport" : "GnssStatusListenerTransport";
            Object GpsStatusListenerTransport = Reflect.on(LocationManager.class.getName()
                    + "$"+clazz).create(locationManager, new android.location.GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                }
            }).get();
            fakeGpsStatus(GpsStatusListenerTransport);
            Log.i("tmap", "fakeGpsStatus1:ok");
        } catch (Exception e) {
            Log.w("tmap", "create", e);
            try {
                locationManager.addGpsStatusListener(new android.location.GpsStatus.Listener() {
                    @Override
                    public void onGpsStatusChanged(int event) {
                    }
                });
                Log.i("tmap", "fakeGpsStatus2:ok");
            } catch (Exception e2) {
                //ignore
            }
        }
    }
    /**
     * 24 GnssStatusListenerTransport
     *
     * @param mGpsStatusListenerTransport
     */
    public static void fakeGpsStatus(Object mGpsStatusListenerTransport) {
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
        if(mGpsStatusListenerTransport instanceof IGpsStatusListener){
            IGpsStatusListener listener=(IGpsStatusListener)mGpsStatusListenerTransport;
            try {
                listener.onSvStatusChanged(svCount,prns, snrs, elevations, azimuths, ephemerisMask, almanacMask,
                        usedInFixMask, svidWithFlags);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else {
            onSvStatusChanged(mGpsStatusListenerTransport, svCount,
                    prns, snrs, elevations, azimuths, ephemerisMask, almanacMask,
                    usedInFixMask, svidWithFlags);
        }
    }
}
