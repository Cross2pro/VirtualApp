package com.lody.virtual.client.hook.proxies.telephony;

import android.content.Context;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.Inject;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.client.ipc.VAppPermissionManager;

import java.lang.reflect.Method;

import mirror.com.android.internal.telephony.ITelephony;

/**
 * @author Lody
 * @see android.telephony.TelephonyManager
 */
@Inject(MethodProxies.class)
public class TelephonyStub extends BinderInvocationProxy {

	public TelephonyStub() {
		super(ITelephony.Stub.asInterface, Context.TELEPHONY_SERVICE);
	}

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        //phone number
        if(VirtualCore.get().hasAnyPermission(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.READ_PHONE_NUMBERS)){
            addMethodProxy(new ReplaceLastPkgMethodProxy("getLine1NumberForDisplay"));
        }else{
            addMethodProxy(new ResultStaticMethodProxy("getLine1NumberForDisplay", null));
        }
        //fake location
        if(VirtualCore.get().hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)){
            addMethodProxy(new MethodProxies.GetCellLocation());
            addMethodProxy(new MethodProxies.GetAllCellInfoUsingSubId());
            addMethodProxy(new MethodProxies.GetAllCellInfo());
            addMethodProxy(new MethodProxies.GetNeighboringCellInfo());
        }else{
            addMethodProxy(new ResultStaticMethodProxy("getCellLocation", null));
            addMethodProxy(new ResultStaticMethodProxy("getAllCellInfoUsingSubId", null));
            addMethodProxy(new ResultStaticMethodProxy("getAllCellInfo", null));
            addMethodProxy(new ResultStaticMethodProxy("getNeighboringCellInfo", null));
        }
        if(VirtualCore.get().hasPermission(android.Manifest.permission.CALL_PHONE)){
            addMethodProxy(new ReplaceCallingPkgMethodProxy("call"));
        }else{
            addMethodProxy(new ResultStaticMethodProxy("call", 0));
            addMethodProxy(new ResultStaticMethodProxy("endCall", false));
        }

        if(VirtualCore.get().hasPermission(android.Manifest.permission.READ_PHONE_STATE)) {
            addMethodProxy(new MethodProxies.GetDeviceId());
            addMethodProxy(new MethodProxies.GetImeiForSlot());
            addMethodProxy(new MethodProxies.GetMeidForSlot());
            addMethodProxy(new ReplaceLastPkgMethodProxy("isSimPinEnabled"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriIconIndex"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriIconIndexForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getCdmaEriIconMode"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriIconModeForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getCdmaEriText"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getCdmaEriTextForSubscriber"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getNetworkTypeForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getDataNetworkType"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getDataNetworkTypeForSubscriber"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getVoiceNetworkTypeForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getLteOnCdmaMode"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getLteOnCdmaModeForSubscriber"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getCalculatedPreferredNetworkType"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getPcscfAddress"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getLine1AlphaTagForDisplay"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getMergedSubscriberIds"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getRadioAccessFamily"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("isVideoCallingEnabled"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getDeviceSoftwareVersionForSlot"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getServiceStateForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getVisualVoicemailPackageName"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("enableVisualVoicemailSmsFilter"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("disableVisualVoicemailSmsFilter"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getVisualVoicemailSmsFilterSettings"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("sendVisualVoicemailSmsForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getVoiceActivationState"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getDataActivationState"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getVoiceMailAlphaTagForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("sendDialerSpecialCode"));
            if (BuildCompat.isOreo()) {
                addMethodProxy(new ReplaceCallingPkgMethodProxy("setVoicemailVibrationEnabled"));
                addMethodProxy(new ReplaceCallingPkgMethodProxy("setVoicemailRingtoneUri"));
            }
        }else{
            addMethodProxy(new ResultStaticMethodProxy("getDeviceId", null));
            addMethodProxy(new ResultStaticMethodProxy("getImeiForSlot", null));
            addMethodProxy(new ResultStaticMethodProxy("getMeidForSlot", null));
            addMethodProxy(new ResultStaticMethodProxy("isSimPinEnabled", false));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriIconIndex", -1));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriIconIndexForSubscriber", -1));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriIconMode", -1));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriIconModeForSubscriber", -1));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriText", null));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriTextForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getNetworkTypeForSubscriber", TelephonyManager.NETWORK_TYPE_UNKNOWN));
            addMethodProxy(new ResultStaticMethodProxy("getDataNetworkType", TelephonyManager.NETWORK_TYPE_UNKNOWN));
            addMethodProxy(new ResultStaticMethodProxy("getDataNetworkTypeForSubscriber", TelephonyManager.NETWORK_TYPE_UNKNOWN));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceNetworkTypeForSubscriber", TelephonyManager.NETWORK_TYPE_UNKNOWN));
            addMethodProxy(new ResultStaticMethodProxy("getLteOnCdmaMode", -1));
            addMethodProxy(new ResultStaticMethodProxy("getLteOnCdmaModeForSubscriber", -1));
            addMethodProxy(new ResultStaticMethodProxy("getLine1AlphaTagForDisplay", null));
            addMethodProxy(new ResultStaticMethodProxy("isVideoCallingEnabled", false));
            addMethodProxy(new ResultStaticMethodProxy("getMergedSubscriberIds", null));
            addMethodProxy(new ResultStaticMethodProxy("setVoiceMailNumber", null));
            addMethodProxy(new ResultStaticMethodProxy("getVisualVoicemailPackageName", null));
            addMethodProxy(new ResultStaticMethodProxy("enableVisualVoicemailSmsFilter", null));
            addMethodProxy(new ResultStaticMethodProxy("disableVisualVoicemailSmsFilter", null));
            addMethodProxy(new ResultStaticMethodProxy("getVisualVoicemailSmsFilterSettings", null));
            addMethodProxy(new ResultStaticMethodProxy("getActiveVisualVoicemailSmsFilterSettings", null));
            addMethodProxy(new ResultStaticMethodProxy("getDeviceSoftwareVersionForSlot", null));
            addMethodProxy(new ResultStaticMethodProxy("sendVisualVoicemailSmsForSubscriber", 0));
            addMethodProxy(new ResultStaticMethodProxy("setVoiceActivationState", 0));
            addMethodProxy(new ResultStaticMethodProxy("setDataActivationState", 0));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceActivationState", 0));
            addMethodProxy(new ResultStaticMethodProxy("getDataActivationState", 0));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceMessageCountForSubscriber", 0));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceMailAlphaTagForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("sendDialerSpecialCode", 0));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriIconIndexForSubscriber", -1));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriIconModeForSubscriber", -1));
            addMethodProxy(new ResultStaticMethodProxy("getCdmaEriTextForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getForbiddenPlmns", null));
            addMethodProxy(new ResultStaticMethodProxy("getServiceStateForSubscriber", ServiceState.STATE_POWER_OFF));
            if (BuildCompat.isOreo()) {
                addMethodProxy(new ResultStaticMethodProxy("setVoicemailVibrationEnabled", 0));
                addMethodProxy(new ResultStaticMethodProxy("setVoicemailRingtoneUri", 0));
            }
        }
        if(VirtualCore.get().hasAnyPermission(
                "android.permission.READ_PRIVILEGED_PHONE_STATE",
                android.Manifest.permission.READ_PHONE_STATE)){
            addMethodProxy(new ReplaceCallingPkgMethodProxy("isOffhook"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("isOffhookForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("isRinging"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("isRingingForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("isIdle"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("isIdleForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("isRadioOn"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("isRadioOnForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getClientRequestStats"));
        }else{
            addMethodProxy(new ResultStaticMethodProxy("isOffhook", false));
            addMethodProxy(new ResultStaticMethodProxy("isOffhookForSubscriber", false));
            addMethodProxy(new ResultStaticMethodProxy("isRinging", false));
            addMethodProxy(new ResultStaticMethodProxy("isRingingForSubscriber", false));
            addMethodProxy(new ResultStaticMethodProxy("isIdle", true));
            addMethodProxy(new ResultStaticMethodProxy("isIdleForSubscriber", true));
            addMethodProxy(new ResultStaticMethodProxy("isRadioOn", false));
            addMethodProxy(new ResultStaticMethodProxy("isRadioOnForSubscriber", false));
            addMethodProxy(new ResultStaticMethodProxy("getClientRequestStats", null));
        }
        //systemApi
        if (!VirtualCore.get().isSystemApp()) {
            addMethodProxy(new ResultStaticMethodProxy("getVisualVoicemailSettings", null));
            addMethodProxy(new ResultStaticMethodProxy("setDataEnabled", 0));
            addMethodProxy(new ResultStaticMethodProxy("getDataEnabled", false));
        }

        //葛垚的拦截
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getActivePhoneTypeForSlot") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                boolean appPermissionEnable = VAppPermissionManager.get().getLocationEnable(getAppPkg());
                if (appPermissionEnable) {
                    Log.e("geyao_TelephonyRegStub", "getActivePhoneTypeForSlot return");
                    return TelephonyManager.PHONE_TYPE_NONE;
                }
                return super.call(who, method, args);
            }
        });
    }

}
