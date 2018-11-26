package io.virtualapp.delegate;

import android.content.Context;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;

/**
 * @author Lody
 */

public class MyAppRequestListener implements VirtualCore.AppRequestListener {

    private final Context context;

    public MyAppRequestListener(Context context) {
        this.context = context;
    }

    @Override
    public void onRequestInstall(String path) {
        Toast.makeText(context, "Installing: " + path, Toast.LENGTH_SHORT).show();
        VirtualCore.get().installPackage(path, InstallStrategy.UPDATE_IF_EXIST, res -> {
            String info;
            if (res.isSuccess) {
                if (res.isUpdate) {
                    info = "Update " + res.packageName + " success!";
                } else {
                    info = "Install " + res.packageName + " success!";
                }
            } else {
                info = "Install " + res.packageName + " failed, reason: " + res.error;
            }
            Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestUninstall(String pkg) {
        Toast.makeText(context, "Intercept uninstall request: " + pkg, Toast.LENGTH_SHORT).show();

    }
}
