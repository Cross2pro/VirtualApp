package io.virtualapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.virtualapp.utils.PackageUtils;

public class PackageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            new Thread(() -> PackageUtils.checkUpdate(packageName)).start();
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            // String packageName = intent.getData().getSchemeSpecificPart();
            // VAManager.get().uninstallPackage(packageName);
        }
    }
}