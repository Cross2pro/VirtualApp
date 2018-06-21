package com.lody.virtual.client.ipc;

import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.remote.WaterMarkInfo;
import com.lody.virtual.server.interfaces.IWaterMark;

public class VWaterMarkManager {
    private static final VWaterMarkManager sInstance = new VWaterMarkManager();
    private IPCSingleton<IWaterMark> singleton = new IPCSingleton<>(IWaterMark.class);

    public static VWaterMarkManager get() {
        return sInstance;
    }

    public IWaterMark getService() {
        return singleton.get();
    }

    /**
     * 设置水印信息
     *
     * @param waterMark 水印信息
     */
    public void setWaterMark(WaterMarkInfo waterMark) {
        try {
            getService().setWaterMark(waterMark);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取水印信息
     *
     * @return 水印信息
     */
    public WaterMarkInfo getWaterMark() {
        try {
            return getService().getWaterMark();
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }
}
