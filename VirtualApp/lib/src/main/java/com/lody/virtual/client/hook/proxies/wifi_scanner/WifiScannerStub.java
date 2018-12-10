package com.lody.virtual.client.hook.proxies.wifi_scanner;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;

import mirror.android.os.ServiceManager;

/**
 * @author Lody
 *
 * This empty implemention of WifiScanner is workaround for run GMS.
 *
 */
public class WifiScannerStub extends BinderInvocationProxy {

    private static final String SERVICE_NAME = "wifiscanner";

    public WifiScannerStub() {
        super(new EmptyWifiScannerImpl(), SERVICE_NAME);
    }

    @Override
    public void inject() throws Throwable {
        if (ServiceManager.checkService.call(SERVICE_NAME) == null) {
            super.inject();
        }
    }
}
