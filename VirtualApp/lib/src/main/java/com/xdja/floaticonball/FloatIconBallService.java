package com.xdja.floaticonball;

import android.os.RemoteException;
import android.util.Log;

import com.xdja.floaticonball.IFloatIconBallCallback;
import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Date 18-11-28 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class FloatIconBallService extends IFloatIconBallService.Stub {

    private String TAG = "wechat-FloatIconBallService";
    private IFloatIconBallCallback mFIBCallback = null;

    private static final Singleton<FloatIconBallService> sService = new Singleton<FloatIconBallService>() {
        @Override
        protected FloatIconBallService create() {
            return new FloatIconBallService();
        }
    };

    public static FloatIconBallService get() {
        return sService.get();
    }
    int mCount = 0;
    boolean mShow = false;
    private Set<String> mActivitys = new HashSet<>();
    synchronized public void activityCountAdd(String name){
        mActivitys.add(name);
        mCount = mActivitys.size();
        Log.d(TAG,"mCount++ "+mCount);
        Log.d(TAG,"mActivitys "+mActivitys);
        if( mCount > 0 && !mShow)
            changeState(true);
    }
    synchronized public void activityCountReduce(String name){
        mActivitys.remove(name);
        mCount=mActivitys.size();
        Log.d(TAG,"mCount-- "+mCount + " name "+name);
        Log.d(TAG,"mActivitys "+mActivitys);
        if(mCount<=0&&mShow)
            changeState(false);
    }
    public boolean isForeGroundApp(String pkg){
        return mShow;
    }
    private void changeState(boolean show) {
        mShow = show;
        Log.d(TAG,"changeState "+show);
        if(mFIBCallback==null){
            Log.e(TAG,"Callback is null.");
            return;
        }

        if(!show){
            try {
                mFIBCallback.hideView();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else{
            try {
                mFIBCallback.showView();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void registerCallback(IFloatIconBallCallback fibCallback) throws RemoteException {
        if(fibCallback != null){
            mFIBCallback = fibCallback;
        }else {
            VLog.e(TAG, "FloatIconBallService csCallback is null, registerCallback failed");
        }
    }
    @Override
    public void unregisterCallback() throws RemoteException {
        mFIBCallback = null;
    }
}
