package com.lody.virtual.client.stub;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Toast;

import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.server.IRequestPermissionsResult;


@TargetApi(Build.VERSION_CODES.M)
public class RequestPermissionsActivity extends Activity {
    private static final int REQUEST_PERMISSION_CODE = 996;

    public static void request(Context context, String packageName, boolean is64bit, String[] permissions, IRequestPermissionsResult callback) {
        Bundle extras = new Bundle();
        extras.putStringArray("permissions", permissions);
        extras.putString("packageName", packageName);
        BundleCompat.putBinder(extras, "callback", callback.asBinder());
        Intent intent = new Intent();
        if (is64bit) {
            intent.setClassName(VASettings.PACKAGE_NAME_64BIT, RequestPermissionsActivity.class.getName());
        } else {
            intent.setClassName(VASettings.PACKAGE_NAME, RequestPermissionsActivity.class.getName());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(extras);
        context.startActivity(intent);
    }
    private IRequestPermissionsResult mCallBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras == null){
            throw new RuntimeException("need extras");
        }
        final String packageName = extras.getString("packageName");
        final String[] permissions = extras.getStringArray("permissions");
        IBinder binder = BundleCompat.getBinder(extras, "callback");
        if(binder == null){
            throw new RuntimeException("need callback@IRequestPermissionsResult");
        }
        mCallBack = IRequestPermissionsResult.Stub.asInterface(binder);
        RequestPermissionsActivity.this.requestPermissions(permissions, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,final String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(mCallBack != null){
            try {
                final String msg = mCallBack.onResult(requestCode, permissions, grantResults);
                if(!TextUtils.isEmpty(msg)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RequestPermissionsActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        finish();
    }
}
