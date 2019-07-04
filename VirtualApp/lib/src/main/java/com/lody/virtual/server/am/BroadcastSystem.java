package com.lody.virtual.server.am;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BroadcastIntentData;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.server.pm.PackageSetting;
import com.lody.virtual.server.pm.VAppManagerService;
import com.lody.virtual.server.pm.parser.VPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mirror.android.app.ContextImpl;
import mirror.android.app.LoadedApkHuaWei;
import mirror.android.rms.HwSysResImplP;
import mirror.android.rms.resource.ReceiverResourceLP;
import mirror.android.rms.resource.ReceiverResourceM;
import mirror.android.rms.resource.ReceiverResourceN;
import mirror.android.rms.resource.ReceiverResourceO;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;

/**
 * @author Lody
 */

public class BroadcastSystem {

    private static final String TAG = BroadcastSystem.class.getSimpleName();
    /**
     * MUST < 10000.
     */
    private static final int BROADCAST_TIME_OUT = 9000;
    private static BroadcastSystem gDefault;

    private final Map<String, Boolean> mReceiverStatus = new ArrayMap<>();
    private final ArrayMap<String, List<StaticBroadcastReceiver>> mReceivers = new ArrayMap<>();
    private final Map<Key, BroadcastRecord> mBroadcastRecords = new HashMap<>();
    private final Context mContext;
    private final StaticScheduler mScheduler;
    private final TimeoutHandler mTimeoutHandler;
    private final VActivityManagerService mAMS;
    private final VAppManagerService mApp;

    private BroadcastSystem(Context context, VActivityManagerService ams, VAppManagerService app) {
        this.mContext = context;
        this.mApp = app;
        this.mAMS = ams;
        mScheduler = new StaticScheduler();
        mTimeoutHandler = new TimeoutHandler();
        fuckHuaWeiVerifier();
    }

    /**
     * FIX ISSUE #171:
     * java.lang.AssertionError: Register too many Broadcast Receivers
     * at android.app.LoadedApk.checkRecevierRegisteredLeakLocked(LoadedApk.java:772)
     * at android.app.LoadedApk.getReceiverDispatcher(LoadedApk.java:800)
     * at android.app.ContextImpl.registerReceiverInternal(ContextImpl.java:1329)
     * at android.app.ContextImpl.registerReceiver(ContextImpl.java:1309)
     * at com.lody.virtual.server.am.BroadcastSystem.startApp(BroadcastSystem.java:54)
     * at com.lody.virtual.server.pm.VAppManagerService.install(VAppManagerService.java:193)
     * at com.lody.virtual.server.pm.VAppManagerService.preloadAllApps(VAppManagerService.java:98)
     * at com.lody.virtual.server.pm.VAppManagerService.systemReady(VAppManagerService.java:70)
     * at com.lody.virtual.server.BinderProvider.onCreate(BinderProvider.java:42)
     */
    private void fuckHuaWeiVerifier() {
        if (LoadedApkHuaWei.mReceiverResource != null) {
            Object packageInfo = ContextImpl.mPackageInfo.get(mContext);
            if (packageInfo != null) {
                Object receiverResource = LoadedApkHuaWei.mReceiverResource.get(packageInfo);
                if (receiverResource != null) {
                    if (BuildCompat.isPie()) {
                        //AMS进程判断, 非白名单每进程最多1000个receiver对象
                        //最差情况，一个月应用100个静态广播接收者，va里面能装10个这样的，多开同一个应用还是按一个计算
                        if (HwSysResImplP.mWhiteListMap != null) {
                            Map<Integer, ArrayList<String>> whiteMap = HwSysResImplP.mWhiteListMap.get(receiverResource);
                            ArrayList<String> list = whiteMap.get(0);
                            if (null == list) {
                                list = new ArrayList<>();
                                whiteMap.put(0, list);
                            }
                            list.add(mContext.getPackageName());
                        }
                    } else if (BuildCompat.isOreo()) {
                        if (ReceiverResourceO.mWhiteListMap != null) {
                            Map<Integer, List<String>> whiteMap = ReceiverResourceO.mWhiteListMap.get(receiverResource);
                            List<String> list = whiteMap.get(0);
                            if (null == list) {
                                list = new ArrayList<>();
                                whiteMap.put(0, list);

                            }
                            list.add(mContext.getPackageName());
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (ReceiverResourceN.mWhiteList != null) {
                            List<String> whiteList = ReceiverResourceN.mWhiteList.get(receiverResource);
                            List<String> newWhiteList = new ArrayList<>();
                            // Add our package name to the white list.
                            newWhiteList.add(mContext.getPackageName());
                            if (whiteList != null) {
                                newWhiteList.addAll(whiteList);
                            }
                            ReceiverResourceN.mWhiteList.set(receiverResource, newWhiteList);
                        }
                    } else {
                        if (ReceiverResourceM.mWhiteList != null) {
                            String[] whiteList = ReceiverResourceM.mWhiteList.get(receiverResource);
                            List<String> newWhiteList = new LinkedList<>();
                            Collections.addAll(newWhiteList, whiteList);
                            // Add our package name to the white list.
                            newWhiteList.add(mContext.getPackageName());
                            ReceiverResourceM.mWhiteList.set(receiverResource, newWhiteList.toArray(new String[newWhiteList.size()]));
                        } else if (ReceiverResourceLP.mResourceConfig != null) {
                            // Just clear the ResourceConfig.
                            ReceiverResourceLP.mResourceConfig.set(receiverResource, null);
                        }
                    }
                }
            }
        }
    }

    public static void attach(VActivityManagerService ams, VAppManagerService app) {
        if (gDefault != null) {
            throw new IllegalStateException();
        }
        gDefault = new BroadcastSystem(VirtualCore.get().getContext(), ams, app);
    }

    public static BroadcastSystem get() {
        return gDefault;
    }

    public void startApp(VPackage p) {
        Boolean status;
        synchronized (mReceiverStatus) {
            status = mReceiverStatus.get(p.packageName);
        }
        if(status != null){
            stopApp(p.packageName);
        }
        synchronized (mReceiverStatus) {
            mReceiverStatus.put(p.packageName, true);
        }
        VLog.d(TAG, "startApp:%s,version=%s/%d", p.packageName, p.mVersionName, p.mVersionCode);
        PackageSetting setting = (PackageSetting) p.mExtras;
        //微信有60多个静态receiver,华为低版本是每进程500个receiver对象，高版本是每进程1000个对象
        List<StaticBroadcastReceiver> receivers = mReceivers.get(p.packageName);
        if (receivers == null) {
            receivers = new ArrayList<>();
            mReceivers.put(p.packageName, receivers);
        }
        for (VPackage.ActivityComponent receiver : p.receivers) {
            ActivityInfo info = receiver.info;
            String componentAction = ComponentUtils.getComponentAction(info);
            IntentFilter componentFilter = new IntentFilter(componentAction);
            StaticBroadcastReceiver r = new StaticBroadcastReceiver(setting.appId, info);
            mContext.registerReceiver(r, componentFilter, null, mScheduler);
            receivers.add(r);
            Log.v("kk", "registerReceiver:" + info.name + ",action=" + componentAction);
            for (VPackage.ActivityIntentInfo ci : receiver.intents) {
                IntentFilter cloneFilter = new IntentFilter(ci.filter);
                SpecialComponentList.protectIntentFilter(cloneFilter);
                StaticBroadcastReceiver r2 = new StaticBroadcastReceiver(setting.appId, info);
                mContext.registerReceiver(r2, cloneFilter, null, mScheduler);
                receivers.add(r2);
            }
        }
    }

    public void stopApp(String packageName) {
        Boolean status;
        synchronized (mReceiverStatus) {
            status = mReceiverStatus.remove(packageName);
        }
        if(status == null || !status){
            return;
        }
        synchronized (mBroadcastRecords) {
            Iterator<Map.Entry<Key, BroadcastRecord>> iterator = mBroadcastRecords.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Key, BroadcastRecord> entry = iterator.next();
                BroadcastRecord record = entry.getValue();
                if (record.receiverInfo.packageName.equals(packageName)) {
                    record.pendingResult.finish();
                    iterator.remove();
                }
            }
        }
        synchronized (mReceivers) {
            List<StaticBroadcastReceiver> receivers = mReceivers.get(packageName);
            if (receivers != null) {
                for (int i = receivers.size() - 1; i >= 0; i--) {
                    StaticBroadcastReceiver r = receivers.get(i);
                    try {
                        mContext.unregisterReceiver(r);
                    }catch (Throwable e){
                        //ignore
                    }
                    receivers.remove(r);
                }
            }
            mReceivers.remove(packageName);
        }
    }

    void broadcastFinish(PendingResultData res, int userId) {
        synchronized (mBroadcastRecords) {
            BroadcastRecord record = mBroadcastRecords.remove(new Key(res.mToken, userId));
            if (record == null) {
                VLog.e(TAG, "Unable to find the BroadcastRecord by token: %s@%d", res.mToken, userId);
            } else {
                VLog.v(TAG, "broadcastFinish token: %s", res.mToken);
            }
        }
        mTimeoutHandler.removeMessages(BROADCAST_TIME_OUT);
        res.finish();
    }

    void broadcastSent(int vuid, ActivityInfo receiverInfo, PendingResultData res) {
        int userId = VUserHandle.getUserId(vuid);
        VLog.v(TAG, "broadcastSent token: %s@%d", res.mToken, userId);
        BroadcastRecord record = new BroadcastRecord(vuid, receiverInfo, res);
        synchronized (mBroadcastRecords) {
            mBroadcastRecords.put(new Key(res.mToken, userId), record);
        }
        Message msg = new Message();
        msg.obj = res.mToken;
        msg.arg1 = userId;
        mTimeoutHandler.sendMessageDelayed(msg, BROADCAST_TIME_OUT);
    }

    private static final class StaticScheduler extends Handler {

    }

    private static final class BroadcastRecord {
        int vuid;
        ActivityInfo receiverInfo;
        PendingResultData pendingResult;

        BroadcastRecord(int vuid, ActivityInfo receiverInfo, PendingResultData pendingResult) {
            this.vuid = vuid;
            this.receiverInfo = receiverInfo;
            this.pendingResult = pendingResult;
        }
    }

    private final class Key {
        private IBinder token;
        private int userId;

        Key(IBinder token, int userId) {
            this.token = token;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return userId == key.userId && ((token == key.token) || (token != null && token.equals(key.token)));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{token, userId});
        }
    }

    private final class TimeoutHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            IBinder token = (IBinder) msg.obj;
            int userId = msg.arg1;
            BroadcastRecord r = mBroadcastRecords.remove(new Key(token, userId));
            if (r != null) {
                VLog.w(TAG, "Broadcast timeout, cancel to dispatch it %s@%d", token, userId);
                r.pendingResult.finish();
            }
        }
    }

    private final class StaticBroadcastReceiver extends BroadcastReceiver {
        private int appId;
        private ActivityInfo info;
        private ComponentName componentName;

        private StaticBroadcastReceiver(int appId, ActivityInfo info) {
            this.appId = appId;
            this.info = info;
            this.componentName = ComponentUtils.toComponentName(info);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mApp.isBooting()) {
                return;
            }
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY) != 0 || isInitialStickyBroadcast()) {
                VLog.w(TAG, "StaticBroadcastReceiver:ignore by FLAG_RECEIVER_REGISTERED_ONLY:%s", intent.getAction());
                return;
            }
            String targetPackage = intent.getStringExtra("_VA_|_privilege_pkg_");
            if(!TextUtils.isEmpty(targetPackage) && !TextUtils.equals(info.packageName, targetPackage)){
                VLog.w(TAG, "StaticBroadcastReceiver:ignore by targetPackage:%s", intent.getAction());
                return;
            }
            BroadcastIntentData data = null;
            if (intent.hasExtra("_VA_|_data_")) {
                intent.setExtrasClassLoader(BroadcastIntentData.class.getClassLoader());
                try {
                    data = intent.getParcelableExtra("_VA_|_data_");
                } catch (Throwable e) {
                    // ignore
                }
            }
            if (data == null) {
                //系统
                intent.setPackage(null);
                data = new BroadcastIntentData(VUserHandle.USER_ALL, intent, null, true);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.O) {
                //8.0的静态广播限制，需要setComponent
                //非系统发送的广播
                if (data.fromSystem
                        || info.packageName.equals(data.intent.getPackage())
                        || componentName.equals(data.intent.getComponent())) {
                    //允许
                } else {
                    //不响应
                    VLog.d(TAG, "StaticBroadcastReceiver:component is null. %s", data.intent.getAction());
                    return;
                }
            }
            VLog.d(TAG, "StaticBroadcastReceiver:onReceive:%s", data.intent.getAction());
            mAMS.scheduleStaticBroadcast(data, this.appId, info, goAsync());
        }
    }

}