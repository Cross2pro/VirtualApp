// IGpsStatusListener.aidl
package com.lody.virtual.server.location;


interface IGpsStatusListener {
    void onGpsStarted();
    void onGpsStopped();
    void onFirstFix(int ttff);
    void onSvStatusChanged(int svCount, in int[] prns, in float[] snrs,
            in float[] elevations, in float[] azimuths,
            int ephemerisMask, int almanacMask, int usedInFixMask,
            in int[] svidWithFlags);
    void onGpsStatusChanged();
    int getUserId();
    int getPid();
    String getPackageName();
    boolean isAlive();
}
