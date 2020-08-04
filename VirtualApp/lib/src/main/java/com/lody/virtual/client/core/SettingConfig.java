package com.lody.virtual.client.core;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;

import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.stub.ChooserActivity;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.client.stub.WindowPreviewActivity;
import com.lody.virtual.helper.compat.BundleCompat;

/**
 * @author Lody
 */
public abstract class SettingConfig {

    public abstract String getHostPackageName();

    public abstract String get64bitEnginePackageName();

    public String getBinderProviderAuthority() {
        return getHostPackageName() + ".virtual.service.BinderProvider";
    }

    public String get64bitHelperAuthority() {
        return get64bitEnginePackageName() + ".virtual.service.64bit_helper";
    }

    public String getProxyFileContentProviderAuthority() {
        return getHostPackageName() + ".virtual.fileprovider";
    }

    public boolean isEnableIORedirect() {
        return true;
    }

    public boolean isAllowCreateShortcut() {
        return true;
    }

    public boolean isUseRealDataDir(String packageName) {
        return false;
    }

    public boolean isUseRealLibDir(String packageName) {
        return false;
    }

    /**
     *
     * 当app请求回到桌面时调用此方法
     *
     * @return intent or null
     */
    public Intent onHandleLauncherIntent(Intent originIntent) {
        return null;
    }

    public enum AppLibConfig {
        UseRealLib,
        UseOwnLib,
    }

    public AppLibConfig getAppLibConfig(String packageName) {
        return AppLibConfig.UseOwnLib;
    }

    public boolean isAllowServiceStartForeground() {
        return true;
    }

    public boolean isEnableAppFileSystemIsolation() {
        return false;
    }

    public boolean isHideForegroundNotification() {
        return false;
    }

    public FakeWifiStatus getFakeWifiStatus() {
        return null;
    }

    public boolean IsServiceCanRestart(ServiceInfo serviceInfo){
        return false;
    }

    /**
     * 是否禁止悬浮窗
     */
    public boolean isDisableDrawOverlays(String packageName){
        return false;
    }

    /***
     * 深色模式处理
     */
    public void onDarkModeChange(boolean isDarkMode){

    }

    /**
     * 是否允许通过广播启动进程
     * 允许规则：
     * 1.userId对应的应用任意一个进程已经启动
     * 2.isAllowStartByReceiver返回true
     */
    public boolean isAllowStartByReceiver(String packageName, int userId, String action) {
        return false;
    }

    public void onFirstInstall(String packageName, boolean isClearData){

    }

    /**
     * 预留接口：定制白屏/黑屏，透明的默认显示界面
     * @param userId
     * @param info
     */
    public void startPreviewActivity(int userId, ActivityInfo info, VirtualCore.UiCallback callBack){
        WindowPreviewActivity.previewActivity(userId, info, callBack);
    }

    public boolean isForceVmSafeMode(String packageName){
        return false;
    }

    public void onPreLunchApp(){

    }

    /**
     *
     * @param intent 如果需要默认组件，就设置intent#setComponent
     * @param packageName
     * @param userId
     * @return true则提示找不到activity，false内部显示选择列表
     */
    public boolean onHandleView(Intent intent, String packageName, int userId){
        return false;
    }

    public Intent getChooserIntent(Intent orgIntent, IBinder resultTo, String resultWho, int requestCode, Bundle options, int userId){
        Bundle extras = new Bundle();
        extras.putInt(Constants.EXTRA_USER_HANDLE, userId);
        extras.putBundle(ChooserActivity.EXTRA_DATA, options);
        extras.putString(ChooserActivity.EXTRA_WHO, resultWho);
        extras.putInt(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
        if (Intent.ACTION_VIEW.equals(orgIntent.getAction()) || Intent.ACTION_GET_CONTENT.equals(orgIntent.getAction())) {
            extras.putParcelable(Intent.EXTRA_INTENT, new Intent(orgIntent).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (orgIntent.getAction() != null
                && (orgIntent.getAction().equals(ChooserActivity.ACTION) || orgIntent.getAction().equals(Intent.ACTION_CHOOSER))) {
            extras.putParcelable(Intent.EXTRA_INTENT, orgIntent.getParcelableExtra(Intent.EXTRA_INTENT));
        }
        BundleCompat.putBinder(extras, ChooserActivity.EXTRA_RESULTTO, resultTo);
        Intent intent =  new Intent();
        //如果上层需要重写ChooserActivity的界面，可以参考这个
        intent.setComponent(new ComponentName(StubManifest.PACKAGE_NAME, ChooserActivity.class.getName()));
        intent.putExtras(extras);
        intent.putExtra("_VA_CHOOSER",true);
        return intent;
    }

    public boolean isClearInvalidTask(){
        return true;
    }

    public boolean isCanShowNotification(String packageName, boolean currentSpace) {
        return false;
    }

    public static class FakeWifiStatus {

        public static String DEFAULT_BSSID = "66:55:44:33:22:11";
        public static String DEFAULT_MAC = "11:22:33:44:55:66";
        public static String DEFAULT_SSID = "VA_SSID";

        public String getSSID() {
            return DEFAULT_SSID;
        }

        public String getBSSID() {
            return DEFAULT_BSSID;
        }

        public String getMAC() {
            return DEFAULT_MAC;
        }

    }

}
