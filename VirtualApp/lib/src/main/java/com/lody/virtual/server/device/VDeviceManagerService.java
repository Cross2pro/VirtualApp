package com.lody.virtual.server.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.utils.PropertiesUtils;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.VDeviceInfo;
import com.lody.virtual.server.interfaces.IDeviceInfoManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * @author Lody
 */
public class VDeviceManagerService extends IDeviceInfoManager.Stub {

    private static VDeviceManagerService sInstance = new VDeviceManagerService();
    private final SparseArray<VDeviceInfo> mDeviceInfos = new SparseArray<>();
    private DeviceInfoPersistenceLayer mPersistenceLayer = new DeviceInfoPersistenceLayer(this);
    private UsedDeviceInfoPool mPool = new UsedDeviceInfoPool();
    private TelephonyManager mTelephonyManager;
    private Context mContext;

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
        mTelephonyManager= (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
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
                if (VASettings.KEEP_ADMIN_PHONE_INFO && userId == 0) {
                    info = defaultDevice();
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
            value = VDeviceInfo.genDeviceId(mTelephonyManager.getDeviceId(), userId);
            info.setDeviceId(value);
        } while (mPool.deviceIds.contains(value));
        do {
            value = VDeviceInfo.generate16(System.currentTimeMillis(), 16);
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

    @SuppressLint("HardwareIds")
    private VDeviceInfo defaultDevice() {
        VDeviceInfo info = new VDeviceInfo();
        info.setDeviceId(mTelephonyManager.getDeviceId());
        info.setAndroidId(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        info.setWifiMac(null);
        info.setBluetoothMac(null);
        info.setIccId(null);
        info.setSerial(Build.SERIAL);
        return info;
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

    @Override
    public Map getMockConfig(int userId, String packageName, boolean auto) {
        Editor editor;
        if(TextUtils.isEmpty(packageName)){
            editor = createSystemBuildEditor(userId);
        }else{
            editor = createAppBuildEditor(packageName, userId);
            if(auto && !editor.exists()){
                editor = createSystemBuildEditor(userId);
            }
        }
        Map<String, String> map = new HashMap<>();
        if (!editor.exists()) {
            return map;
        }
        for (Map.Entry<Object, Object> e : editor.properties.entrySet()) {
            String k = String.valueOf(e.getKey());
            String val = e.getValue() == null ? "" : String.valueOf(e.getValue());
            map.put(k, val);
        }
        return map;
    }

    @Override
    public void setMockConfig(int userId, String packageName, Map _map) {
        if(_map == null)return;
        Map<Object, Object> map = (Map<Object, Object>)_map;
        Editor editor;
        if(TextUtils.isEmpty(packageName)){
            editor = createSystemBuildEditor(userId);
        }else{
            editor = createAppBuildEditor(packageName, userId);
        }
        if(!editor.exists()){
            editor.setDefault();
        }
        for (Map.Entry<Object, Object> e : map.entrySet()) {
            String k = String.valueOf(e.getKey());
            String v = e.getValue() == null ? "" : e.getValue().toString();
            editor.set(k, v);
        }
        editor.save();
    }

    @Override
    public int getMockMode(int userId, String packageName) {
        Editor editor = createAppBuildEditor(packageName, userId);
        if (editor.exists()) {
            return VDeviceManager.MOCK_MODE_APP;
        }
        editor = createSystemBuildEditor(userId);
        if (editor.exists()) {
            return VDeviceManager.MOCK_MODE_USER;
        }
        return VDeviceManager.MOCK_MODE_NONE;
    }

    @Override
    public void removeMockConfig(int userId, String packageName){
        if(TextUtils.isEmpty(packageName)){
            VEnvironment.getSystemBuildFile(userId).delete();
        }else{
            VEnvironment.getAppBuildFile(packageName, userId).delete();
        }
    }

    /**
     * @see #createAppBuildEditor(String, int)
     * @see #createSystemBuildEditor(int)
     * @deprecated
     */
    public Editor createBuildEditor(int userId) {
        return createSystemBuildEditor(userId);
    }

    private Editor createAppBuildEditor(String packageName, int userId) {
        return new Editor(VEnvironment.getAppBuildFile(packageName, userId));
    }

    private Editor createSystemBuildEditor(int userId) {
        return new Editor(VEnvironment.getSystemBuildFile(userId));
    }

    private static class Editor {
        private final Properties properties = new Properties();
        private final File file;
        private boolean reset;

        public Editor(File file) {
            this.file = file;
            reload();
        }

            public boolean exists() {
            return file.exists();
        }

        public void setDefault() {
            setDefault("/system/build.prop");
        }

        public boolean hasDefault() {
            return properties.size() > 0;
        }

        public void setDefault(String path) {
            Properties system = new Properties();
            if (path != null && PropertiesUtils.load(system, new File(path))) {
                this.properties.clear();
                this.properties.putAll(system);
            } else {
                setBrand(Build.BRAND);
                setBoard(Build.BOARD);
                setProduct(Build.PRODUCT);
                setDevice(Build.DEVICE);
                setID(Build.ID);
                setDisplay(Build.DISPLAY);
                setSerial(Build.SERIAL);
                setManufacturer(Build.MANUFACTURER);
                setModel(Build.MODEL);
                setFingerprint(null);
            }
        }

        public String get(String key, String def) {
            return properties.getProperty(key, def);
        }

        public void set(String key, String value) {
            reset = false;
            if (value == null) {
                value = "";
            }
            properties.setProperty(key, value);
        }

        public void reset(boolean save) {
            reset = true;
            properties.clear();
            setDefault();
            if (save) {
                save();
            }
        }

        public void setModel(String brand) {
            set("ro.product.model", brand);
        }

        public String getModel() {
            return get("ro.product.model", Build.MODEL);
        }

        public void setBrand(String brand) {
            set("ro.product.brand", brand);
        }

        public String getBrand() {
            return get("ro.product.brand", Build.BRAND);
        }

        public void setBoard(String broad) {
            set("ro.product.board", broad);
        }

        public String getBoard() {
            return get("ro.product.board", Build.BOARD);
        }

        public void setProduct(String product) {
            set("ro.product.name", product);
        }

        public String getProduct() {
            return get("ro.product.name", Build.PRODUCT);
        }

        public void setDevice(String device) {
            set("ro.product.device", device);
        }

        public String getDevice() {
            return get("ro.product.device", Build.DEVICE);
        }

        public void setManufacturer(String device) {
            set("ro.product.manufacturer", device);
        }

        public String getManufacturer() {
            return get("ro.product.manufacturer", Build.MANUFACTURER);
        }

        public String genFingerprint() {
            return getBrand() + '/' +
                    getProduct() + '/' +
                    getDevice() + ':' +
                    getID();
        }

        public void setFingerprint(String device) {
            set("ro.build.fingerprint", device);
        }

        public String getFingerprint() {
            return get("ro.build.fingerprint", Build.FINGERPRINT);
        }

        public void setID(String id) {
            set("ro.build.id", id);
        }

        public String getID() {
            return get("ro.build.id", Build.ID);
        }

        public void setDisplay(String displayid) {
            set("ro.build.display.id", displayid);
        }

        public String getDisplay() {
            return get("ro.build.display.id", Build.DISPLAY);
        }

        public void setSerial(String serial) {
            set("no.such.thing", serial);
        }

        @SuppressLint("HardwareIds")
        public String getSerial() {
            return get("no.such.thing", Build.SERIAL);
        }

        /**
         * clear and load
         */
        public void reload() {
            properties.clear();
            PropertiesUtils.load(properties, file);
        }

        public boolean save() {
            if (reset) {
                if (file.exists()) {
                    file.delete();
                }
                return true;
            }
            return PropertiesUtils.save(properties, file, " begin build properties\n# autogenerated by buildinfo.sh");
        }
    }
}
