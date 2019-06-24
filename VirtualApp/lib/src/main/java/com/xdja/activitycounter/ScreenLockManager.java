package com.xdja.activitycounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
/**
 * @Date 19-03-20 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class ScreenLockManager extends BaseCounterManager{

    private String TAG = "ScreenLockManager";

    private static boolean isScreenOn = false;

    final int UNLOCK = 0; // 亮屏到前台
    final int LOCK = 1; //  手机灭屏
    final int SHOW = 2; //  到前台
    final int HIDE = 3; //  到后台
    final int INCALL = 4; //来电页面到前台

    public ScreenLockManager(){

        IntentFilter slFilter = new IntentFilter();
        slFilter.addAction(Intent.ACTION_SCREEN_ON);
        slFilter.addAction(Intent.ACTION_SCREEN_OFF);
        VirtualCore.get().getContext().registerReceiver(new ScreenLockReceiver(),slFilter);
    }

    @Override
    void changeState(int mode, boolean on,String name) {
        Log.e(TAG,"isScreenOn " + on);
        Log.e(TAG,"name " + name);
        if ("com.xdja.incallui.InCallActivity".equals(name)){
            screenLock(4);
            return;
        }
        if(isScreenOn){
            screenLock(UNLOCK);
            isScreenOn = false; // 进入安全盒后清除锁屏状态
        }
        screenLock(on?SHOW:HIDE);
    }

    private void screenLock(int on) {
        Log.e(TAG,"screenlock " + on);
        if(mForegroundInterface==null){
            Log.e(TAG,"ScreenLock callback is null! ");
            return;
        }
        try {
            mForegroundInterface.screenChanged(on);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    class ScreenLockReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e(TAG,"onReceive " + intent.getAction());
           if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
               isScreenOn = false;
               screenLock(LOCK);
            }else if(Intent.ACTION_SCREEN_ON.equals(intent.getAction())){
//               screenLock(UNLOCK);
               isScreenOn = true;
           }
        }
    }
}
