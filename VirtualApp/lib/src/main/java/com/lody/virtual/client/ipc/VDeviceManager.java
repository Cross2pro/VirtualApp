package com.lody.virtual.client.ipc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.remote.VDeviceInfo;
import com.lody.virtual.server.interfaces.IDeviceInfoManager;

import mirror.RefStaticObject;

/**
 * @author Lody
 */

public class VDeviceManager {

    private static final VDeviceManager sInstance = new VDeviceManager();

    public static VDeviceManager get() {
        return sInstance;
    }

    private VDeviceInfo mDefault;

    private IDeviceInfoManager mService;

    public IDeviceInfoManager getService() {
        if (mService == null || !IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                mService = LocalProxyUtils.genProxy(IDeviceInfoManager.class, binder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IDeviceInfoManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.DEVICE));
    }


    public VDeviceInfo getDeviceInfo(int userId) {
        try {
            return getService().getDeviceInfo(userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void updateDeviceInfo(int userId, VDeviceInfo deviceInfo) {
        try {
            getService().updateDeviceInfo(userId, deviceInfo);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public VDeviceInfo getDefaultDeviceInfo(Context context) {
        if (mDefault == null) {
            synchronized (this) {
                if (mDefault == null) {
                    mDefault = defaultDevice(context);
                }
            }
        }
        return mDefault;
    }

    @SuppressLint("HardwareIds")
    public static VDeviceInfo defaultDevice(Context context) {
        TelephonyManager telephonyManager= (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        VDeviceInfo info = new VDeviceInfo();
        try {
            info.setDeviceId(telephonyManager.getDeviceId());
        } catch (Exception e) {
            //ignore
        }
        info.setAndroidId(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        try {
            info.setWifiMac(null);
        } catch (Exception e) {
            //ignore
        }
        info.setBluetoothMac(null);
        info.setSerial(Build.SERIAL);
        info.setIccId(null);
        return info;
    }

    private void setBuild(RefStaticObject<String> field, String val){
        if(!TextUtils.isEmpty(val)){
            field.set(val);
        }
    }

    public void attachBuildProp(VDeviceInfo info){
        mirror.android.os.Build.DEVICE.set(Build.DEVICE.replace(" ", "_"));
        if(info != null) {
            setBuild(mirror.android.os.Build.DEVICE, info.getDevice());
            setBuild(mirror.android.os.Build.SERIAL, info.getSerial());
            setBuild(mirror.android.os.Build.MODEL, info.getModel());
            setBuild(mirror.android.os.Build.BRAND, info.getBrand());
            setBuild(mirror.android.os.Build.BOARD, info.getBoard());
            setBuild(mirror.android.os.Build.PRODUCT, info.getProduct());
            setBuild(mirror.android.os.Build.ID, info.getID());
            setBuild(mirror.android.os.Build.DISPLAY, info.getDisplay());
            setBuild(mirror.android.os.Build.MANUFACTURER, info.getManufacturer());
            setBuild(mirror.android.os.Build.FINGERPRINT, info.getFingerprint());
        }
    }
}
