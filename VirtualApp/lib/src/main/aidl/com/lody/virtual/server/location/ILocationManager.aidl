// ILocationManager.aidl
package com.lody.virtual.server.location;

import android.location.Location;
import com.lody.virtual.server.location.IGpsStatusListener;
import com.lody.virtual.server.location.ILocationListener;

interface ILocationManager {

    void setVirtualLocation(in Location loc, int userId);

    boolean hasVirtualLocation(int userId);

    Location getVirtualLocation(in Location loc, int userId);

    void addGpsStatusListener(in IGpsStatusListener listener);

    void removeGpsStatusListener(in IGpsStatusListener listener);

    void addLocationListener(in ILocationListener listener);

    void removeLocationListener(in ILocationListener listener);
}
