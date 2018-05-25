//
// Created by wxudong on 17-12-16.
//


#include <cstring>
#include <utils/mylog.h>
#include <stdlib.h>
#include <utils/zString.h>
#include <linux/time.h>
#include <time.h>

#include "safekey_jni.h"
#include "utils/zJNIEnv.h"

extern JavaVM *gVm;
extern jclass gClass;
extern jclass vskmClass;

int SafeKeyJni::encryptKey(char *input, int inputlen, char *output, int outputlen){
    /*return operatorKey(input,inputlen, output, outputlen,0);*/
    for(int i = 0; i < inputlen; i++)
        output[i] = input[i] + (char)3;

    return 0;
}

int SafeKeyJni::decryptKey(char *input, int inputlen, char *output, int outputlen){
    /*return operatorKey(input,inputlen, output, outputlen,1);*/
    for(int i = 0; i < inputlen; i++)
        output[i] = input[i] - (char)3;

    return 0;
}

int SafeKeyJni::operatorKey(char *input, int inputlen, char *output, int outputlen,int mode) {

    /*log("SafeKeyJni operatorKey start mode %d keylen %d",mode,inputlen);

    zJNIEnv env;
    if(env.get() == NULL) {
        log("JNIEnv is NULL");
        return -1;
    }

    jbyteArray _input = env.get()->NewByteArray(inputlen);
    env.get()->SetByteArrayRegion(_input, 0, inputlen, (jbyte*)input);
    jbyteArray _output = env.get()->NewByteArray(inputlen);
//    env->SetByteArrayRegion(seckey, 0, seckeylen, (jbyte*)pseckey);
    jmethodID mid = NULL;
    if(mode==0){
        mid = env.get()->GetStaticMethodID(vskmClass, "encryptKey", "([BI[BI)I");
    }else{
        mid = env.get()->GetStaticMethodID(vskmClass, "decryptKey", "([BI[BI)I");
    }

    int ret = env.get()->CallStaticIntMethod(vskmClass, mid ,_input, inputlen, _output, inputlen);
    jbyte* a = env.get()->GetByteArrayElements(_output, JNI_FALSE);
    memcpy(output, a, (size_t)inputlen);

    env.get()->ReleaseByteArrayElements(_output, a, 0);
    env.get()->DeleteLocalRef(_input);
    env.get()->DeleteLocalRef(_output);

    zString tmp;
    char * p = tmp.getBuf();
    for(int i = 0; i < inputlen; i++)
    {
        sprintf(p + i*2, "%02hhx", output[i]);
    }

    log("SafeKeyJni operatorKey end return %d [%s]", ret, p);
    return ret;*/

    return 0;
}

int SafeKeyJni::getRandom(int len, char *random) {

    int ret = 0;
    log("SafeKeyJni getRandom start keylen %d", len);
    /*zJNIEnv env;
    if(env.get() == NULL) {
        log("JNIEnv is NULL");
        return -1;
    }

    jbyteArray _output = env.get()->NewByteArray(len);
    jmethodID mid = env.get()->GetStaticMethodID(vskmClass, "getRandom", "(I[B)I");
    ret = env.get()->CallStaticIntMethod(vskmClass, mid , len, _output);
    jbyte* a = env.get()->GetByteArrayElements(_output, JNI_FALSE);
    memcpy(random, a, (size_t)len);

    env.get()->ReleaseByteArrayElements(_output, a, 0);
    env.get()->DeleteLocalRef(_output);*/

    timespec time;
    clock_gettime(CLOCK_REALTIME, &time);  //获取相对于1970到现在的秒数
    srand48(time.tv_nsec);
    lrand48();

    for(int i = 0; i < len; i++)
    {
        random[i] = 'a' + (char)(lrand48() / 26);
    }

    zString tmp;
    char * p = tmp.getBuf();
    for(int i = 0; i < len; i++)
    {
        sprintf(p + i*2, "%02hhx", random[i]);
    }

    log("SafeKeyJni getRandom end return %d [%s]", ret, p);
    return ret;
}