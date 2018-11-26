package io.virtualapp.home.models;

import android.content.Context;

import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.VDeviceInfo;

public class DeviceData extends SettingsData {
    private VDeviceInfo defInfo;
    public DeviceData(Context context, InstalledAppInfo installedAppInfo, int userId) {
        super(context, installedAppInfo, userId);
        defInfo = VDeviceManager.get().getDefaultDeviceInfo(context);
    }

    public boolean isMocking() {
        VDeviceInfo deviceInfo = VDeviceManager.get().getDeviceInfo(userId);
        return !deviceInfo.isEmpty(defInfo);
    }
}
