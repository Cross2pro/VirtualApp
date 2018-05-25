package com.example.hooktest.tests.custom;

/**
 * Created by weishu on 17/11/6.
 */

public interface Case {
    void hook();

    boolean validate(Object... args);
}
