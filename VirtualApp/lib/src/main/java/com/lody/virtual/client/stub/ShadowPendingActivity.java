package com.lody.virtual.client.stub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.remote.StubActivityRecord;

/**
 * @author Lody
 */

public class ShadowPendingActivity extends Activity {

    public static final String EXTRA_RESULTTO = "_va|ibinder|resultTo";
    public static final String EXTRA_RESULTWHO = "_va|string|resultWho";
    public static final String EXTRA_REQUESTCODE = "_va|int|requestCode";
    public static final String EXTRA_OPTIONS = "_va|bundle|options";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        Intent intent = getIntent();
        StubActivityRecord r = new StubActivityRecord(intent);
        if (r.intent == null) {
            return;
        }
        r.intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        Bundle extras = intent.getExtras();
        if (extras == null) {
            VActivityManager.get().startActivity(r.intent, r.userId);
            return;
        }
        IBinder resultTo = BundleCompat.getBinder(extras, EXTRA_RESULTTO);
        if (resultTo != null) {
            int requestCode = extras.getInt(EXTRA_REQUESTCODE, 0);
            String resultWho = extras.getString(EXTRA_RESULTWHO);
            Bundle options = extras.getBundle(EXTRA_OPTIONS);
            int res = VActivityManager.get().startActivity(r.intent, null, resultTo, options, resultWho, requestCode, r.userId);
            if (res != 0 && requestCode > 0) {
                VActivityManager.get().sendActivityResult(resultTo, resultWho, requestCode);
            }
        } else {
            VActivityManager.get().startActivity(r.intent, r.userId);
        }
    }
}
