package com.lody.virtual.client.hook.proxies.bluetooth;

import android.os.Build;
import android.text.TextUtils;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.helper.utils.marks.FakeDeviceMark;

import java.lang.reflect.Method;

import mirror.android.bluetooth.IBluetooth;

/**
 * @see android.bluetooth.BluetoothManager
 */
public class BluetoothStub extends BinderInvocationProxy {
    public static final String SERVICE_NAME = Build.VERSION.SDK_INT >= 17 ?
            "bluetooth_manager" :
            "bluetooth";

    public BluetoothStub() {
        super(IBluetooth.Stub.asInterface, SERVICE_NAME);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new GetAddress());
    }

    @FakeDeviceMark("fake MAC")
    private static class GetAddress extends ReplaceLastPkgMethodProxy {
        public GetAddress() {
            super("getAddress");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String mac = getDeviceInfo().bluetoothMac;
            if(!TextUtils.isEmpty(mac)){
                return mac;
            }
            return super.call(who, method, args);
        }
    }
}
