package com.example.hooktest.tests.returntype;

import android.util.Log;

import com.lody.hooklib.art.vposed.VposedBridge;
import com.lody.hooklib.art.vposed.XC_MethodHook;

import com.example.hooktest.tests.TestCase;

/**
 * Created by weishu on 17/11/13.
 */

public class VoidType extends TestCase {

    private static final String TAG = "VoidType";

    boolean callBefore = false;
    boolean callAfter = false;

    public VoidType() {
        super("无返回值");
    }

    @Override
    public void test() {
        VposedBridge.findAndHookMethod(ReturnTypeTarget.class, "returnVoid", new XC_MethodHook() {
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
        ReturnTypeTarget.returnVoid();

        Log.i(TAG, "callBefore:" + callBefore + ", callAfter:" + callAfter);
        return callBefore && callAfter;
    }

}
