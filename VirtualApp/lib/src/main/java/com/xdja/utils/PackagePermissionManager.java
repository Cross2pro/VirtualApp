package com.xdja.utils;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date 19-4-19 15
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class PackagePermissionManager {

    //keep alive
    private static ArrayList<String> mKeepLiveList = new ArrayList<>();
    //can't uninstall
    private static ArrayList<String> mNoUnInstallList =  new ArrayList<>();

    public static ArrayList<String> getKeepLiveList(){
        return mKeepLiveList;
    }
    public static void setKeepLiveList(@NonNull ArrayList<String> list){
        synchronized (mKeepLiveList){
            mKeepLiveList = list;
            mKeepLiveList.add("com.xdja.dialer");
        }
    }
    public static boolean isKeepLiveApp(@NonNull String pkg){
        return mKeepLiveList.contains(pkg);
    }
    public static ArrayList<String> getProtectUninstallList(){
        return mNoUnInstallList;
    }
    public static void setProtectUninstallList(@NonNull ArrayList<String> list){
        synchronized (mNoUnInstallList){
            mNoUnInstallList = list;
        }
    }
    public static boolean isProtectUninstallApp(@NonNull String pkg){
        return mNoUnInstallList.contains(pkg);
    }

    private static ArrayList<String> EnabledInstallationSource = new ArrayList<>();
    /**
     * 控制安全域内安装源接口
     *
     * @param bundle
     */
    public void setEnableInstallationSource(Bundle bundle) {
        ArrayList<String> apps = bundle.getStringArrayList("installationSourceList");
        synchronized (EnabledInstallationSource){
            if (apps != null && !apps.isEmpty()) {
                EnabledInstallationSource = apps;
            } else {
                EnabledInstallationSource.clear();
            }
        }
    }
    public ArrayList<String> setEnableInstallationSource(){
        return EnabledInstallationSource;
    }
}