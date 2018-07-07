package com.lody.virtual.client.ipc;

import android.content.ClipData;
import android.content.IOnPrimaryClipChangedListener;
import android.os.RemoteException;

import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.interfaces.IAppPermissionCallback;
import com.lody.virtual.server.interfaces.IAppPermissionManager;

/**
 * Created by geyao on 2018/1/22.
 */

public class VAppPermissionManager {
    private static final String TAG = VAppPermissionManager.class.getSimpleName();
    /**
     * 禁止对此应用进行截屏or录屏
     */
    public static final String PROHIBIT_SCREEN_SHORT_RECORDER = "禁止对此应用进行截屏,录屏";
    /**
     * 禁止使用网络
     */
    public static final String PROHIBIT_NETWORK = "禁止使用网络";
    /**
     * 禁止使用摄像头
     */
    public static final String PROHIBIT_CAMERA = "禁止使用摄像头";
    /**
     * 启用应用界面水印功能
     */
    public static final String ALLOW_WATER_MARK = "启用水印功能";
    /**
     * 禁止调用蓝牙功能
     */
    public static final String PROHIBIT_BLUETOOTH = "禁止调用蓝牙功能";
    /**
     * 禁止使用录音功能
     */
    public static final String PROHIBIT_SOUND_RECORD = "禁止使用录音功能";
    /**
     * 禁止读取位置信息
     */
    public static final String PROHIBIT_LOCATION = "禁止读取位置信息";
    /**
     * 应用数据加解密
     */
    public static final String ALLOW_DATA_ENCRYPT_DECRYPT = "应用数据加解密";
    /**
     * 应用防卸载
     */
    public static final String PROHIBIT_APP_UNINSTALL = "应用防卸载";
    /**
     * 目前支持的权限集合
     */
    public static final String[] permissions = new String[]{
            PROHIBIT_SCREEN_SHORT_RECORDER,//禁止对此应用进行截屏or录屏
            PROHIBIT_NETWORK,//禁止使用网络
            PROHIBIT_CAMERA,//禁止使用摄像头
            ALLOW_WATER_MARK,//启用应用界面水印功能
            PROHIBIT_BLUETOOTH,//禁止调用蓝牙功能
            PROHIBIT_SOUND_RECORD,//禁止使用录音功能
            PROHIBIT_LOCATION,//禁止读取位置信息
            ALLOW_DATA_ENCRYPT_DECRYPT,//应用数据加解密
            PROHIBIT_APP_UNINSTALL//应用防卸载
    };
    private static final VAppPermissionManager sInstance = new VAppPermissionManager();
    private IPCSingleton<IAppPermissionManager> singleton = new IPCSingleton<>(IAppPermissionManager.class);

    public static VAppPermissionManager get() {
        return sInstance;
    }

    public IAppPermissionManager getService() {
        return singleton.get();
    }

    /**
     * 是否是支持的权限
     *
     * @param permissionName 权限名称
     * @return 是否是支持的权限
     */
    public boolean isSupportPermission(String permissionName) {
        VLog.d(TAG, "isSupportPermission permissionName: " + permissionName);
        try {
            return getService().isSupportPermission(permissionName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 是否支持加解密
     *
     * @param packageName 应用名称
     * @return 是否支持加解密 true:支持 false:不支持
     */
    public boolean isSupportEncrypt(String packageName) {
        VLog.d(TAG, "isSupportEncrypt packageName: " + packageName);
        try {
            return getService().isSupportEncrypt(packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 清除权限信息
     */
    public void clearPermissionData() {
        VLog.d(TAG, "clearPermissionData");
        try {
            getService().clearPermissionData();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 设置应用权限
     *
     * @param packageName       应用包名
     * @param appPermissionName 应用权限名称
     * @param isPermissionOpen  权限开关
     */
    public void setAppPermission(String packageName, String appPermissionName, boolean isPermissionOpen) {
        VLog.d(TAG, "setAppPermission packageName: " + packageName + " appPermissionName: " + appPermissionName + " isPermissionOpen: " + isPermissionOpen);
        try {
            getService().setAppPermission(packageName, appPermissionName, isPermissionOpen);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取应用权限开关状态
     *
     * @param packageName       应用包名
     * @param appPermissionName 应用权限名称
     * @return 权限开关状态
     */
    public boolean getAppPermissionEnable(String packageName, String appPermissionName) {
        VLog.d(TAG, "getAppPermissionEnable packageName: " + packageName + " appPermissionName: " + appPermissionName);
        try {
            boolean appPermissionEnable = getService().getAppPermissionEnable(packageName, appPermissionName);
            VLog.d(TAG, "getAppPermissionEnable result: " + appPermissionEnable);
            return appPermissionEnable;
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 注册回调监听
     *
     * @param iAppPermissionCallback 回调监听
     */
    public void registerCallback(IAppPermissionCallback iAppPermissionCallback) {
        VLog.d(TAG, "registerCallback");
        try {
            getService().registerCallback(iAppPermissionCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 接触回调监听注册
     */
    public void unregisterCallback() {
        VLog.d(TAG, "unregisterCallback");
        try {
            getService().unregisterCallback();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 权限拦截触发回调
     *
     * @param appPackageName 应用名称
     * @param permissionName 权限名称
     */
    public void interceptorTriggerCallback(String appPackageName, String permissionName) {
        VLog.d(TAG, "interceptorTriggerCallback");
        try {
            getService().interceptorTriggerCallback(appPackageName, permissionName);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 缓存剪切板信息
     *
     * @param clipData 剪切板信息
     */
    public void cacheClipData(ClipData clipData) {
        VLog.d(TAG, "cacheClipData");
        try {
            getService().cacheClipData(clipData);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 获取缓存的剪切板信息
     */
    public ClipData getClipData() {
        VLog.d(TAG, "getClipData");
        try {
            return getService().getClipData();
        } catch (RemoteException e) {
            e.printStackTrace();
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * 缓存剪切板数据改变监听
     *
     * @param listener 监听
     */
    public void cachePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
        VLog.d(TAG, "cachePrimaryClipChangedListener");
        try {
            getService().cachePrimaryClipChangedListener(listener);
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 响应剪切板数据改变监听
     */
    public void callPrimaryClipChangedListener() {
        VLog.d(TAG, "callPrimaryClipChangedListener");
        try {
            getService().callPrimaryClipChangedListener();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }

    /**
     * 移除剪切板数据改变监听
     */
    public void removePrimaryClipChangedListener() {
        VLog.d(TAG, "removePrimaryClipChangedListener");
        try {
            getService().removePrimaryClipChangedListener();
        } catch (RemoteException e) {
            e.printStackTrace();
            VirtualRuntime.crash(e);
        }
    }
}
