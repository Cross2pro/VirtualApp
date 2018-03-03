//
// Created by zhangsong on 18-1-22.
//

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/fcntl.h>
#include <string>
#include <list>
#include "utils.h"
#include "mylog.h"

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

    return temp_result == 1;
}

static bool is_FT_Enable()
{
    static int temp_result = -1;

    return temp_result == 1;
}

void doFileTrace(const char* path, char* operation)
{
    if(is_FT_Enable())
        slog("%s %s", path, operation);
}

static inline bool startWith(const std::string &str, const std::string &prefix) {
    return str.compare(0, prefix.length(), prefix) == 0;
}

static std::list<std::string> EncryptPathMap;

bool isEncryptPath(const char *_path) {

    return false;
}

const char * getMagicPath()
{
    return "/sdcard/magic.mgc";
}


