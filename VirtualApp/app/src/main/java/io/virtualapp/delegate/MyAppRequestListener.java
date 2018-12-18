package io.virtualapp.delegate;

import android.content.Context;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

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
        Context context = VirtualCore.get().getContext();
        Toast.makeText(context, "正在安装: " + path, Toast.LENGTH_SHORT).show();
        VirtualCore.get().installPackage(path, InstallStrategy.FORCE_UPDATE, res -> {
            String info;
            if (res.isSuccess) {
                if (res.isUpdate) {
                    info = "更新 " + res.packageName + " 成功!";
                } else {
                    info = "安装 " + res.packageName + " 成功!";
                }
            } else {
                info = "安装 " + res.packageName + " 失败, 原因: " + res.error;
            }
            Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
            if (res.isSuccess) {
                boolean success = VActivityManager.get().launchApp(0, res.packageName);
                if (!success) {
                    Toast.makeText(context, "启动失败", Toast.LENGTH_SHORT).show();
                }
            }
//            Intent intent = new Intent(context, InstallInnerActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra(VCommends.EXTRA_PACKAGE, res.packageName);
//            new Handler(Looper.getMainLooper()).post(() -> context.startActivity(intent));
        });
    }

    @Override
    public void onRequestUninstall(String pkg) {
        Toast.makeText(context, "Intercept uninstall request: " + pkg, Toast.LENGTH_SHORT).show();

    }
}
