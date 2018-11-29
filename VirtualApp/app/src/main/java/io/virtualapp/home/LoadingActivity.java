package io.virtualapp.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.PermissionCompat;
import com.lody.virtual.remote.InstalledAppInfo;

import java.util.Locale;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.PackageAppDataStorage;
import io.virtualapp.utils.PackageUtils;
import io.virtualapp.widgets.EatBeansView;

/**
 * @author Lody
 */

public class LoadingActivity extends VActivity {

    private static final String PKG_NAME_ARGUMENT = "MODEL_ARGUMENT";
    private static final String KEY_INTENT = "KEY_INTENT";
    private static final String KEY_USER = "KEY_USER";
    private static final String TAG = LoadingActivity.class.getSimpleName();
    private final static int REQUEST_PERMISSION_CODE = 995;
    private PackageAppData appModel;
    private EatBeansView loadingView;
    private Intent preLunchIntent;
    private int preLunchUserId;

    public static boolean launch(Context context, String packageName, int userId) {
        if(VirtualCore.get().shouldRun64BitProcess(packageName)){
            if(!VirtualCore.get().is64BitEngineInstalled()){
                //need install support64
                Toast.makeText(context,
                        "need install app:" + VirtualCore.getConfig().get64bitEnginePackageName(),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        Intent intent = VirtualCore.get().getLaunchIntent(packageName, userId);
        if (intent != null) {
            Intent loadingPageIntent = new Intent(context, LoadingActivity.class);
            loadingPageIntent.putExtra(PKG_NAME_ARGUMENT, packageName);
            loadingPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            loadingPageIntent.putExtra(KEY_INTENT, intent);
            loadingPageIntent.putExtra(KEY_USER, userId);
            context.startActivity(loadingPageIntent);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        loadingView = (EatBeansView) findViewById(R.id.loading_anim);
        int userId = getIntent().getIntExtra(KEY_USER, -1);
        String pkg = getIntent().getStringExtra(PKG_NAME_ARGUMENT);
        appModel = PackageAppDataStorage.get().acquire(pkg);
        ImageView iconView = (ImageView) findViewById(R.id.app_icon);
        iconView.setImageDrawable(appModel.icon);
        TextView nameView = (TextView) findViewById(R.id.app_name);
        nameView.setText(String.format(Locale.ENGLISH, "Opening %s...", appModel.name));
        Intent intent = getIntent().getParcelableExtra(KEY_INTENT);
        if (intent == null) {
            return;
        }

        VirtualCore.get().setUiCallback(intent, mUiCallback);
        VUiKit.defer().when(() -> {
            InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(pkg, userId);
            if (info.notCopyApk) {
                PackageUtils.checkUpdate(info, info.packageName);
            }
            boolean isBit64 = VirtualCore.get().shouldRun64BitProcess(info.packageName);
            ApplicationInfo applicationInfo = info.getApplicationInfo(0);
            //check permissions with lock
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (PermissionCompat.needCheckPermission(applicationInfo.targetSdkVersion)) {
                    //check per

                    String[] permissions = VPackageManager.get().getDangrousPermissions(info.packageName);
                    Log.i(TAG, "checkPermissions:"+info.packageName);
                    if (!PermissionCompat.checkPermissions(permissions, isBit64)) {
                        //need request per
                        Log.i(TAG, "requestPermissions:"+info.packageName);
                        runOnUiThread(() -> {
                            requestPermissions(permissions, appModel.name, intent, userId);
                        });
                        return false;
                    }
                }
            }
            return true;

        }).fail((e)->{
            e.printStackTrace();
            Toast.makeText(this,
                    getString(R.string.start_app_failed, appModel.name), Toast.LENGTH_SHORT).show();
        }).done((res) -> {
            if(res) {
                VActivityManager.get().startActivity(intent, userId);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions(String[] permissions, String label, Intent intent, int userId){
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.permission_tip_title)
                .setMessage(getString(R.string.permission_tips_content, label))
                .setOnCancelListener((dlg)->{
                    LoadingActivity.this.finish();
                    Toast.makeText(this, getString(R.string.start_app_failed, label),
                            Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton(R.string.permission_tips_confirm, (dialog, which) -> {
                    //request permissions
                    preLunchIntent = intent;
                    preLunchUserId = userId;
                    requestPermissions(permissions, REQUEST_PERMISSION_CODE);
                })
                .create();
        try {
            alertDialog.show();
        } catch (Throwable ignored) {
            // BadTokenException.
            Toast.makeText(this, getString(R.string.start_app_failed, label),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(PermissionCompat.isRequestGranted(permissions, grantResults)){
            if(preLunchIntent != null) {
                VActivityManager.get().startActivity(preLunchIntent, preLunchUserId);
            }
        }else{
            runOnUiThread(()->{
                Toast.makeText(this, getString(R.string.start_app_failed, appModel.name),
                        Toast.LENGTH_SHORT).show();
            });
            finish();
        }
    }

    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) throws RemoteException {
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        loadingView.startAnim();
    }

    @Override
    protected void onPause() {
        super.onPause();
        loadingView.stopAnim();
    }
}
