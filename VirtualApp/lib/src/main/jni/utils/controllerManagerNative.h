//
// Created by zhangsong on 18-1-23.
//

#ifndef VIRTUALAPP_CONTROLLERMANAGERNATIVE_H
#define VIRTUALAPP_CONTROLLERMANAGERNATIVE_H


#include <jni.h>

class controllerManagerNative {
    static JavaVM * _jvm;
    static jclass cmn_class;
    static jmethodID isNetworkEnable_method;
    static jmethodID isCameraEnable_method;
    static jmethodID isChangeConnect_method;
    static jmethodID isGatewayEnable_method;
    static jmethodID isSoundRecordEnable_method;

public:
    static bool initial();

public:
    static bool isNetworkEnable();
    static bool isCameraEnable();
    static bool isChangeConnect(int port, char *ip);
    static bool isGatewayEnable();
    static bool isSoundRecordEnable();

};


#endif //VIRTUALAPP_CONTROLLERMANAGERNATIVE_H
