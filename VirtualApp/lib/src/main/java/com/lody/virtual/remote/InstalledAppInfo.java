package com.lody.virtual.remote;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Parcel;
import android.os.Parcelable;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VEnvironment;

import java.io.File;

/**
 * @author Lody
 */
public final class InstalledAppInfo implements Parcelable {

    public String packageName;
    public String apkPath;
    public String libPath;
    public boolean notCopyApk;
    public int appId;

    public InstalledAppInfo(String packageName, String apkPath, String libPath, boolean notCopyApk, int appId) {
        this.packageName = packageName;
        this.apkPath = apkPath;
        this.libPath = libPath;
        this.notCopyApk = notCopyApk;
        this.appId = appId;
    }

    public File getOdexFile(){
        return getOdexFile(VirtualCore.get().is64BitEngine());
    }

    public File getOdexFile(boolean is64Bit) {
        if(is64Bit){
            return VEnvironment.getOdexFile64(packageName);
        }
        return VEnvironment.getOdexFile(packageName);
    }

    public ApplicationInfo getApplicationInfo(int userId) {
        return VPackageManager.get().getApplicationInfo(packageName, 0, userId);
    }

    public PackageInfo getPackageInfo(int userId) {
        return VPackageManager.get().getPackageInfo(packageName, 0, userId);
    }

    public int[] getInstalledUsers() {
        return VirtualCore.get().getPackageInstalledUsers(packageName);
    }

    public boolean isLaunched(int userId) {
        return VirtualCore.get().isPackageLaunched(userId, packageName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.apkPath);
        dest.writeString(this.libPath);
        dest.writeByte(this.notCopyApk ? (byte) 1 : (byte) 0);
        dest.writeInt(this.appId);
    }

    protected InstalledAppInfo(Parcel in) {
        this.packageName = in.readString();
        this.apkPath = in.readString();
        this.libPath = in.readString();
        this.notCopyApk = in.readByte() != 0;
        this.appId = in.readInt();
    }

    public static final Creator<InstalledAppInfo> CREATOR = new Creator<InstalledAppInfo>() {
        @Override
        public InstalledAppInfo createFromParcel(Parcel source) {
            return new InstalledAppInfo(source);
        }

        @Override
        public InstalledAppInfo[] newArray(int size) {
            return new InstalledAppInfo[size];
        }
    };
}
