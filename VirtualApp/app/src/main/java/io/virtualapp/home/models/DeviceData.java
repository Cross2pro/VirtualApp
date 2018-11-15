package io.virtualapp.home.models;

import android.content.Context;

import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.remote.InstalledAppInfo;

public class DeviceData extends SettingsData {
    private VDeviceManager.Editor mEditor;
    private VDeviceManager.Editor mGEditor;

    public DeviceData(Context context, InstalledAppInfo installedAppInfo, int userId) {
        super(context, installedAppInfo, userId);
    }
    public VDeviceManager.Editor getUserEditor() {
        if (mGEditor == null) {
            mGEditor = VDeviceManager.get().createSystemBuildEditor(userId);
        }
        return mGEditor;
    }
    public VDeviceManager.Editor getEditor() {
        if (mEditor == null) {
            mEditor = VDeviceManager.get().createAppBuildEditor(packageName, userId);
        }
        return mEditor;
    }
}
