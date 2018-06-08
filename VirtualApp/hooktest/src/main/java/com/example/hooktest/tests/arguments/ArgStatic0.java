package com.example.hooktest.tests.arguments;

import com.lody.hooklib.art.vposed.VposedBridge;

import com.example.hooktest.tests.LogMethodHook;
import com.example.hooktest.tests.TestCase;

/**
 * @author weishu
 * @date 17/11/14.
 */

public class ArgStatic0 extends TestCase {

    boolean beforeCalled = false;
    boolean afterCalled = false;
    public ArgStatic0() {
        super("ArgStatic0");
    }

    @Override
    public void test() {
        VposedBridge.findAndHookMethod(ArgumentTarget.class, "arg0", new LogMethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                beforeCalled = true;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                afterCalled = true;
            }
        });
    }

    @Override
    public boolean predicate() {

        ArgumentTarget.arg0();

        return beforeCalled && afterCalled;
    }

}
