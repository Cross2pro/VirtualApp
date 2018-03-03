//
// Created by wxudong on 17-12-17.
//

#ifndef VIRTUALAPP_SAFEKEY_JNI_H
#define VIRTUALAPP_SAFEKEY_JNI_H

#include <jni.h>

class SafeKeyJni {

public:
    static int encryptKey(char *input, int inputlen, char *output, int outputlen);
    static int decryptKey(char *input, int inputlen, char *output, int outputlen);
    static int operatorKey(char *input, int inputlen, char *output, int outputlen,int mode);
    static int getRandom(int len, char *random);
};


#endif //VIRTUALAPP_SAFEKEY_JNI_H
