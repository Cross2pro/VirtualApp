package com.xdja.utils;

import android.support.annotation.NonNull;

import java.util.ArrayList;

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
        mKeepLiveList = list;
    }
    public static boolean isKeepLiveApp(@NonNull String pkg){
        return mKeepLiveList.contains(pkg);
    }
    public static ArrayList<String> getProtectUninstallList(){
        return mNoUnInstallList;
    }
    public static void setProtectUninstallList(@NonNull ArrayList<String> list){
        mNoUnInstallList = list;
    }
    public static boolean isProtectUninstallApp(@NonNull String pkg){
        return mNoUnInstallList.contains(pkg);
    }

}