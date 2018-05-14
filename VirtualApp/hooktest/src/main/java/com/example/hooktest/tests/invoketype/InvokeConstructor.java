package com.example.hooktest.tests.invoketype;

import com.lody.hooklib.art.vposed.VposedBridge;
import com.lody.hooklib.art.vposed.VposedHelpers;

import com.example.hooktest.tests.LogMethodHook;
import com.example.hooktest.tests.TestCase;

/**
 * Created by weishu on 17/11/14.
 */

public class InvokeConstructor extends TestCase {

    boolean callBefore = false;
    boolean callAfter = false;
    public InvokeConstructor() {
        super("Constructor");
    }

    @Override
    public void test() {
        VposedBridge.hookMethod(VposedHelpers.findConstructorExact(InvokeTypeTarget.class), new LogMethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                callBefore = true;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                callAfter = true;
            }
        });
    }

    @Override
    public boolean predicate() {
        new InvokeTypeTarget();
        return callBefore && callAfter;
    }
}
