package com.lody.virtual.client.hook.proxies.accessibility;

import android.content.Context;
import android.os.UserHandle;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceLastUidMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;

import java.lang.reflect.Method;

import mirror.android.view.accessibility.IAccessibilityManager;

/**
 * @author Lody
 */
public class AccessibilityManagerStub extends BinderInvocationProxy {

    public AccessibilityManagerStub() {
        super(IAccessibilityManager.Stub.asInterface, Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("addClient") {
            @Override
            public boolean beforeCall(Object who, Method method, Object... args) {
                int index = args.length - 1;
                if (index >= 0 && args[index] instanceof Integer) {
                    args[index] = 0;
                }
                return super.beforeCall(who, method, args);
            }
        });
    }
}
