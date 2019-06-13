package com.xdja.watermark;

import android.util.Log;

public class VWaterMarkService extends IWaterMark.Stub {
    private static final String TAG = VWaterMarkService.class.getSimpleName();
    private static VWaterMarkService sInstance;
    private static String waterMarkInfo= "I";

    public static void systemReady() {
        sInstance = new VWaterMarkService();
    }

    public static VWaterMarkService get() {
        return sInstance;
    }

    public void setWaterMark(String waterMark) {
        Log.e(TAG, "set water mark");
        if (waterMark == null) {
            Log.e(TAG, "set water mark params is null return");
            return;
        }
        waterMarkInfo = waterMark;
    }

    public String getWaterMark() {
        Log.e(TAG, "get water mark: " + (waterMarkInfo == null ? "is null" : waterMarkInfo.toString()));
        return waterMarkInfo;
    }
}
