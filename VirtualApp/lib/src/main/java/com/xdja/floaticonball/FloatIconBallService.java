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
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Date 18-11-28 10
 * @Author lxf@xdja.com
 * @Descrip:
 */
public class FloatIconBallService extends IFloatIconBallService.Stub {

    public static final int ADD = 0;
    public static final int RDU = 1;
    public static final int GET = 2;
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
    private Map<String,Integer> mActivityMap = new ConcurrentHashMap<>();

    @Override
    public void activityCountAdd(String pkg) throws RemoteException {
        activityCounter(pkg,ADD);
    }

    @Override
    public void activityCountReduce(String pkg) throws RemoteException {
        activityCounter(pkg,RDU);
    }

    @Override
    public boolean isForeGroundApp(String pkg) throws RemoteException {
        return activityCounter(pkg,GET);
    }

    @Override
    public boolean isForeGround() throws RemoteException {
        return activityCounter(null,GET);
    }

    synchronized public boolean activityCounter(String pkg,int mode){
        switch(mode){
            case ADD:
            case RDU:
                count(pkg,mode);
                break;
            case GET:
                if(pkg==null){
                    return getAllCount();
                }else{
                    return getAppCount(pkg);
                }
        }
        return false;
    }
    public void count(String pkg,int mode){
        int num;
        if(mActivityMap.containsKey(pkg)){
            num = mActivityMap.get(pkg);
        }else{
            num = 0;
        }
        Log.d(TAG,"count pkg "+pkg + " mode "+mode+" num "+num);
        if(mode==ADD)
            num+=1;
        else if(mode==RDU){
            num-=1;
            num = num<0?0:num;
        }
        mActivityMap.put(pkg,num);
        int count = getFroundCount();
        Log.d(TAG,"count "+count);
        if(mode==ADD && count > 0 && !mShow)
            changeState(true);
        else if(mode==RDU && count<=0 && mShow)
            changeState(false);
    }

    private int getFroundCount(){
        Set<String> keys = mActivityMap.keySet();
        if (keys!=null){
            int count = 0;
            for (String key: keys){
                Log.e(TAG,"getCount() count "+mActivityMap.get(key)+" key "+key);
                count += mActivityMap.get(key);
            }
            return count;
        }
        return 0;
    }
    boolean getAppCount(String pkg){
        return mActivityMap.get(pkg)>0;
    }
    boolean getAllCount(){
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
