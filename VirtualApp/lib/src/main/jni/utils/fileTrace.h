//
// Created by zhangsong on 18-1-5.
//

#ifndef VIRTUALAPP_FILETRACE_H
#define VIRTUALAPP_FILETRACE_H


#include <jni.h>

class fileTrace {
    static jclass ft_class;
    static jmethodID ft_method;

public:
    static bool initial();
    static void doFileTrace(const char * path, char * operation);
};

#endif //VIRTUALAPP_FILETRACE_H
