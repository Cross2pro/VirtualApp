//
// Created by zhangsong on 18-1-22.
//

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/fcntl.h>
#include <string>
#include <list>
#include <fcntl.h>
#include <dirent.h>
#include <sys/socket.h>
#include <cstdlib>
#include <string.h>

#include "transparentED/originalInterface.h"
#include "utils.h"
#include "mylog.h"
#include "controllerManagerNative.h"

static inline bool startWith(const std::string &str, const std::string &prefix) {
    return str.compare(0, prefix.length(), prefix) == 0;
}


static int my_readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    int ret = syscall(__NR_readlinkat, dirfd, pathname, buf, bufsiz);
    return ret;
}

bool getSelfProcessName(zString & name)
{
    int fd = originalInterface::original_openat(AT_FDCWD, "/proc/self/cmdline", O_RDONLY, 0);
    if(!fd)
        return false;

    int len = originalInterface::original_read(fd, name.getBuf(), name.getSize());
    if(len <= 0) {
        originalInterface::original_close(fd);
        return false;
    }

    originalInterface::original_close(fd);

    return true;
}

bool getPathFromFd(int fd, zString & path) {
    zString fd_path("/proc/self/fd/%d", fd);
    int ret = my_readlinkat(AT_FDCWD, fd_path.toString(), path.getBuf(), (size_t )path.getSize());

    if (ret < 0) {
        path.format("readlinkat fail : %s", strerror(errno));
    }

    return ret > 0;
}

const char * TED_packageVector[] =
        {
                "com.tencent.mm",
                "cn.wps.moffice"
        };

bool is_TED_Enable()
{
    static int temp_result = -1;
    zString pname;

    if(!getSelfProcessName(pname))
    {
        slog("getSelfProcessName fail !");
        return false;
    }

    if(temp_result == -1)
    {
        temp_result = 0;

        for(int i = 0; i < sizeof(TED_packageVector)/sizeof(TED_packageVector[0]); i++) {
            if (startWith(std::string(pname.toString()), std::string(TED_packageVector[i]))) {
                temp_result = 1;
                break;
            }
        }

        slog("%s is_TED_Enable %s", pname.toString(), temp_result == 1 ? "true" : "false");
    }

    return temp_result == 1;
}

const char * FT_packageVector[] =
        {
                "com.tencent.mm",
                "cn.wps.moffice"
        };

static bool is_FT_Enable()
{
    static int temp_result = -1;
    zString pname;

    if(!getSelfProcessName(pname))
    {
        slog("getSelfProcessName fail !");
        return false;
    }

    if(temp_result == -1)
    {
        temp_result = 0;

        for(int i = 0; i < sizeof(FT_packageVector)/sizeof(FT_packageVector[0]); i++) {
            if (startWith(std::string(pname.toString()), std::string(FT_packageVector[i]))) {
                temp_result = 1;
                break;
            }
        }

        slog("%s is_FT_Enable %s", pname.toString(), temp_result == 1 ? "true" : "false");
    }

    return temp_result == 1;
}

void doFileTrace(const char* path, char* operation)
{
    if(is_FT_Enable())
        slog("%s %s", path, operation);
}

const char* EncryptPathMap[] =
        {
                "/data/data/io.virtualapp/virtual/storage",
                "/data/user/0/io.virtualapp/virtual/storage/emulated"
        };

bool isEncryptPath(const char *_path) {

    bool result = false;
    for(int i = 0; i < sizeof(EncryptPathMap)/sizeof(EncryptPathMap[0]); i++)
    {
        if(startWith(std::string(_path), std::string(EncryptPathMap[i]))) {
            result = true;
            break;
        }
    }

    slog("%s isEncryptPath %s", _path, result == 1 ? "true" : "false");

    return result;
}

const char * magicPath[] = {
        "/system/magic.mgc",
};

const char * getMagicPath()
{
    for(int i = 0; i < sizeof(magicPath) / sizeof(magicPath[0]); i++)
    {
        int fd = originalInterface::original_openat(AT_FDCWD, magicPath[i], O_RDONLY, 0);
        if( fd > 0)
        {
            originalInterface::original_close(fd);

            return magicPath[i];
        }
    }

    slog("magic file not found !");

    return "unknow";
}

void getStrMidle(char* buf,char* inote){
    bool start = false;
    int a = 0;
    for(int i=0; i<30; i++){
        if(buf[i] == '['){
            start = true;
        }else if(buf[i] == ']'){
            inote[a] = '\0';
            break;
        }else{
            if(start == true){
                inote[a] = buf[i];
                a++;
            }

        }
    }
}

bool checkSocketFromTcp(char* path){

    bool ret = false;
    char dest[20]={0};

    getStrMidle(path, dest);
    //LOGE("wxd %s path %s dest %s ", __FUNCTION__, path, dest);
    zString filename("/proc/net/tcp");
    FILE *fp;
    char StrLine[1024];             //每行最大读取的字符数
    if((fp = fopen(filename.toString(), "r")) == NULL) //判断文件是否存在及可读
    {
        printf("error!");
        return ret;
    }

    while (!feof(fp))
    {
        fgets(StrLine, 1024, fp);  //读取一行
        if(strstr(StrLine, dest) != NULL){
            ret = true;
            break;
        }
    }
    fclose(fp);
    return ret;
}


bool checkSocketFromTcp6(char* path){

    bool ret = false;
    char dest[20]={0};

    getStrMidle(path, dest);
    //LOGE("wxd %s path %s dest %s ", __FUNCTION__, path, dest);
    zString filename("/proc/net/tcp6");
    FILE *fp;
    char StrLine[1024];             //每行最大读取的字符数
    if((fp = fopen(filename.toString(), "r")) == NULL) //判断文件是否存在及可读
    {
        printf("error!");
        return ret;
    }

    while (!feof(fp))
    {
        fgets(StrLine, 1024, fp);  //读取一行
        if(strstr(StrLine, dest) != NULL){
            ret = true;
            break;
        }
    }
    fclose(fp);
    return ret;
}

bool closeAllSockets(){
    bool isclose = false;
    int i = 0;
    do{
        zString *path = new zString();
        bool ret = getPathFromFd(i, *path);
        if(ret && strncmp("socket", path->toString(), 6)==0
           && checkSocketFromTcp(path->toString())){
            shutdown(i, SHUT_RDWR);
            int ret = close(i);
            LOGE("lxf %s tcp socket close fd %d ret %d", __FUNCTION__, i, ret);
            isclose = true;
        }
        if(ret && strncmp("socket", path->toString(), 6)==0
           && checkSocketFromTcp6(path->toString())){
            shutdown(i, SHUT_RDWR);
            int ret = close(i);
            LOGE("lxf %s tcp6 socket6 close fd %d ret %d", __FUNCTION__, i, ret);
            isclose = true;
        }
       ++i;
   }while (i<1024);

   return isclose;
}
