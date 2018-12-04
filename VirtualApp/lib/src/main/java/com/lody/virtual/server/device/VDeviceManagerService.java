package com.lody.virtual.server.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.remote.VDeviceInfo;
import com.lody.virtual.server.interfaces.IDeviceInfoManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Lody
 */
public class VDeviceManagerService extends IDeviceInfoManager.Stub {

    private static VDeviceManagerService sInstance = new VDeviceManagerService();
    private final SparseArray<VDeviceInfo> mDeviceInfos = new SparseArray<>();
    private DeviceInfoPersistenceLayer mPersistenceLayer = new DeviceInfoPersistenceLayer(this);
    private UsedDeviceInfoPool mPool = new UsedDeviceInfoPool();
    private Context mContext;
    private String defDeviceId;

    public static VDeviceManagerService get() {
        return sInstance;
    }

    private final class UsedDeviceInfoPool {
        List<String> deviceIds = new ArrayList<>();
        List<String> androidIds = new ArrayList<>();
        List<String> wifiMacs = new ArrayList<>();
        List<String> bluetoothMacs = new ArrayList<>();
        List<String> iccIds = new ArrayList<>();
    }

    public static void systemReady(Context context) {
        get().init(context);
    }

    private void init(Context context){
        mContext = context;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            defDeviceId = telephonyManager.getDeviceId();
        }catch (Throwable e){
            //ignore
        }
    }

    private VDeviceManagerService() {
        mPersistenceLayer.read();
        for (int i = 0; i < mDeviceInfos.size(); i++) {
            VDeviceInfo info = mDeviceInfos.valueAt(i);
            addDeviceInfoToPool(info);
        }
    }

    private void addDeviceInfoToPool(VDeviceInfo info) {
        mPool.deviceIds.add(info.getDeviceId());
        mPool.androidIds.add(info.getAndroidId());
        mPool.wifiMacs.add(info.getWifiMac());
        mPool.bluetoothMacs.add(info.getBluetoothMac());
        mPool.iccIds.add(info.getIccId());
    }

    @Override
    public VDeviceInfo getDeviceInfo(int userId) {
        VDeviceInfo info;
        synchronized (mDeviceInfos) {
            info = mDeviceInfos.get(userId);
            if (info == null) {
                if (VirtualCore.getConfig().isKeepAdminDeviceInfo() && userId == 0) {
                    info = VDeviceManager.defaultDevice(mContext);
                } else {
                    info = generateRandomDeviceInfo(userId);
                }
                mDeviceInfos.put(userId, info);
                mPersistenceLayer.save();
            }
        }
        return info;
    }

    @Override
    public void updateDeviceInfo(int userId, VDeviceInfo info) {
        synchronized (mDeviceInfos) {
            if (info != null) {
                mDeviceInfos.put(userId, info);
                mPersistenceLayer.save();
            }
        }
    }

    @SuppressLint("HardwareIds")
    private VDeviceInfo generateRandomDeviceInfo(int userId) {
        VDeviceInfo info = new VDeviceInfo();
        String value;
        do {
            value = VDeviceInfo.genDeviceId(defDeviceId, userId);
            info.setDeviceId(value);
        } while (mPool.deviceIds.contains(value));
        do {
            value = VDeviceInfo.generateHex(System.currentTimeMillis(), 16);
            info.setAndroidId(value);
        } while (mPool.androidIds.contains(value));
        do {
            value = generateMac();
            info.setWifiMac(value);
        } while (mPool.wifiMacs.contains(value));
        do {
            value = generateMac();
            info.setBluetoothMac(value);
        } while (mPool.bluetoothMacs.contains(value));

        do {
            value = VDeviceInfo.generate10(System.currentTimeMillis(), 20);
            info.setIccId(value);
        } while (mPool.iccIds.contains(value));

        info.setSerial(generateSerial());

        addDeviceInfoToPool(info);
        return info;
    }

    SparseArray<VDeviceInfo> getDeviceInfos() {
        return mDeviceInfos;
    }

    private static String generateMac() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        int next = 1;
        int cur = 0;
        while (cur < 12) {
            int val = random.nextInt(16);
            if (val < 10) {
                sb.append(val);
            } else {
                sb.append((char) (val + 87));
            }
            if (cur == next && cur != 11) {
                sb.append(":");
                next += 2;
            }
            cur++;
        }
        return sb.toString();
    }

    @SuppressLint("HardwareIds")
    private static String generateSerial() {
        String serial;
        if (Build.SERIAL == null || Build.SERIAL.length() <= 0) {
            serial = "0123456789ABCDEF";
        } else {
            serial = Build.SERIAL;
        }
        List<Character> list = new ArrayList<>();
        for (char c : serial.toCharArray()) {
            list.add(c);
        }
        Collections.shuffle(list);
        StringBuilder sb = new StringBuilder();
        for (Character c : list) {
            sb.append(c.charValue());
        }
        return sb.toString();
    }
}
