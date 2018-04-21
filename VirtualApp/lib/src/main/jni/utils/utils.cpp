//
// Created by zhangsong on 18-1-22.
//

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/fcntl.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/socket.h>
#include <cstdlib>
#include "utils.h"
#include "controllerManagerNative.h"
#include "mylog.h"
#include <string.h>

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
