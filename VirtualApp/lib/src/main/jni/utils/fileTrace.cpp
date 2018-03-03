//
// Created by zhangsong on 18-1-5.
//

#include <unistd.h>
#include <sys/syscall.h>

#include "fileTrace.h"
#include "zJNIEnv.h"

jclass fileTrace::ft_class = 0;
jmethodID fileTrace::ft_method = 0;


bool fileTrace::initial() {
    zJNIEnv env;
    if(env.get() == NULL)
        return false;

    bool ret = false;
    do{
        fileTrace::ft_class = env.get()->FindClass("com/xdja/zs/fileTraceManager");
        if(fileTrace::ft_class == NULL)
            break;

        fileTrace::ft_class = (jclass)env.get()->NewGlobalRef(fileTrace::ft_class);

        fileTrace::ft_method = env.get()->GetStaticMethodID(fileTrace::ft_class, "fileTrace", "(IILjava/lang/String;Ljava/lang/String;)V");
        if(fileTrace::ft_method == NULL)
            return false;

        ret = true;
    }while(false);

    return ret;
}

void fileTrace::doFileTrace(const char *path, char *operation) {

    zJNIEnv env;
    if(env.get() == NULL)
        return ;

    jstring j_path = env.get()->NewStringUTF(path);
    jstring j_oper = env.get()->NewStringUTF(operation);

    env.get()->CallStaticVoidMethod(fileTrace::ft_class, ft_method, syscall(__NR_getpid), syscall(__NR_gettid), j_path, j_oper);

    env.get()->DeleteLocalRef(j_path);
    env.get()->DeleteLocalRef(j_oper);
}