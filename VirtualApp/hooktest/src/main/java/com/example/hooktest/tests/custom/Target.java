package com.example.hooktest.tests.custom;

import android.util.Log;


/**
 * Created by weishu on 17/11/3.
 */

public class Target {

    public Target() {
    }

    public int test1(Object a, int b) {
        Log.i("mylog", "test1, arg1: " + a + " , arg2:" + b);

        return a.hashCode() + b;
    }

    public int test2(int a, int b) {
        return a * b + b * b;
    }

    public int test3(Object a, int b) {
        Log.i("mylog", "test1, arg1: " + a + " , arg2:" + b);
        return a.hashCode() + b;
    }

    public static int test4(int a) {
        return Integer.valueOf(a).hashCode();
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static float add(int a, float b) {
        return a + b;
    }

    public static int test2(Object a, int b) {
        Log.i("mylog", "test1, arg1: " + a + " , arg2:" + b);

        return a.hashCode() + b;
    }

    public static void validate() {

    }
}
