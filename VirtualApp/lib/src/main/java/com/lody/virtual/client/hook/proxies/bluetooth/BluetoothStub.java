package com.lody.virtual.client.hook.proxies.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.ipc.VAppPermissionManager;
import com.lody.virtual.helper.utils.marks.FakeDeviceMark;

import java.lang.reflect.Method;

import mirror.android.bluetooth.IBluetooth;

/**
 * @see android.bluetooth.BluetoothManager
 */
public class BluetoothStub extends BinderInvocationProxy {
    private static final String TAG = "Test" + BluetoothStub.class.getSimpleName();
    public static final String SERVICE_NAME = Build.VERSION.SDK_INT >= 17 ?
            "bluetooth_manager" :
            "bluetooth";

    public BluetoothStub() {
        super(IBluetooth.Stub.asInterface, SERVICE_NAME);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new BluetoothMethodProxy("isEnabled"));
        addMethodProxy(new BluetoothMethodProxy("getState"));
        addMethodProxy(new BluetoothMethodProxy("enable"));
        addMethodProxy(new BluetoothMethodProxy("enableNoAutoConnect"));
        addMethodProxy(new BluetoothMethodProxy("disable"));
        addMethodProxy(new BluetoothMethodProxy("getAddress"));
        addMethodProxy(new BluetoothMethodProxy("getUuids"));
        addMethodProxy(new BluetoothMethodProxy("setName"));
        addMethodProxy(new BluetoothMethodProxy("getName"));
        addMethodProxy(new BluetoothMethodProxy("getScanMode"));
        addMethodProxy(new BluetoothMethodProxy("setScanMode"));
        addMethodProxy(new BluetoothMethodProxy("getDiscoverableTimeout"));
        addMethodProxy(new BluetoothMethodProxy("setDiscoverableTimeout"));
        addMethodProxy(new BluetoothMethodProxy("startDiscovery"));
        addMethodProxy(new BluetoothMethodProxy("cancelDiscovery"));
        addMethodProxy(new BluetoothMethodProxy("isDiscovering"));
        addMethodProxy(new BluetoothMethodProxy("getDiscoveryEndMillis"));
        addMethodProxy(new BluetoothMethodProxy("getAdapterConnectionState"));
        addMethodProxy(new BluetoothMethodProxy("getBondedDevices"));
        addMethodProxy(new BluetoothMethodProxy("createBond"));
        addMethodProxy(new BluetoothMethodProxy("createBondOutOfBand"));
        addMethodProxy(new BluetoothMethodProxy("cancelBondProcess"));
        addMethodProxy(new BluetoothMethodProxy("removeBond"));
        addMethodProxy(new BluetoothMethodProxy("getBondState"));
        addMethodProxy(new BluetoothMethodProxy("isBondingInitiatedLocally"));
        addMethodProxy(new BluetoothMethodProxy("getSupportedProfiles"));
        addMethodProxy(new BluetoothMethodProxy("getConnectionState"));
        addMethodProxy(new BluetoothMethodProxy("getRemoteName"));
        addMethodProxy(new BluetoothMethodProxy("getRemoteType"));
        addMethodProxy(new BluetoothMethodProxy("getRemoteAlias"));
        addMethodProxy(new BluetoothMethodProxy("setRemoteAlias"));
        addMethodProxy(new BluetoothMethodProxy("getRemoteClass"));
        addMethodProxy(new BluetoothMethodProxy("getRemoteUuids"));
        addMethodProxy(new BluetoothMethodProxy("fetchRemoteUuids"));
        addMethodProxy(new BluetoothMethodProxy("sdpSearch"));
        addMethodProxy(new BluetoothMethodProxy("setPin"));
        addMethodProxy(new BluetoothMethodProxy("setPasskey"));
        addMethodProxy(new BluetoothMethodProxy("setPairingConfirmation"));
        addMethodProxy(new BluetoothMethodProxy("getPhonebookAccessPermission"));
        addMethodProxy(new BluetoothMethodProxy("setPhonebookAccessPermission"));
        addMethodProxy(new BluetoothMethodProxy("getMessageAccessPermission"));
        addMethodProxy(new BluetoothMethodProxy("setMessageAccessPermission"));
        addMethodProxy(new BluetoothMethodProxy("getSimAccessPermission"));
        addMethodProxy(new BluetoothMethodProxy("setSimAccessPermission"));
        addMethodProxy(new BluetoothMethodProxy("sendConnectionStateChange"));
        addMethodProxy(new BluetoothMethodProxy("registerCallback"));
        addMethodProxy(new BluetoothMethodProxy("unregisterCallback"));
        addMethodProxy(new BluetoothMethodProxy("connectSocket"));
        addMethodProxy(new BluetoothMethodProxy("createSocketChannel"));
        addMethodProxy(new BluetoothMethodProxy("factoryReset"));
        addMethodProxy(new BluetoothMethodProxy("isMultiAdvertisementSupported"));
        addMethodProxy(new BluetoothMethodProxy("isOffloadedFilteringSupported"));
        addMethodProxy(new BluetoothMethodProxy("isOffloadedScanBatchingSupported"));
        addMethodProxy(new BluetoothMethodProxy("isActivityAndEnergyReportingSupported"));
        addMethodProxy(new BluetoothMethodProxy("isLe2MPhySupported"));
        addMethodProxy(new BluetoothMethodProxy("isLeCodedPhySupported"));
        addMethodProxy(new BluetoothMethodProxy("isLeExtendedAdvertisingSupported"));
        addMethodProxy(new BluetoothMethodProxy("isLePeriodicAdvertisingSupported"));
        addMethodProxy(new BluetoothMethodProxy("getLeMaximumAdvertisingDataLength"));
        addMethodProxy(new BluetoothMethodProxy("reportActivityInfo"));
        addMethodProxy(new BluetoothMethodProxy("requestActivityInfo"));
        addMethodProxy(new BluetoothMethodProxy("onLeServiceUp"));
        addMethodProxy(new BluetoothMethodProxy("onBrEdrDown"));
    }

    @FakeDeviceMark("fake MAC")
    private static class BluetoothMethodProxy extends ReplaceLastPkgMethodProxy {
        public BluetoothMethodProxy(String name) {
            super(name);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String methodName = getMethodName();
            Log.e(TAG, methodName + " calling");
            boolean bluetoothEnable = VAppPermissionManager.get().getAppPermissionEnable(getAppPkg(), VAppPermissionManager.PROHIBIT_BLUETOOTH);
            switch (methodName) {
                case "isEnabled":
                case "enable":
                case "enableNoAutoConnect":
                case "disable":
                case "setName":
                case "setScanMode":
                case "setDiscoverableTimeout":
                case "startDiscovery":
                case "cancelDiscovery":
                case "isDiscovering":
                case "createBond":
                case "createBondOutOfBand":
                case "cancelBondProcess":
                case "removeBond":
                case "isBondingInitiatedLocally":
                case "setRemoteAlias":
                case "fetchRemoteUuids":
                case "sdpSearch":
                case "setPin":
                case "setPasskey":
                case "setPairingConfirmation":
                case "setPhonebookAccessPermission":
                case "setMessageAccessPermission":
                case "setSimAccessPermission":
                case "factoryReset":
                case "isMultiAdvertisementSupported":
                case "isOffloadedFilteringSupported":
                case "isOffloadedScanBatchingSupported":
                case "isActivityAndEnergyReportingSupported":
                case "isLe2MPhySupported":
                case "isLeCodedPhySupported":
                case "isLeExtendedAdvertisingSupported":
                case " isLePeriodicAdvertisingSupported":
                    return bluetoothEnable ? false : super.call(who, method, args);
                case "getState":
                    return bluetoothEnable ? BluetoothAdapter.STATE_OFF : super.call(who, method, args);
                case "getAddress":
                case "getName":
                case "getRemoteName":
                case "getRemoteAlias":
                    return bluetoothEnable ? "" : super.call(who, method, args);
                case "getDiscoverableTimeout":
                case "getDiscoveryEndMillis":
                case "getBondState":
                case "getSupportedProfiles":
                case "getRemoteType":
                case "getRemoteClass":
                case "getPhonebookAccessPermission":
                case "getMessageAccessPermission":
                case "getSimAccessPermission":
                    return bluetoothEnable ? -1 : super.call(who, method, args);
                case "getLeMaximumAdvertisingDataLength":
                    return bluetoothEnable ? 0 : super.call(who, method, args);
                case "getAdapterConnectionState":
                case "getProfileConnectionState":
                case "getConnectionState":
                    return bluetoothEnable ? BluetoothAdapter.STATE_DISCONNECTED : super.call(who, method, args);
                case "getScanMode":
                    return bluetoothEnable ? BluetoothAdapter.SCAN_MODE_NONE : super.call(who, method, args);
                case "getUuids":
                case "getBondedDevices":
                case "getRemoteUuids":
                case "sendConnectionStateChange":
                case "registerCallback":
                case "unregisterCallback":
                case "connectSocket":
                case "createSocketChannel":
                case "reportActivityInfo":
                case "requestActivityInfo":
                case "onLeServiceUp":
                case "onBrEdrDown":
                default:
                    return bluetoothEnable ? null : super.call(who, method, args);
            }
        }
    }
}
