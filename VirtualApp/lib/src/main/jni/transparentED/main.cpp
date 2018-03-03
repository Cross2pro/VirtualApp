//
// Created by zhangsong on 17-11-13.
//

#include <stdio.h>

#include <sys/types.h>
#include <unistd.h>
#include <errno.h>


#include "originalInterface.h"
#include "fileCoder1.h"
#include "virtualFileSystem.h"
#include "../utils/mylog.h"

#define HOOK

#ifdef HOOK
int hook_open(char * path, int flags, mode_t mods = 0)
{
    int fd;
    fd = open(path, flags, mods);

    if(fd > 0) {
        virtualFileDescribe *vfd = new virtualFileDescribe();
        virtualFileDescribeSet::getVFDSet().set(fd, vfd);

        virtualFile *vf = virtualFileManager::getVFM().getVF(fd, path);
        if (vf != NULL) {
            LOGE("judge : open vf [PATH %s] [VFS %d] [FD %d]", vf->getPath(), vf->getVFS(), fd);
            vfd->_vf = vf;
            vf->vlseek(fd, 0, SEEK_SET);
        } else {
            virtualFileDescribeSet::getVFDSet().set(fd, 0);
            delete vfd;
        }
    }

    return fd;
}

int hook_close(int fd)
{
    int ret;
    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != NULL)
    {
        LOGE("judge : close [%s][__fd %d]", vfd->_vf->getPath(), fd);
        virtualFileManager::getVFM().releaseVF(vfd->_vf->getPath());
        delete vfd;
        virtualFileDescribeSet::getVFDSet().set(fd, 0);
    }
    else {
    }

    ret = close(fd);
    return ret;
}

ssize_t hook_write(int fd, const void* buf, size_t count)
{
    ssize_t ret;

    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != NULL)
    {
        LOGE("judge : write [%s] %ld", vfd->_vf->getPath(), count);
        ret = vfd->_vf->vwrite(fd, (char *)buf, count);
    }
    else
        ret = write(fd, buf, count);

    return ret;
}

ssize_t hook_read(int fd, void *buf, size_t count)
{
    ssize_t ret;

    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != NULL)
    {
        ret = vfd->_vf->vread(fd, (char *)buf, count);
        LOGE("judge : read [%s] [from %d] [len %d] [ret %d]", vfd->_vf->getPath(), vfd->_vf->vlseek(fd, 0, SEEK_CUR), count, ret);
    }
    else {
        ret = read(fd, buf, count);
    }

    return ret;
}

ssize_t hook_pread64(int fd, void* buf, size_t count, off64_t offset)
{
    ssize_t ret;

    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != NULL)
    {
        LOGE("judge : pread64 [%s] %ld, %ld", vfd->_vf->getPath(), offset, count);
        ret = vfd->_vf->vpread64(fd, (char *)buf, count, offset);
    }
    else {
        ret = pread64(fd, buf, count, offset);
    }

    return ret;
}

ssize_t hook_pwrite64(int fd, const void *buf, size_t count, off64_t offset)
{
    ssize_t ret;

    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != NULL)
    {
        LOGE("judge : pwrite64 [%s] %ld, %ld", vfd->_vf->getPath(), offset, count);
        ret = vfd->_vf->vpwrite64(fd, (char *)buf, count, offset);
    }
    else
        ret = pwrite64(fd, buf, count, offset);

    return ret;
}

int hook_fstat(int fd, struct stat *buf)
{
    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != 0)
    {
        LOGE("judge : fstat [%s] [%d]", vfd->_vf->getPath(), fd);
        return vfd->_vf->vfstat(fd, buf);
    }
    else
        return fstat(fd, buf);
}

off_t hook_lseek(int fd, off_t offset, int whence)
{
    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != 0)
    {
        LOGE("judge : lseek [%s] [%d] [offset %d] [whence %d]",
             vfd->_vf->getPath(), fd, offset, whence);
        return vfd->_vf->vlseek(fd, offset, whence);
    }
    else
        return lseek(fd, offset, whence);
}

int _llseek(unsigned int fd, unsigned long offset_high, unsigned long offset_low, off64_t *result, unsigned int whence)
{
    log("UNREACHABLE");
    log("UNREACHABLE");
    log("UNREACHABLE");

    return -1;
}
int hook__llseek(unsigned int fd, unsigned long offset_high, unsigned long offset_low, off64_t *result, unsigned int whence)
{
    off64_t rel_offset = 0;

    rel_offset |= offset_high;
    rel_offset << 32;
    rel_offset |= offset_low;

    int ret;

    virtualFileDescribe * vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd != 0)
    {
        off_t ori_cursor = vfd->_vf->vlseek(fd, 0, SEEK_CUR);
        vfd->_vf->vllseek(fd, offset_high, offset_low, result, whence);
        LOGE("judge : __llseek [%s][offset %lld] [llseek %ld - %lld] [whence %d]",
             vfd->_vf->getPath(), rel_offset, ori_cursor, *result, whence);
    } else {
        ret = _llseek(fd, offset_high, offset_low, result, whence);
    }

    return ret;
}
#else
#define hook_open open
#define hook_close close
#define hook_pread64 pread64
#define hook_pwrite64 pwrite64
#define hook_read read
#define hook_write write
#endif

#define THREAD_COUNT 5
int g_total_size = 200;
int g_block_size = 89;

char g_path[260] = {0};
char g_path2[260] = {0};

void * threadrutine(void * args)
{
    /*sleep(1);*/
    int idx = (int)args;
    int total_count = g_total_size / g_block_size;
    int tail = (g_total_size % g_block_size != 0) ? 1 : 0;


    log("** total size %d, total count %d %d", g_total_size, total_count, tail);
    total_count += tail;

    int r_fd = hook_open(g_path, O_RDWR);
    if(r_fd <= 0)
    {
        log("open r_fd fail ! %s", strerror(errno));
        return 0;
    }

    int w_fd = hook_open(g_path2, O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if(w_fd <= 0)
    {
        log("open w_fd fail ! %s", strerror(errno));
        return 0;
    }

    char * buf = new char[g_block_size];
    for(int i = idx; i < total_count; i+=THREAD_COUNT)
    {
        int begin = i * g_block_size;
        int len = g_block_size;

        int ret = hook_pread64(r_fd, buf, len, begin);
        if(ret >= 0) {
            /*hook_pwrite64(w_fd, buf, ret, begin);*/
            hook_lseek(w_fd, begin, SEEK_SET);
            hook_write(w_fd, buf, ret);

            if(ret < len )
            {
                log("******* AJLKFDJSFDLKSFSJL ***************");
                log("******* AJLKFDJSFDLKSFSJL ***************");
                log("******* AJLKFDJSFDLKSFSJL ***************");

            }
        } else {
            printf("pread error : %s", strerror(errno));
            break;
        }
    }
    delete []buf;

    hook_close(r_fd);
    hook_close(w_fd);

    return 0;
}

int main(int argc, char * argv[])
{

    if(argc != 4)
    {
        printf("usage : xx -encrypt|decrytp inputfile outputfile\n");
        return 0;
    }

    if(argv[1][0] != '-')
        return 0;

    if(strcmp(&argv[1][1], "mt") == 0)
    {
        strcpy(g_path, argv[2]);
        strcpy(g_path2, argv[3]);

        struct stat buf;
        stat(g_path, &buf);
        g_total_size = buf.st_size;

        pthread_t id[199];
        for(int i = 0; i < THREAD_COUNT; i++)
        {
            pthread_create(&id[i], 0, threadrutine, (void *)i);
            /*sleep(1);*/
        }

        for(int i = 0; i < THREAD_COUNT; i++)
        {
            pthread_join(id[i], 0);
        }
    }

#define B_SIZE 99 
    if(strcmp(&argv[1][1], "en") == 0)
    {
        int fd = hook_open(argv[2], O_RDONLY);
        if(fd <= 0)
        {
            log("encrypt : open original file fail!");
            return 0;
        }

        int fd2 = hook_open(argv[3], O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
        if(fd2 <= 0)
        {
            log("encrypt : open target file fail!");
            return 0;
        }

        char buf[B_SIZE] = {0};
        int len = 0;
        int ret = 0;
        while(ret = hook_read(fd, buf, B_SIZE))
        {
            printf(" \n\nmain write %d \n", ret);
           hook_pwrite64(fd2, buf, ret, len);
           len += ret;
        }

        hook_close(fd2);
        hook_close(fd);
    }

    if(strcmp(&argv[1][1], "de") == 0)
    {
        int fd = hook_open(argv[2], O_RDONLY);
        if(fd <= 0)
        {
            log("decrypt : open original file fail!");
            return 0;
        }

        int fd2 = open(argv[3], O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
        if(fd2 <= 0)
        {
            log("decrypt : open target file fail!");
            return 0;
        }

        char buf[B_SIZE] = {0};
        int ret = 0;
        int len = 0;
        while(ret = hook_pread64(fd, buf, B_SIZE, len))
        {
          write(fd2, buf, ret);
            len += ret;
        }

        close(fd2);
        hook_close(fd);
    }

    return 0;
}
