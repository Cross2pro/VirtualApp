//
// Created by zhangsong on 18-1-23.
//

#include "controllerManagerNative.h"
#include "zJNIEnv.h"

jclass controllerManagerNative::cmn_class = 0;
jmethodID controllerManagerNative::cmn_method = 0;
jmethodID controllerManagerNative::cmn_method2 = 0;

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

        controllerManagerNative::cmn_method = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "is_TED_Enable", "()Z");
        if(controllerManagerNative::cmn_method == NULL)
            break;

        controllerManagerNative::cmn_method2 = env.get()->GetStaticMethodID(controllerManagerNative::cmn_class, "is_FT_Enable", "()Z");
        if(controllerManagerNative::cmn_method == NULL)
            break;

        ret = true;
    }while(false);

    return ret;
}

bool controllerManagerNative::is_TED_Enable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::cmn_method);
}

bool controllerManagerNative::is_FT_Enable() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    return env.get()->CallStaticBooleanMethod(controllerManagerNative::cmn_class, controllerManagerNative::cmn_method2);
}