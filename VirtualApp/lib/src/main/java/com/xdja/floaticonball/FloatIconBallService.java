package com.xdja.floaticonball;

import android.os.RemoteException;
import android.util.Log;

import com.xdja.floaticonball.IFloatIconBallCallback;
import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    boolean mShow = false;
    private Map<String,Integer> mActivityMap = new HashMap<String,Integer>();
    synchronized public void activityCountAdd(String pkg){
        int num;
        if(mActivityMap.containsKey(pkg)){
            num = mActivityMap.get(pkg);
        }else{
            num = 0;
        }
        num+=1;
        mActivityMap.put(pkg,num);
        int count = getFroundCount();
        Log.d(TAG,"count++ "+count + " pkg "+pkg);
        if( count > 0 && !mShow)
            changeState(true);
    }
    synchronized public void activityCountReduce(String pkg){
        int num;
        if(mActivityMap.containsKey(pkg)){
            num = mActivityMap.get(pkg);
        }else{
            num = 0;
        }
        num = (num-=1)<0?0:num;
        mActivityMap.put(pkg,num);
        int count = getFroundCount();
        Log.d(TAG,"count-- "+count + " pkg "+pkg);
        if(count<=0&&mShow)
            changeState(false);
    }

    private int getFroundCount(){
        Set<String> keys = mActivityMap.keySet();
        if (keys!=null){
            int count = 0;
            for (String key: keys){
                Log.e(TAG,"getCount() key "+key+ " count "+mActivityMap.get(key));
                count += mActivityMap.get(key);
            }
            return count;
        }
        return 0;
    }
    public boolean isForeGroundApp(String pkg){
        return mActivityMap.get(pkg)>0;
    }
    public boolean isForeGround(){
        return getFroundCount()>0;
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
