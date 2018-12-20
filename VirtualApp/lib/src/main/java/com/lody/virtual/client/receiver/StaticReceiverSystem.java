package com.lody.virtual.client.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.ReceiverInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;

/**
 * @author Lody
 */
public class StaticReceiverSystem {

    private static final String TAG = "StaticReceiverSystem";
    private static final StaticReceiverSystem mSystem = new StaticReceiverSystem();
    /**
     * MUST < 10000.
     */
    private static final int BROADCAST_TIME_OUT = 8500;

    private Context mContext;
    private ApplicationInfo mApplicationInfo;
    private int mUserId;
    private StaticScheduler mScheduler;
    private TimeoutHandler mTimeoutHandler;
    private final Map<IBinder, BroadcastRecord> mBroadcastRecords = new HashMap<>();

    private static final class StaticScheduler extends Handler {

        StaticScheduler(Looper looper) {
            super(looper);
        }
    }

    private static final class BroadcastRecord {
        ActivityInfo receiverInfo;
        BroadcastReceiver.PendingResult pendingResult;

        BroadcastRecord(ActivityInfo receiverInfo, BroadcastReceiver.PendingResult pendingResult) {
            this.receiverInfo = receiverInfo;
            this.pendingResult = pendingResult;
        }
    }

    public void attach(String processName, Context context, ApplicationInfo appInfo, int userId) {
        if (mApplicationInfo != null) {
            throw new IllegalStateException("attached");
        }
        this.mContext = context;
        this.mApplicationInfo = appInfo;
        this.mUserId = userId;
        HandlerThread broadcastThread = new HandlerThread("BroadcastThread");
        HandlerThread anrThread = new HandlerThread("BroadcastAnrThread");
        broadcastThread.start();
        anrThread.start();
        mScheduler = new StaticScheduler(broadcastThread.getLooper());
        mTimeoutHandler = new TimeoutHandler(anrThread.getLooper());
        List<ReceiverInfo> receiverList = VPackageManager.get().getReceiverInfos(appInfo.packageName, processName, userId);
        for (ReceiverInfo receiverInfo : receiverList) {
            String componentAction = ComponentUtils.getComponentAction(receiverInfo.info);
            IntentFilter componentFilter = new IntentFilter(componentAction);
            componentFilter.addCategory("__VA__|_static_receiver_");
            mContext.registerReceiver(new StaticReceiver(receiverInfo.info), componentFilter, null, mScheduler);
            for (IntentFilter filter : receiverInfo.filters) {
                SpecialComponentList.protectIntentFilter(filter);
                filter.addCategory("__VA__|_static_receiver_");
                mContext.registerReceiver(new StaticReceiver(receiverInfo.info), filter, null, mScheduler);
            }
        }
    }

    private final class TimeoutHandler extends Handler {

        TimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            IBinder token = (IBinder) msg.obj;
            BroadcastRecord r = mBroadcastRecords.remove(token);
            if (r != null) {
                VLog.w(TAG, "Broadcast timeout, cancel to dispatch it.");
                r.pendingResult.finish();
            }
        }
    }

    public static StaticReceiverSystem get() {
        return mSystem;
    }

    private class StaticReceiver extends BroadcastReceiver {
        private ActivityInfo info;

        public StaticReceiver(ActivityInfo info) {
            this.info = info;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!VClient.get().isAppRunning()) {
                return;
            }
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY) != 0 || isInitialStickyBroadcast()) {
                return;
            }
            intent.setExtrasClassLoader(BroadcastIntentData.class.getClassLoader());
            BroadcastIntentData data = null;
            try {
                data = intent.getParcelableExtra("_VA_|_data_");
            } catch (Throwable e) {
                // ignore
            }
            if (data == null) {
                intent.setPackage(null);
                data = new BroadcastIntentData(VUserHandle.USER_ALL, intent, null);
            }
            PendingResult result = goAsync();
            if (!handleStaticBroadcast(data, info, result)) {
                result.finish();
            }
        }
    }

    private void broadcastSent(ActivityInfo receiverInfo, BroadcastReceiver.PendingResult result) {
        BroadcastRecord record = new BroadcastRecord(receiverInfo, result);
        IBinder token = mirror.android.content.BroadcastReceiver.PendingResult.mToken.get(result);
        synchronized (mBroadcastRecords) {
            mBroadcastRecords.put(token, record);
        }
        Message msg = new Message();
        msg.obj = token;
        mTimeoutHandler.sendMessageDelayed(msg, BROADCAST_TIME_OUT);
    }

    public boolean broadcastFinish(BroadcastReceiver.PendingResult result) {
        IBinder token = mirror.android.content.BroadcastReceiver.PendingResult.mToken.get(result);
        BroadcastRecord record;
        synchronized (mBroadcastRecords) {
            record = mBroadcastRecords.remove(token);
        }
        if (record == null) {
            return false;
        }
        mTimeoutHandler.removeMessages(0, token);
        record.pendingResult.finish();
        return true;
    }

    private boolean handleStaticBroadcast(BroadcastIntentData data, ActivityInfo info, BroadcastReceiver.PendingResult result) {
        if (data.targetPackage != null && !data.targetPackage.equals(info.packageName)) {
            return false;
        }
        if (data.userId != VUserHandle.USER_ALL && data.userId != mUserId) {
            return false;
        }
        ComponentName componentName = ComponentUtils.toComponentName(info);
        broadcastSent(info, result);
        VClient.get().scheduleReceiver(info.processName, componentName, data.intent, result);
        return true;
    }

}
