//
// Created by zhangsong on 18-1-23.
//

#include <cstring>
#include "controllerManagerNative.h"
#include "zJNIEnv.h"

jclass controllerManagerNative::cmn_class = 0;

jmethodID controllerManagerNative::isNetworkEnable_method = 0;
jmethodID controllerManagerNative::isCameraEnable_method = 0;
jmethodID controllerManagerNative::isChangeConnect_method = 0;
jmethodID controllerManagerNative::isGatewayEnable_method = 0;
jmethodID controllerManagerNative::isSoundRecordEnable_method = 0;
jmethodID controllerManagerNative::isIpOrNameEnable_method = 0;

bool controllerManagerNative::initial() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    bool ret = false;
    do{
        controllerManagerNative::cmn_class = env.get()->FindClass("com/xdja/zs/controllerManager");
        if(controllerManagerNative::cmn_class == NULL)
            break;

        controllerManagerNative::cmn_class = (jclass )env.get()->NewGlobalRef((jobject )controllerManagerNative::cmn_class);

        controllerManagerNative::isNetworkEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isNetworkEnable", "()Z");
        controllerManagerNative::isCameraEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isCameraEnable", "()Z");
        controllerManagerNative::isGatewayEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isGatewayEnable", "()Z");
        controllerManagerNative::isChangeConnect_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isChangeConnect", "(ILjava/lang/String;)Z");
        controllerManagerNative::isSoundRecordEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "isSoundRecordEnable", "()Z");
        controllerManagerNative::isIpOrNameEnable_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class,"isIpOrNameEnable","(Ljava/lang/String;)Z");
        if (controllerManagerNative::isNetworkEnable_method == NULL
            || controllerManagerNative::isChangeConnect_method == NULL
            || controllerManagerNative::isGatewayEnable_method == NULL
            || controllerManagerNative::isCameraEnable_method == NULL
            || controllerManagerNative::isSoundRecordEnable_method == NULL
            || controllerManagerNative::isIpOrNameEnable_method == NULL)
            break;

        ret = true;
    }while(false);

    return ret;
}

bool controllerManagerNative::isNetworkEnable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isNetworkEnable_method);
}

bool controllerManagerNative::isCameraEnable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isCameraEnable_method);
}

bool controllerManagerNative::isGatewayEnable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isGatewayEnable_method);
}

bool controllerManagerNative::isChangeConnect(int port, char *ip){
    zJNIEnv env;
    bool ret = false;
    if(env.get() == NULL)
        return false;

    jstring ips = env.get()->NewStringUTF(ip);
    ret = env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::isChangeConnect_method, port, ips);
    env.get()->DeleteLocalRef(ips);
    return ret;
}

bool controllerManagerNative::isSoundRecordEnable() {
    zJNIEnv env;
    if (env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,
                                              controllerManagerNative::isSoundRecordEnable_method);
}

bool controllerManagerNative::isIpOrNameEnable(char *ip) {
    zJNIEnv env;
    bool ret = false;
    if (env.get() == NULL) {
        return false;
    }
    jstring ipstr = env.get()->NewStringUTF(ip);
    ret = env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class,controllerManagerNative::isIpOrNameEnable_method,ipstr);
    env.get()->DeleteLocalRef(ipstr);
    return ret;
}