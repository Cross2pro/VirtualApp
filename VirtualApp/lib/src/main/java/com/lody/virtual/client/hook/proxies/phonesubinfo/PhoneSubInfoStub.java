package com.lody.virtual.client.hook.proxies.phonesubinfo;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.Inject;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;

import mirror.com.android.internal.telephony.IPhoneSubInfo;

/**
 * @author Lody
 * @see android.telephony.TelephonyManager
 */
@Inject(MethodProxies.class)
public class PhoneSubInfoStub extends BinderInvocationProxy {
	public PhoneSubInfoStub() {
		super(IPhoneSubInfo.Stub.asInterface, "iphonesubinfo");
	}

	@Override
	protected void onBindMethods() {
		super.onBindMethods();
        if(VirtualCore.get().hasPermission(android.Manifest.permission.READ_PHONE_STATE)) {
            addMethodProxy(new ReplaceLastPkgMethodProxy("getNaiForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getDeviceSvn"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getDeviceSvnUsingSubId"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getSubscriberId"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getSubscriberIdForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getGroupIdLevel1"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getGroupIdLevel1ForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getLine1AlphaTag"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getLine1AlphaTagForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getMsisdn"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getMsisdnForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getVoiceMailNumber"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getVoiceMailNumberForSubscriber"));
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getVoiceMailAlphaTag"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getVoiceMailAlphaTagForSubscriber"));
        }else {
            addMethodProxy(new ResultStaticMethodProxy("getNaiForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getDeviceSvn", null));
            addMethodProxy(new ResultStaticMethodProxy("getDeviceSvnUsingSubId", null));
            addMethodProxy(new ResultStaticMethodProxy("getSubscriberId", null));
            addMethodProxy(new ResultStaticMethodProxy("getSubscriberIdForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getGroupIdLevel1", null));
            addMethodProxy(new ResultStaticMethodProxy("getGroupIdLevel1ForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getLine1AlphaTag", null));
            addMethodProxy(new ResultStaticMethodProxy("getLine1AlphaTagForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getMsisdn", null));
            addMethodProxy(new ResultStaticMethodProxy("getMsisdnForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceMailNumber", null));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceMailNumberForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getCompleteVoiceMailNumberForSubscriber", null));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceMailAlphaTag", null));
            addMethodProxy(new ResultStaticMethodProxy("getVoiceMailAlphaTagForSubscriber", null));
        }
        if(VirtualCore.get().hasAnyPermission(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.READ_PHONE_NUMBERS)){
            addMethodProxy(new ReplaceCallingPkgMethodProxy("getLine1Number"));
            addMethodProxy(new ReplaceLastPkgMethodProxy("getLine1NumberForSubscriber"));
        }else{
            addMethodProxy(new ResultStaticMethodProxy("getLine1Number", null));
            addMethodProxy(new ResultStaticMethodProxy("getLine1NumberForSubscriber", null));
        }
	}

}
