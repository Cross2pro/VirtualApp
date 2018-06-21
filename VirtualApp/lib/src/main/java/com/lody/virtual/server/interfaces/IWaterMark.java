package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

import com.lody.virtual.remote.WaterMarkInfo;

public interface IWaterMark extends IPCInterface {

    /**
     * 设置水印信息
     */
    void setWaterMark(WaterMarkInfo waterMark) throws RemoteException;

    /**
     * 获取水印信息
     */
    WaterMarkInfo getWaterMark() throws RemoteException;

    abstract class Stub implements IWaterMark {
        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }
    }
}
