package com.lody.virtual.remote;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

/**
 * @author Lody
 */
public class VDeviceInfo implements Parcelable {
    private String deviceId;
    private String androidId;
    private String wifiMac;
    private String bluetoothMac;
    private String iccId;
    private String serial;
    private String gmsAdId;
    private String brand;
    private String board;
    private String product;
    private String device;
    private String Id;
    private String display;
    private String manufacturer;
    private String model;
    private String fingerprint;
    public static final int VERSION = 2;

    public void empty() {
        setSerial(null);
        setIccId(null);
        setWifiMac(null);
        setDeviceId(null);
        setAndroidId(null);
        setBluetoothMac(null);
        setGmsAdId(null);

        setBrand(null);
        setBoard(null);
        setProduct(null);
        setDevice(null);
        setID(null);
        setDisplay(null);
        setManufacturer(null);
        setModel(null);
        setFingerprint(null);
    }

    public void reset() {
        setBrand(Build.BRAND);
        setBoard(Build.BOARD);
        setProduct(Build.PRODUCT);
        setDevice(Build.DEVICE);
        setID(Build.ID);
        setDisplay(Build.DISPLAY);
        setSerial(Build.SERIAL);
        setManufacturer(Build.MANUFACTURER);
        setModel(Build.MODEL);
        setFingerprint(Build.FINGERPRINT);
    }

    private boolean isEmpty(String value, String defValue) {
        return TextUtils.isEmpty(value) || TextUtils.equals(value, defValue);
    }

    public boolean isEmpty(VDeviceInfo vDeviceInfo) {
        return isEmpty(deviceId, vDeviceInfo.deviceId)
                && isEmpty(androidId, vDeviceInfo.androidId)
                && isEmpty(bluetoothMac, vDeviceInfo.bluetoothMac)
                && isEmpty(iccId, vDeviceInfo.iccId)
                && isEmpty(wifiMac, vDeviceInfo.wifiMac)
                && isEmpty(serial, Build.SERIAL)
                && isEmpty(gmsAdId, vDeviceInfo.gmsAdId)
                && isEmpty(brand, Build.BRAND)
                && isEmpty(board, Build.BOARD)
                && isEmpty(product, Build.PRODUCT)
                && isEmpty(device, Build.DEVICE)
                && isEmpty(Id, Build.ID)
                && isEmpty(display, Build.DISPLAY)
                && isEmpty(manufacturer, Build.MANUFACTURER)
                && isEmpty(model, Build.MODEL)
                && isEmpty(fingerprint, Build.FINGERPRINT);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getID() {
        return Id;
    }

    public void setID(String id) {
        Id = id;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public String getWifiMac() {
        return wifiMac;
    }

    public void setWifiMac(String wifiMac) {
        this.wifiMac = wifiMac;
    }

    public String getBluetoothMac() {
        return bluetoothMac;
    }

    public void setBluetoothMac(String bluetoothMac) {
        this.bluetoothMac = bluetoothMac;
    }

    public String getIccId() {
        return iccId;
    }

    public void setIccId(String iccId) {
        this.iccId = iccId;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getGmsAdId() {
        return gmsAdId;
    }

    public void setGmsAdId(String gmsAdId) {
        this.gmsAdId = gmsAdId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceId);
        dest.writeString(this.androidId);
        dest.writeString(this.wifiMac);
        dest.writeString(this.bluetoothMac);
        dest.writeString(this.iccId);
        dest.writeString(this.serial);
        dest.writeString(this.gmsAdId);
        //version 2
        dest.writeString(this.brand);
        dest.writeString(this.board);
        dest.writeString(this.product);
        dest.writeString(this.device);
        dest.writeString(this.Id);
        dest.writeString(this.display);
        dest.writeString(this.manufacturer);
        dest.writeString(this.model);
        dest.writeString(this.fingerprint);
    }

    public VDeviceInfo() {}

    public VDeviceInfo(Parcel in, int version) {
        this.deviceId = in.readString();
        this.androidId = in.readString();
        this.wifiMac = in.readString();
        this.bluetoothMac = in.readString();
        this.iccId = in.readString();
        this.serial = in.readString();
        this.gmsAdId = in.readString();
        if(version > 1){
            this.brand = in.readString();
            this.board = in.readString();
            this.product = in.readString();
            this.device = in.readString();
            this.Id = in.readString();
            this.display = in.readString();
            this.manufacturer = in.readString();
            this.model = in.readString();
            this.fingerprint = in.readString();
        }
    }

    public static final Parcelable.Creator<VDeviceInfo> CREATOR = new Parcelable.Creator<VDeviceInfo>() {
        @Override
        public VDeviceInfo createFromParcel(Parcel source) {
            return new VDeviceInfo(source, VERSION);
        }

        @Override
        public VDeviceInfo[] newArray(int size) {
            return new VDeviceInfo[size];
        }
    };

    public File getWifiFile(int userId, boolean is64Bit) {
        if(TextUtils.isEmpty(wifiMac)){
            return null;
        }
        File wifiMacFie = VEnvironment.getWifiMacFile(userId, is64Bit);
        if (!wifiMacFie.exists()) {
            try {
                RandomAccessFile file = new RandomAccessFile(wifiMacFie, "rws");
                file.write((wifiMac + "\n").getBytes());
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wifiMacFie;
    }

    public static String genDeviceId(String imei, int userId) {
        if (imei == null) {
            imei = "864503010000000";
        }
        if (imei.length() < 15) {
            //meid;
            return generate10(System.currentTimeMillis() + userId, 14);
        } else {
            String pre = imei.substring(0, 6);
            String ot = imei.substring(6, 8);
            Random random = new Random(Long.parseLong(imei.substring(8, 14)) + userId);
            long num = random.nextLong();
            if (num < 0) {
                num = -num;
            }
            String last = "" + num;
            if (last.length() >= 6) {
                last = last.substring(0, 6);
            } else {
                int len = last.length();//1
                for (int i = 0; i < (6 - len); i++) {
                    last = "0" + last;
                }
            }
            return checkSum(pre + ot + last);
        }
    }

    private static String checkSum(String imeiString) {
        while (imeiString.length() < 15) {
            imeiString += "0";
        }
        int resultInt = 0;
        int len = 14;
        for (int i = 0; i < len; i++) {
            int a = Integer.parseInt(imeiString.substring(i, i + 1));
            i++;
            final int temp = Integer.parseInt(imeiString.substring(i, i + 1)) * 2;
            final int b = temp < 10 ? temp : temp - 9;
            resultInt += a + b;
        }
        resultInt %= 10;
        resultInt = resultInt == 0 ? 0 : 10 - resultInt;
        return imeiString.substring(0, 14) + resultInt;
    }

    public static String generate10(long seed, int length) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String generateHex(long seed, int length) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int nextInt = random.nextInt(16);
            if (nextInt < 10) {
                sb.append(nextInt);
            } else {
                sb.append((char) ((nextInt - 10) + 'a'));
            }
        }
        return sb.toString();
    }

}
