package com.example.hooktest.tests.returntype;

import com.lody.hooklib.art.vposed.VposedBridge;

import com.example.hooktest.tests.LogMethodHook;
import com.example.hooktest.tests.TestCase;

/**
 * Created by weishu on 17/11/13.
 */
public class ByteType extends TestCase {

    final byte returnType = Byte.MAX_VALUE;
    final byte returnTypeModified = Byte.MAX_VALUE - 1;

    public ByteType() {
        super("Byte");
    }

    @Override
    public void test() {

        VposedBridge.findAndHookMethod(ReturnTypeTarget.class, "returnByte", byte.class, new LogMethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(returnTypeModified);
                super.beforeHookedMethod(param);
            }
        });
    }

    @Override
    public boolean predicate() {
        final byte raw = ReturnTypeTarget.returnByte(returnType);
        return raw == returnTypeModified;
    }
}
