package com.lody.virtual.remote;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

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
    }

    public VDeviceInfo() {}

    public VDeviceInfo(Parcel in) {
        this.deviceId = in.readString();
        this.androidId = in.readString();
        this.wifiMac = in.readString();
        this.bluetoothMac = in.readString();
        this.iccId = in.readString();
        this.serial = in.readString();
        this.gmsAdId = in.readString();
    }

    public static final Parcelable.Creator<VDeviceInfo> CREATOR = new Parcelable.Creator<VDeviceInfo>() {
        @Override
        public VDeviceInfo createFromParcel(Parcel source) {
            return new VDeviceInfo(source);
        }

        @Override
        public VDeviceInfo[] newArray(int size) {
            return new VDeviceInfo[size];
        }
    };

    public File getWifiFile(int userId) {
        File wifiMacFie = VEnvironment.getWifiMacFile(userId);
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
            Random random = new Random(Long.parseLong(imei.substring(8, 14)));
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

    public static String generate16(long seed, int length) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int nextInt = random.nextInt(16);
            if (nextInt < 10) {
                sb.append(nextInt);
            } else {
                sb.append((char) (nextInt + 87));
            }
        }
        return sb.toString();
    }

}
