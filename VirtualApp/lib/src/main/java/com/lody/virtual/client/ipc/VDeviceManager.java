package com.lody.virtual.client.ipc;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.helper.utils.PropertiesUtils;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.VDeviceInfo;
import com.lody.virtual.server.interfaces.IDeviceInfoManager;

import java.io.File;
import java.util.Properties;

/**
 * @author Lody
 */

public class VDeviceManager {

    private static final VDeviceManager sInstance = new VDeviceManager();
    private IPCSingleton<IDeviceInfoManager> singleton = new IPCSingleton<>(IDeviceInfoManager.class);

    public static VDeviceManager get() {
        return sInstance;
    }

    public IDeviceInfoManager getService() {
        return singleton.get();
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

    /**
     * @see com.lody.virtual.helper.utils.PropertiesUtils#load
     * @see com.lody.virtual.helper.utils.PropertiesUtils#save
     */
    public File getSystemBuildFile(int userId) {
        return VEnvironment.getSystemBuildFile(userId);
    }

    /**
     * @see com.lody.virtual.helper.utils.PropertiesUtils#load
     * @see com.lody.virtual.helper.utils.PropertiesUtils#save
     */
    public File getAppBuildFile(String packageName, int userId) {
        return VEnvironment.getAppBuildFile(packageName, userId);
    }

    /**
     * only get using build.prop
     * @hide
     * @see com.lody.virtual.helper.utils.PropertiesUtils#load
     * @see com.lody.virtual.helper.utils.PropertiesUtils#save
     */
    public File getBuildFile(String packageName, int userId) {
        File appFile = getAppBuildFile(packageName, userId);
        if (appFile.exists()) {
            return appFile;
        }
        return getSystemBuildFile(userId);
    }

    /**
     * @deprecated
     * @see #createAppBuildEditor(String, int)
     * @see #createSystemBuildEditor(int)
     */
    public Editor createBuildEditor(int userId) {
        return createSystemBuildEditor(userId);
    }

    public Editor createAppBuildEditor(String packageName, int userId) {
        return new Editor(getAppBuildFile(packageName, userId));
    }

    public Editor createSystemBuildEditor(int userId) {
        return new Editor(getSystemBuildFile(userId));
    }

    public static class Editor {
        private final Properties properties = new Properties();
        private final File file;

        public Editor(File file) {
            this.file = file;
            reload();
        }

        public void setDefault() {
            setDefault("/system/build.prop");
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
            }
        }

        public String get(String key, String def) {
            return properties.getProperty(key, def);
        }

        public void set(String key, String value) {
            properties.setProperty(key, value);
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
            return PropertiesUtils.save(properties, file, " begin build properties\n# autogenerated by buildinfo.sh");
        }
    }
}
