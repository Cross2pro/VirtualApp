package com.xdja.zs;

import android.os.RemoteException;

import java.util.List;

/**
 * @Date 19-11-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class InstallerSettingManager {

    public static List<String> getSystemApps() {
        try {
            return InstallerSettingService.get().getSystemApps();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setSystemApps(List<String> list) {
        try {
            InstallerSettingService.get().setSystemApps(list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void addSystemApp(String packagename){
        try {
            InstallerSettingService.get().addSystemApp(packagename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void removeSystemApp(String packagename) {
        try {
            InstallerSettingService.get().removeSystemApp(packagename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static boolean isSystemApp(String pkg) {
        try {
            return InstallerSettingService.get().isSystemApp(pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }
}
