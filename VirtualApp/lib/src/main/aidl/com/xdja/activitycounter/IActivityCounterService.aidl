// IActivityCounterService.aidl
package com.xdja.activitycounter;
import com.xdja.activitycounter.IForegroundInterface;
// Declare any non-default types here with import statements

interface IActivityCounterService {
    void activityCountAdd(String pkg,int pid);
    void activityCountReduce(String pkg,int pid);
    void cleanProcess(int pid);
    boolean isForeGroundApp(String pkg);
    boolean isForeGround();
    void registerCallback(IForegroundInterface vsCallback);
    void unregisterCallback();
}
