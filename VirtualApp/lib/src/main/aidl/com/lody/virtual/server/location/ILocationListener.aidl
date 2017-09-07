// ILocationListener.aidl
package com.lody.virtual.server.location;

import android.location.Location;
import android.os.Bundle;

interface ILocationListener {
    void onLocationChanged(in Location location);
    void onStatusChanged(String provider, int status, in Bundle extras);
    void onProviderEnabled(String provider);
    void onProviderDisabled(String provider);
    int getUserId();
    int getPid();
    String getPackageName();
    boolean isAlive();
    long getInterval();
}
