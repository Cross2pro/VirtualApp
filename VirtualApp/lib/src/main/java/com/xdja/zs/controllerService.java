package com.xdja.zs;

/**
 * Created by zhangsong on 18-1-23.
 */

import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;
import com.xdja.activitycounter.ActivityCounterManager;
import com.xdja.zs.netstrategy.BlackNetStrategyPersistenceLayer;
import com.xdja.zs.netstrategy.TurnOnOffNetPersistenceLayer;
import com.xdja.zs.netstrategy.WhiteNetStrategyPersistenceLayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class controllerService extends IController.Stub {
    private final String Tag = "controllerService";

    private static HashSet<String> GATEWAY_list = new HashSet<String>();
    private static HashMap<String, HashSet<String>> IPMAP = new HashMap<>();
    private static HashSet<String> JXIP_list = new HashSet<String>();
    private static boolean activitySwitchFlag = true;
    public Map<String,Integer> White_Network_Strategy = new HashMap<String,Integer>();
    private BlackNetStrategyPersistenceLayer mBlackNetStrategyPersistence = new BlackNetStrategyPersistenceLayer(this);
    public Map<String,Integer> Black_Network_Strategy = new HashMap<String,Integer>();
    private WhiteNetStrategyPersistenceLayer mWhiteNetStrategyPersistence = new WhiteNetStrategyPersistenceLayer(this);
    public static boolean NetworkStragegyOnorOff = false;
    public static boolean isWhiteOrBlackFlag = false;
    private TurnOnOffNetPersistenceLayer mNetworkPersistence = new TurnOnOffNetPersistenceLayer(this);
    private IControllerServiceCallback mCSCallback = null;
    private IToastCallback mToastCallback = null;
    private static HashSet<String> packagenames = new HashSet<>();
    public controllerService() {
        init();
    }

    private void init() {
        mNetworkPersistence.read();
        mBlackNetStrategyPersistence.read();
        mWhiteNetStrategyPersistence.read();
    }

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
    public boolean isSoundRecordEnable(String packageName) throws RemoteException {
        VAppPermissionManager vapm = VAppPermissionManager.get();
        boolean appPermissionEnable = vapm.getAppPermissionEnable(packageName,
                VAppPermissionManager.PROHIBIT_SOUND_RECORD);
        Log.e(Tag, "isSoundRecordEnable getAppPermissionEnable : " + packageName + " " + appPermissionEnable);
        if (appPermissionEnable) {
            vapm.interceptorTriggerCallback(packageName, VAppPermissionManager.PROHIBIT_SOUND_RECORD);
        }
        boolean ret = !appPermissionEnable;
        Log.e(Tag, "isSoundRecordEnable : " + packageName + " " + ret);
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

    @Override
    public void registerCallback(IControllerServiceCallback csCallback) throws RemoteException {
        VLog.e(Tag, "controllerService registerCallback ");
        if(csCallback != null){
            mCSCallback = csCallback;
        }else {
            VLog.e(Tag, "controllerService csCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterCallback() throws RemoteException {
        VLog.e(Tag, "controllerService unregisterCallback ");
        mCSCallback = null;
    }

    @Override
    public void appStart(String packageName) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appStart(packageName);
                VLog.e(Tag, "appStart " + packageName);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void appStop(String packageName) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appStop(packageName);
                VLog.e(Tag, "appStop " + packageName);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void appProcessStart(String packageName, String processName, int pid) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appProcessStart(packageName, processName, pid);
                VLog.e(Tag, "appProcessStart " + packageName + " process : " + processName + pid);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void appProcessStop(String packageName, String processName, int pid) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appProcessStop(packageName, processName, pid);
                VLog.e(Tag, "appProcessStop " + packageName + "process : " + processName + pid);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBinderAlive() {
        return super.isBinderAlive();
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

    @Override
    public void registerToastCallback(IToastCallback iToastCallback) throws RemoteException{
        VLog.d(Tag, "registerCallback IToastCallback");
        if(iToastCallback != null) {
            mToastCallback = iToastCallback;
        } else {
            VLog.e(Tag, "controllerService iToastCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterToastCallback() throws RemoteException {
        VLog.d(Tag, "controllerService unregisterToastCallback ");
        mCSCallback = null;
    }

    @Override
    public void OnOrOffNetworkStrategy(boolean isOnOrOff) throws RemoteException {
        VLog.d(Tag,"OnOrOffNetworkStrategy isOnOrOff " + isOnOrOff);
        NetworkStragegyOnorOff = isOnOrOff;
        mNetworkPersistence.save();
    }

    @Override
    public boolean isIpV6Enable(String packageName, String ipv6) throws RemoteException {
        if (NetworkStragegyOnorOff) {
            if(ipv6 != null) {
                if(isWhiteOrBlackFlag) {
                    if(White_Network_Strategy != null && !White_Network_Strategy.isEmpty()) {
                        for(Map.Entry<String,Integer> entry:White_Network_Strategy.entrySet()) {
                            String network_strategy = entry.getKey();
                            int network_type = entry.getValue();
                            if(network_type == 1) {//ip
                                String splictIp = null;
                                if(ipv6.contains(".")) {
                                    String[] strs = ipv6.split(":");
                                    splictIp = strs[strs.length - 1];
                                    if(network_strategy.contains("-")) {
                                        if(judgeIpSection(splictIp,network_strategy)) {
                                            return true;
                                        }
                                    } else if(network_strategy.contains("/")) {
                                        if(judgeIpSubnet(splictIp,network_strategy)) {
                                            return true;
                                        }
                                    } else {
                                        if(judgeIp(splictIp,network_strategy)) {
                                            return true;
                                        }
                                    }
                                }
                            } else if(network_type == 2) {//domain name
                                if(network_strategy.contains("*")) {
                                    network_strategy = network_strategy.replace("*", "www");
                                }
                                if(judgeIpV6Domain(ipv6,network_strategy)) {
                                    return true;
                                }
                            }
                        }
                        showToast(packageName);
                        return false;
                    }
                    showToast(packageName);
                    return false;
                } else {
                    if(Black_Network_Strategy != null && !Black_Network_Strategy.isEmpty()) {
                        for(Map.Entry<String,Integer> entry:Black_Network_Strategy.entrySet()) {
                            String network_strategy = entry.getKey();
                            int network_type = entry.getValue();
                            if(network_type == 1) {//ip
                                String splictIp = null;
                                if(ipv6.contains(".")) {
                                    String[] strs = ipv6.split(":");
                                    splictIp = strs[strs.length - 1];
                                    if(network_strategy.contains("-")) {
                                        if(judgeIpSection(splictIp,network_strategy)) {
                                            showToast(packageName);
                                            return false;
                                        }
                                    } else if(network_strategy.contains("/")) {
                                        if(judgeIpSubnet(splictIp,network_strategy)) {
                                            showToast(packageName);
                                            return false;
                                        }
                                    } else {
                                        if(judgeIp(splictIp,network_strategy)) {
                                            showToast(packageName);
                                            return false;
                                        }
                                    }
                                }
                            } else if(network_type == 2) {//domain name
                                if(network_strategy.contains("*")) {
                                    network_strategy = network_strategy.replace("*", "www");
                                }
                                if(judgeIpV6Domain(ipv6,network_strategy)) {
                                    showToast(packageName);
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isIpOrNameEnable(String packageName, String ip) throws RemoteException{
        if(NetworkStragegyOnorOff) {
            if(ip != null) {
                //handle white list
                if (isWhiteOrBlackFlag) {
                    if(White_Network_Strategy != null && !White_Network_Strategy.isEmpty()) {
                        for(Map.Entry<String,Integer> entry:White_Network_Strategy.entrySet()) {
                            String network_strategy = entry.getKey();
                            int network_type = entry.getValue();
                            if(network_type == 1) {//ip
                                if(network_strategy.contains("-")) {
                                    if(judgeIpSection(ip,network_strategy)) {
                                        return true;
                                    }
                                } else if(network_strategy.contains("/")) {
                                    if(judgeIpSubnet(ip,network_strategy)) {
                                        return true;
                                    }
                                } else {
                                    if(judgeIp(ip,network_strategy)) {
                                        return true;
                                    }
                                }
                            } else if(network_type == 2) {//domain name
                                if(network_strategy.contains("*")) {
                                    network_strategy = network_strategy.replace("*", "www");
                                }
                                if(judgeDomain(ip,network_strategy)) {
                                    return true;
                                }
                            }
                        }
                        showToast(packageName);
                        return false;
                    }
                    showToast(packageName);
                    return false;
                } else {// handle black list
                    if(Black_Network_Strategy != null && !Black_Network_Strategy.isEmpty()) {
                        for(Map.Entry<String,Integer> entry:Black_Network_Strategy.entrySet()) {
                            String network_strategy = entry.getKey();
                            int network_type = entry.getValue();
                            if(network_type == 1) {//ip
                                if(network_strategy.contains("-")) {
                                    if(judgeIpSection(ip,network_strategy)) {
                                        showToast(packageName);
                                        return false;
                                    }
                                } else if(network_strategy.contains("/")) {
                                    if(judgeIpSubnet(ip,network_strategy)) {
                                        showToast(packageName);
                                        return false;
                                    }
                                } else {
                                    if(judgeIp(ip,network_strategy)) {
                                        showToast(packageName);
                                        return false;
                                    }
                                }
                            } else if(network_type == 2) {//domain name
                                if(network_strategy.contains("*")) {
                                    network_strategy = network_strategy.replace("*", "www");
                                }
                                if(judgeDomain(ip,network_strategy)) {
                                    showToast(packageName);
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                    return true;
                }
            }
        }
        return true;
    }
    private void showToast(String packagename) throws RemoteException{
        if (!packagenames.contains(packagename)) {
            showToast();
            packagenames.add(packagename);
        }
    }

    private void showToast() throws RemoteException {
        if(mToastCallback != null) {
            mToastCallback.showToast();
        }
    }

    private boolean judgeIp(String ip,String ipStreategy) {
        return ip.equals(ipStreategy);
    }

    private boolean judgeIpSection(String ip,String ipStreategy) {
        String ipSection = ipStreategy.trim();
        int idx = ipSection.indexOf("-");
        String beginIp = ipSection.substring(0,idx);
        String endIp = ipSection.substring(idx+1);
        return (getIp2long(beginIp)<=getIp2long(ip) && getIp2long(ip)<=getIp2long(endIp));
    }

    private boolean judgeIpSubnet(String ip,String ipStrategy) {
        String ipSubnet = ipStrategy.trim();
        int idx = ipSubnet.indexOf("/");
        String subnet = ipStrategy.substring(0,idx);
        int len = Integer.valueOf(subnet);
        String ip_mask = getSubnet(ip,len);
        String ipStrategy_mask = getSubnet(subnet,len);
        return ip_mask.equals(ipStrategy_mask);
    }

    private String getSubnet(String ip,int len) {
        String[] ips = ip.split("\\.");
        StringBuilder spliip = new StringBuilder();
        for(String str : ips) {
            String subip = Integer.toBinaryString(Integer.valueOf(str));
            int sub_len = subip.length();
            if(sub_len < 8) {
                for(int i = 0; i < 8 - sub_len;i++) {
                    subip = subip + '0';
                }
                spliip.append(subip);
            }
            spliip.append(subip);
        }
        return spliip.substring(0,len);
    }

    private long getIp2long(String ip) {
        String[] ips = ip.split("\\.");
        long ip2long = 0L;
        for (int i = 0; i < ips.length; ++i) {
            ip2long = ip2long << 8 | Integer.parseInt(ips[i]);
        }
        return ip2long;
    }

    private boolean judgeDomain(String ip,String domain) {
        try {
            InetAddress[] ips = InetAddress.getAllByName(domain);
            for(InetAddress inetAddress:ips) {
                if(ip.equals(inetAddress.getHostAddress()))
                {
                    return  true;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean judgeIpV6Domain(String ipv6,String domain) {
        try {
            InetAddress[] ips = InetAddress.getAllByName(domain);
            for(InetAddress inetAddress:ips) {
                if(ipv6.contains(inetAddress.getHostAddress()) || ipv6.equals(inetAddress.getHostAddress()))
                {
                    return  true;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void addNetworkStrategy(Map networkStrategy, boolean isWhiteOrBlackList) {
        VLog.d(Tag,"addNetworkStrategy isWhiteOrBlackList " + isWhiteOrBlackList + " networkStrategy " + networkStrategy);
        packagenames.clear();
        isWhiteOrBlackFlag = isWhiteOrBlackList;
        if (isWhiteOrBlackList) {
            if (networkStrategy != null) {
                White_Network_Strategy = (HashMap<String, Integer>) networkStrategy;
                mWhiteNetStrategyPersistence.save();
            }
        } else {
            if (networkStrategy != null) {
                Black_Network_Strategy = (HashMap<String, Integer>) networkStrategy;
                mBlackNetStrategyPersistence.save();
            }
        }
        mNetworkPersistence.save();
    }
}
