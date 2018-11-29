package com.lody.virtual.client.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class ANRWatchDog extends Thread {
    public static final int MESSAGE_WATCHDOG_TIME_TICK = 0;  
    public static final int ANR_TIMEOUT = 6000;
  
  
    private static int lastTimeTick = -1;  
    private static int timeTick = 0;  
  
  
    private Handler watchDogHandler = new Handler() {
        @Override  
        public void handleMessage(Message msg) {
            timeTick++;  
            timeTick = timeTick % Integer.MAX_VALUE;  
        }  
    };  
    @Override  
    public void run() {  
        while (true) {  
            watchDogHandler.sendEmptyMessage(MESSAGE_WATCHDOG_TIME_TICK);  
            try {  
                Thread.sleep(ANR_TIMEOUT);
            } catch (InterruptedException e) {  
                e.printStackTrace();  
            }  
            if (timeTick == lastTimeTick) {
                throw new ANRException();  
            } else {  
                lastTimeTick = timeTick;  
            }  
        }  
    }

    public class ANRException extends RuntimeException {
        public ANRException() {
            super("========= ANR =========");
            Thread mainThread = Looper.getMainLooper().getThread();
            setStackTrace(mainThread.getStackTrace());
        }
    }
}  