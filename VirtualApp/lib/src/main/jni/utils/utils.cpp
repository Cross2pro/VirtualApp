//
// Created by zhangsong on 18-1-22.
//

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/fcntl.h>
#include "utils.h"
#include "controllerManagerNative.h"
#include "mylog.h"
#include "fileTrace.h"

static int my_readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    int ret = syscall(__NR_readlinkat, dirfd, pathname, buf, bufsiz);
    return ret;
}

bool getPathFromFd(int fd, zString & path) {
    zString fd_path("/proc/self/fd/%d", fd);
    int ret = my_readlinkat(AT_FDCWD, fd_path.toString(), path.getBuf(), (size_t )path.getSize());

    if (ret < 0) {
        path.format("readlinkat fail : %s", strerror(errno));
    }

    return ret > 0;
}

bool is_TED_Enable()
{
    static int temp_result = -1;

    if(temp_result < 0)
        temp_result = (int)controllerManagerNative::is_TED_Enable();

    return temp_result == 1;
}

static bool is_FT_Enable()
{
    static int temp_result = -1;

    if(temp_result < 0)
        temp_result = (int)controllerManagerNative::is_FT_Enable();

    return temp_result == 1;
}

void doFileTrace(const char* path, char* operation)
{
    /*if(is_FT_Enable())
        fileTrace::doFileTrace(path, operation);*/
    slog("%s %s", path, operation);
}