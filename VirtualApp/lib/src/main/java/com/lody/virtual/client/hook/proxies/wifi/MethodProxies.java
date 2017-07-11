package com.lody.virtual.client.hook.proxies.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.VLocationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author Lody
 */

class MethodProxies {

    static class GetBatchedScanResults extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getBatchedScanResults";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if(VLocationManager.get().hasVirtualLocation(getAppUserId())){
                return null;
            }
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    static class GetScanResults extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getScanResults";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if(VLocationManager.get().hasVirtualLocation(getAppUserId())){
                return new ArrayList<ScanResult>();
            }
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    static class SetWifiEnabled extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setWifiEnabled";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }
}
