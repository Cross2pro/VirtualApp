package com.lody.virtual.client.stub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.compat.IntentCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.remote.IntentSenderData;
import com.lody.virtual.remote.IntentSenderExtData;

/**
 * @author Lody
 */

public class ShadowPendingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setExtrasClassLoader(IntentSenderExtData.class.getClassLoader());
        Intent finalIntent = ComponentUtils.getIntentForIntentSender(intent);
        int userId = ComponentUtils.getUserIdForIntentSender(intent);
        if (finalIntent == null || userId == -1) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("targetIntent = null");
            }
            return;
        }
        IntentSenderExtData ext = intent.getParcelableExtra("_VA_|_ext_");
        if (ext != null && ext.sender != null) {
            IntentSenderData data = VActivityManager.get().getIntentSender(ext.sender);
            Intent fillIn = ext.fillIn;
            if (fillIn != null) {
                finalIntent.fillIn(fillIn, data.flags);
            }
            int flagsMask = ext.flagsMask;
            int flagsValues = ext.flagsValues;
            flagsMask &= ~IntentCompat.IMMUTABLE_FLAGS;
            flagsValues &= flagsMask;
            finalIntent.setFlags((finalIntent.getFlags() & ~flagsMask) | flagsValues);
        }
        Intent redirectIntent = ComponentUtils.redirectBroadcastIntent(finalIntent, userId);
        context.sendBroadcast(redirectIntent);
    }
}
