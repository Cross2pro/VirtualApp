//
// Created by zhangsong on 17-11-24.
//

#ifndef VIRTUALAPP_VIRTUALFILESYSTEM_H
#define VIRTUALAPP_VIRTUALFILESYSTEM_H

#include <map>
#include <string>
#include <pthread.h>
#include <utils/RefBase.h>
#include <utils/StrongPointer.h>
#include <utils/atomicVessel.h>
#include <utils/releaser.h>

#include "EncryptFile.h"
#include "TemplateFile.h"
#include "IgnoreFile.h"

/*
 * 虚拟文件描述符
 * 维护文件的状态 —— 文件游标等
 */
enum vfileState
{
    VFS_IGNORE = 0,
    VFS_TESTING = 1,
    VFS_ENCRYPT
};

class virtualFile;
class virtualFileDescribe : public xdja::zs::LightRefBase<virtualFileDescribe>
{
public:
    virtualFile * _vf;
    vfileState cur_state;

    virtualFileDescribe()
    {
        _vf = 0;
        cur_state = VFS_IGNORE;
    }
};

/*
 * 虚拟文件描述符集
 */
class virtualFileDescribeSet
{
    atomicVessel items[1024];
    releaser<virtualFileDescribe> rl;

public:
    void reset(int idx);
    void set(int idx, virtualFileDescribe * vfd);
    virtualFileDescribe * get(int idx);

    void release(virtualFileDescribe * vfd) { rl.release(vfd); }

    virtualFileDescribeSet() {pthread_rwlock_init(&_rw_lock, NULL);}
    virtual ~virtualFileDescribeSet() {
        pthread_rwlock_destroy(&_rw_lock);
        rl.finish();
    }   
    pthread_rwlock_t _rw_lock;

    static virtualFileDescribeSet & getVFDSet();
};

/*
 *
 */

class virtualFileManager;
class TemplateFile;                  //占位
class virtualFile
{
private:
    char _path[MAX_PATH];

    pthread_mutex_t _ref_lock;
    unsigned int refrence;                   //文件引用计数

    pthread_mutex_t _vfs_lock;
    vfileState       _vfs;                //文件状态

    pthread_rwlock_t _rw_lock;
    EncryptFile * ef;                       //操作加密文件的对象
    TemplateFile * tf;                      //操作临时文件的对象

public:
    virtualFile(char * path)
    {
        strcpy(_path, path);

        pthread_mutex_init(&_ref_lock, NULL);
        pthread_mutex_init(&_vfs_lock, NULL);
        pthread_rwlock_init(&_rw_lock, NULL);

        ef = 0;
        tf = 0;

        refrence = 0;
    }

    ~virtualFile()
    {
        pthread_mutex_destroy(&_ref_lock);
        pthread_mutex_destroy(&_vfs_lock);
        pthread_rwlock_destroy(&_rw_lock);

        if(tf != NULL)
        {
            //delete tf;
            delete tf;
        }

        if(ef != NULL)
        {
            delete ef;
        }
    }

    unsigned int addRef();
    unsigned int delRef();

    void setVFS(vfileState vfs);
    vfileState getVFS();

    void lockWhole();
    void unlockWhole();

public:
    int vpread64(int fd, char * buf, size_t len, off64_t from);
    int vpwrite64(int fd, char * buf, size_t len, off64_t from);

    int vread(int fd, char * buf, size_t len);
    int vwrite(int fd, char * buf, size_t len);

    int vfstat(int fd, struct stat * buf);
    off_t vlseek(int fd, off_t offset, int whence);
    int vllseek(int fd, unsigned long offset_high,
               unsigned long offset_low, loff_t *result,
               unsigned int whence);

    int vftruncate(int fd, off_t length);
    int vftruncate64(int fd, off64_t length);

    char * getPath() {return _path;}
    void setPath(char * path) { strncpy(_path, path, MAX_PATH); }

    bool create(int fd);


    int vclose();

    void forceTranslate();

private:

    friend class virtualFileManager;
};

/*
 * 虚拟文件管理器
 * 已文件绝对路径来管理文件
 * 每个linux file对应一个virtualFile
 */


typedef std::map<std::string, virtualFile*> VFMap;

class virtualFileManager
{
private:
    pthread_mutex_t _lock;
    VFMap _vfmap;

public:
    virtualFileManager()
    {
        pthread_mutex_init(&_lock, NULL);
    }

    ~virtualFileManager()
    {
        pthread_mutex_destroy(&_lock);
    }

    virtualFile * getVF(int fd, char * path, int * pErrno);

    virtualFile * queryVF(char *path);
    void updateVF(virtualFile & vf);

    void releaseVF(char *path);

    /*void forceClean(char * path);*/

    void deleted(char *path);

public:
    static virtualFileManager & getVFM();
};

#endif //VIRTUALAPP_VIRTUALFILESYSTEM_H
