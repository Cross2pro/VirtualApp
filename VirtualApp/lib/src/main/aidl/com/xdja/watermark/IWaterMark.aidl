// IWaterMark.aidl
package com.xdja.watermark;

interface IWaterMark {
    /**
         * 设置水印信息
         */
        void setWaterMark(in String waterMark);

        /**
         * 获取水印信息
         */
        String getWaterMark();
}
