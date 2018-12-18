package io.virtualapp.home;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.lody.virtual.client.ipc.VActivityManager;

import io.virtualapp.VCommends;

/**
 * @author Lody
 */
public class InstallInnerActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pkg = getIntent().getStringExtra(VCommends.EXTRA_PACKAGE);
        Toast.makeText(this, "启动: " + pkg, Toast.LENGTH_SHORT).show();
        boolean success = VActivityManager.get().launchApp(0, pkg);
        if (!success) {
            Toast.makeText(this, "启动失败.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
