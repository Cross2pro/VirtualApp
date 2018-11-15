package com.lody.virtual.server.interfaces;

import android.os.RemoteException;
import java.util.List;
import com.lody.virtual.remote.VDeviceInfo;

/**
 * @author Lody
 */
interface IDeviceInfoManager{

    VDeviceInfo getDeviceInfo(int userId);

    void updateDeviceInfo(int userId,in  VDeviceInfo info);

    Map getMockConfig(int userId, String packageName, boolean auto);

    void setMockConfig(int userId, String packageName,in Map list);

    int getMockMode(int userId, String packageName);

    void removeMockConfig(int userId, String packageName);
}
