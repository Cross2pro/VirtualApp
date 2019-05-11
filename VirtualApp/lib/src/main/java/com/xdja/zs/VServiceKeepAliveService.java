package com.xdja.zs;


import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.VLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VServiceKeepAliveService extends IServiceKeepAlive.Stub {

    private static final String TAG = "VServiceKeepAliveService";
    private static VServiceKeepAliveService sInstance;
    private static Map<String, String> mKeepAliveServiceList = new HashMap<>();
    private static HandlerThread mHandlerThread = new HandlerThread("keepAliveThread");
    private static Handler mHandler;
    private static final int UPDATE_APP_LIST = 1;
    private static final int RUN_KEEPALIVE_APP = 2;
    private static final int ACTION_DEL = 1;
    private static final int ACTION_ADD = 2;


    static {
        mKeepAliveServiceList.put("com.xdja.emm.InitService", "com.xdja.emm");
    }

    public static void systemReady() {
        sInstance = new VServiceKeepAliveService();
        mHandlerThread.start();
        mHandler = new H(mHandlerThread.getLooper());
    }

    private void sendMessage(int what, int arg, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg;
        mHandler.sendMessage(msg);
    }

    public static VServiceKeepAliveService get() {
        return sInstance;
    }


    @Override
    public void addKeepAliveServiceName(String pkgName, String serviceName) throws RemoteException {
        if (!mKeepAliveServiceList.containsKey(serviceName)) {
            mKeepAliveServiceList.put(serviceName, pkgName);
        }
    }

    @Override
    public void removeKeepAliveServiceName(String name) throws RemoteException {
        mKeepAliveServiceList.remove(name);
    }

    private boolean hasKeepAliveService(String pkgName) {
        Iterator entries = mKeepAliveServiceList.entrySet().iterator();
        if (entries.hasNext()) {
            do {
                Map.Entry entry = (Map.Entry) entries.next();
                if (entry.getValue().equals(pkgName)) {
                    return true;
                }
            } while (entries.hasNext());
        }
        return false;
    }

    private static void runKeepAliveService(String pkgName, int userId) {
        Iterator entries = mKeepAliveServiceList.entrySet().iterator();
        if (entries.hasNext()) {
            do {
                final Map.Entry entry = (Map.Entry) entries.next();
                if (entry.getValue().equals(pkgName)) {

                    Intent intent = new Intent();
                    intent.setClassName(pkgName, (String) entry.getKey());
                    VLog.d(TAG, "service:" + entry.getKey());
                    VActivityManager.get().startService(userId, intent);
                }
            } while (entries.hasNext());
        }
    }

    private static void clearAppFromList(String pkgName) {
        Iterator entries = mKeepAliveServiceList.entrySet().iterator();
        if (entries.hasNext()) {
            do {
                final Map.Entry entry = (Map.Entry) entries.next();
                if (entry.getValue().equals(pkgName)) {
                    //noinspection SuspiciousMethodCalls
                    mKeepAliveServiceList.remove(entry.getKey());
                }
            } while (entries.hasNext());
        }
    }

    @Override
    public void scheduleRunKeepAliveService(String pkgName, int userId) {
        sendMessage(RUN_KEEPALIVE_APP, userId, pkgName);
    }

    @Override
    public void scheduleUpdateKeepAliveList(String pkgName, int action) {
        sendMessage(UPDATE_APP_LIST, action, pkgName);
    }

    private static class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_APP_LIST:
                    String pkg = msg.obj.toString();
                    if (msg.arg1 == ACTION_DEL) {
                        clearAppFromList(pkg);
                        VLog.d(TAG, "Update del List:" + mKeepAliveServiceList);
                    } else if(msg.arg1 == ACTION_ADD) {
                        if (pkg.equals("com.xdja.emm")) {
                            mKeepAliveServiceList.put("com.xdja.emm.InitService", "com.xdja.emm");
                            VLog.d(TAG, "Update add List:" + mKeepAliveServiceList);
                        }
                    }
                    break;
                case RUN_KEEPALIVE_APP:
                    runKeepAliveService((String) msg.obj, msg.arg1);
                    break;
                default:
                    break;
            }
        }
    }

}
