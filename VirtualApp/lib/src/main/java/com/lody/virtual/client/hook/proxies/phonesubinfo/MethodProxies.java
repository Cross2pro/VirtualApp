package com.lody.virtual.client.hook.proxies.phonesubinfo;

import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.helper.utils.marks.FakeDeviceMark;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
@SuppressWarnings("ALL")
class MethodProxies {

    @FakeDeviceMark("fake device id")
    static class GetDeviceId extends ReplaceLastPkgMethodProxy {
        public GetDeviceId() {
            super("getDeviceId");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String deviceId = getDeviceInfo().getDeviceId();
            if (!TextUtils.isEmpty(deviceId)) {
                Log.w("kk", getMethodName() + " imei=" + deviceId);
                return deviceId;
            }
            Log.w("kk", getMethodName() + " system imei");
            return super.call(who, method, args);
        }
    }

    //
    @FakeDeviceMark("fake device id")
    static class GetDeviceIdForPhone extends GetDeviceId {
        @Override
        public String getMethodName() {
            return "getDeviceIdForPhone";
        }
    }

    @FakeDeviceMark("fake device id.")
    static class GetDeviceIdForSubscriber extends GetDeviceId {

        @Override
        public String getMethodName() {
            return "getDeviceIdForSubscriber";
        }

    }

    @FakeDeviceMark("fake iccid")
    static class GetIccSerialNumber extends ReplaceLastPkgMethodProxy {
        public GetIccSerialNumber() {
            super("getIccSerialNumber");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String iccId = getDeviceInfo().getIccId();
            if (!TextUtils.isEmpty(iccId)) {
                return iccId;
            }
            return super.call(who, method, args);
        }
    }


    static class getIccSerialNumberForSubscriber extends GetIccSerialNumber {
        @Override
        public String getMethodName() {
            return "getIccSerialNumberForSubscriber";
        }
    }
}
