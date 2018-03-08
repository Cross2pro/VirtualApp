package io.virtualapp.delegate;

import android.content.Context;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.server.pm.VAppManagerService;

import java.io.IOException;

import io.virtualapp.home.HomeActivity;

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
        InstallResult res = VirtualCore.get().installPackage(path, InstallStrategy.UPDATE_IF_EXIST);
        if (res.isSuccess) {
            if (res.isUpdate) {
                VAppManagerService.get().sendUpdateBroadcast(res.packageName, VUserHandle.ALL);
                Toast.makeText(context, "Update: " + res.packageName + " success!", Toast.LENGTH_SHORT).show();
            } else {
                VAppManagerService.get().sendInstalledBroadcast(res.packageName, VUserHandle.ALL);
                Toast.makeText(context, "Install: " + res.packageName + " success!", Toast.LENGTH_SHORT).show();
                HomeActivity.goHome(context,1);
            }
        } else {
            Toast.makeText(context, "Install failed: " + res.error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestUninstall(String pkg) {
        Toast.makeText(context, "Uninstall: " + pkg, Toast.LENGTH_SHORT).show();
        boolean isSucess = VirtualCore.get().uninstallPackage(pkg);
        if(isSucess == true){
            VAppManagerService.get().sendUninstalledBroadcast(pkg, VUserHandle.ALL);
            Toast.makeText(context, "Uninstall: " + pkg + " success!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Uninstall failed: " + pkg, Toast.LENGTH_SHORT).show();
        }

    }
}
