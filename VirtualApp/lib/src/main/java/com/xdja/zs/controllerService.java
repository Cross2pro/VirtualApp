package com.xdja.zs;

/**
 * Created by zhangsong on 18-1-23.
 */

import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.ipc.VAppPermissionManager;
import com.lody.virtual.helper.utils.Singleton;

import java.util.HashMap;
import java.util.HashSet;

public class controllerService extends IController.Stub {
    private final String Tag = "controllerService";

    private static HashSet<String> GATEWAY_list = new HashSet<String>();
    private static HashMap<String, HashSet<String>> IPMAP = new HashMap<>();
    private static HashSet<String> JXIP_list = new HashSet<String>();
    private static boolean activitySwitchFlag = true;

    static {

        //Gatway
        //GATEWAY_list.add("com.xdja.jxclient");
        //GATEWAY_list.add("com.tencent.mm");
        //GATEWAY_list.add("cn.wps.moffice");
        //setIPMAP();

    }

    private static final Singleton<controllerService> sService = new Singleton<controllerService>() {
        @Override
        protected controllerService create() {
            return new controllerService();
        }
    };

    public static controllerService get() {
        return sService.get();
    }

    @Override
    public boolean isNetworkEnable(String packageName) throws RemoteException {

        boolean appPermissionEnable = VAppPermissionManager.get().getAppPermissionEnable(packageName,
                VAppPermissionManager.PROHIBIT_NETWORK);
        Log.e(Tag, "isNetworkEnable getAppPermissionEnable : " + packageName + " " + appPermissionEnable);
        boolean ret = !appPermissionEnable;
        Log.e(Tag, "isNetworkEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean isCameraEnable(String packageName) throws RemoteException {
        VAppPermissionManager vapm = VAppPermissionManager.get();
        boolean appPermissionEnable = vapm.getAppPermissionEnable(packageName,
                VAppPermissionManager.PROHIBIT_CAMERA);
        Log.e(Tag, "isCameraEnable getAppPermissionEnable : " + packageName + " " + appPermissionEnable);
        if (appPermissionEnable) {
            vapm.interceptorTriggerCallback(packageName, VAppPermissionManager.PROHIBIT_CAMERA);
        }
        boolean ret = !appPermissionEnable;
        Log.e(Tag, "isCameraEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean isGatewayEnable(String packageName) throws RemoteException {
        boolean ret = false;

        for (String item : GATEWAY_list) {
            if (packageName.startsWith(item)) {
                ret = true;
                break;
            }
        }

        Log.e(Tag, "isGatewayEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean isChangeConnect(String packageName, int port, String ip) throws RemoteException {
        boolean ret = false;
        Log.e(Tag, "PackageName : " + packageName + " Ip " + ip + " Port " + port);

        if (IPMAP.get(packageName) != null) {
            for (String item : IPMAP.get(packageName)) {
                String str = String.valueOf(port) + ip;
                if (str.equals(item)) {
                    ret = true;
                    break;
                }
            }
        }
        Log.e(Tag, "isChangeConnect : " + ret);
        return ret;
    }

    @Override
    public boolean getActivitySwitch() throws RemoteException {
        Log.e(Tag, "getActivitySwitch : " + activitySwitchFlag);
        return activitySwitchFlag;
    }

    @Override
    public void setActivitySwitch(boolean switchFlag) throws RemoteException {
        Log.e(Tag, "setActivitySwitch : " + switchFlag);
        activitySwitchFlag = switchFlag;
    }

    private static void setIPMAP() {
        String serverip_6 = "::ffff:120.194.4.131";
        String serverip_4 = "120.194.4.131";
        IPMAP.put("com.xdja.jxclient", JXIP_list);

        JXIP_list.add("5222" + serverip_4);
        JXIP_list.add("5060" + serverip_4);
        JXIP_list.add("2055" + serverip_6);
        JXIP_list.add("8010" + serverip_6);
        JXIP_list.add("8210" + serverip_6);
        JXIP_list.add("8040" + serverip_6);
        JXIP_list.add("8211" + serverip_6);
        JXIP_list.add("2011" + serverip_6);
        JXIP_list.add("5061" + serverip_6);
        JXIP_list.add("8030" + serverip_6);
        JXIP_list.add("9030" + serverip_6);
        JXIP_list.add("15306" + serverip_6);
    }

}
