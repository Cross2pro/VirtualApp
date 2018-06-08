package com.example.hooktest.tests.arguments;

/**
 * @author weishu
 * @date 17/11/14.
 */

public class ArgStatic4 extends AbsArgStaticCase {

    @Override
    protected void makeCall(long... args) {
        ArgumentTarget.arg1((int)args[0]);
    }
}
