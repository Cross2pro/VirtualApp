// IFloatIconBallService.aidl
package com.xdja.floaticonball;
import com.xdja.floaticonball.IFloatIconBallCallback;
// Declare any non-default types here with import statements

interface IFloatIconBallService {
    void activityCountAdd(String pkg);
    void activityCountReduce(String pkg);
    boolean isForeGroundApp(String pkg);
    void registerCallback(IFloatIconBallCallback vsCallback);
    void unregisterCallback();
}
