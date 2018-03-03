//
// Created by zhangsong on 18-1-23.
//

#ifndef VIRTUALAPP_CONTROLLERMANAGERNATIVE_H
#define VIRTUALAPP_CONTROLLERMANAGERNATIVE_H


#include <jni.h>

class controllerManagerNative {
    static JavaVM * _jvm;
    static jclass cmn_class;
    static jmethodID cmn_method;
    static jmethodID cmn_method2;

public:
    static bool initial();

public:
    static bool is_TED_Enable();
    static bool is_FT_Enable();
};


#endif //VIRTUALAPP_CONTROLLERMANAGERNATIVE_H
