//
// VirtualApp Native Project
//
#include <unistd.h>
#include <stdlib.h>
#include <fb/include/fb/ALog.h>
#include <Substrate/CydiaSubstrate.h>
#include <asm/mman.h>
#include <sys/mman.h>
#include <utils/zMd5.h>
#include <utils/controllerManagerNative.h>

#include "IOUniformer.h"
#include "SandboxFs.h"
#include "Path.h"
#include "SymbolFinder.h"

bool iu_loaded = false;

#include "transparentED/originalInterface.h"
#include "transparentED/ff_Recognizer.h"


#include "utils/zString.h"
#include "utils/utils.h"
#include "utils/Autolock.h"
#include "transparentED/virtualFileSystem.h"
#include "utils/mylog.h"

std::map<uint32_t, MmapFileInfo *> MmapInfoMap;
using namespace xdja;

void IOUniformer::init_env_before_all() {
    if (iu_loaded)
        return;

    char *api_level_chars = getenv("V_API_LEVEL");
    char *preview_api_level_chars = getenv("V_PREVIEW_API_LEVEL");
    char *need_dlopen_chars = getenv("V_NEED_DLOPEN");
    if (api_level_chars) {
        int api_level = atoi(api_level_chars);
        int preview_api_level;
        preview_api_level = atoi(preview_api_level_chars);
        int need_dlopen = atoi(need_dlopen_chars);

        char keep_env_name[25];
        char forbid_env_name[25];
        char dlopen_keep_env_name[25];
        char replace_src_env_name[25];
        char replace_dst_env_name[25];
        int i = 0;
        while (true) {
            sprintf(keep_env_name, "V_KEEP_ITEM_%d", i);
            char *item = getenv(keep_env_name);
            if (!item) {
                break;
            }
            add_keep_item(item);
            i++;
        }
        i = 0;
        while (true) {
            sprintf(dlopen_keep_env_name, "V_DLOPEN_KEEP_ITEM_%d", i);
            char *item = getenv(dlopen_keep_env_name);
            if (!item) {
                break;
            }
            add_dlopen_keep_item(item);
            i++;
        }
        i = 0;
        while (true) {
            sprintf(forbid_env_name, "V_FORBID_ITEM_%d", i);
            char *item = getenv(forbid_env_name);
            if (!item) {
                break;
            }
            add_forbidden_item(item);
            i++;
        }
        i = 0;
        while (true) {
            sprintf(replace_src_env_name, "V_REPLACE_ITEM_SRC_%d", i);
            char *item_src = getenv(replace_src_env_name);
            if (!item_src) {
                break;
            }
            sprintf(replace_dst_env_name, "V_REPLACE_ITEM_DST_%d", i);
            char *item_dst = getenv(replace_dst_env_name);
            add_replace_item(item_src, item_dst);
            i++;
        }
        startUniformer(getenv("V_SO_PATH"), api_level, preview_api_level, need_dlopen);
        iu_loaded = true;
    }
}


static inline void
hook_function(void *handle, const char *symbol, void *new_func, void **old_func) {
    void *addr = dlsym(handle, symbol);
    if (addr == NULL) {
        return;
    }
    MSHookFunction(addr, new_func, old_func);
}


void onSoLoaded(const char *name, void *handle);

void IOUniformer::redirect(const char *orig_path, const char *new_path) {
    add_replace_item(orig_path, new_path);
}

const char *IOUniformer::query(const char *orig_path) {
    int res;
    return relocate_path(orig_path, &res);
}

void IOUniformer::whitelist(const char *_path) {
    add_keep_item(_path);
}

void IOUniformer::dlopen_whitelist(const char *_path){
    add_dlopen_keep_item(_path);
}

void IOUniformer::forbid(const char *_path) {
    add_forbidden_item(_path);
}


const char *IOUniformer::reverse(const char *_path) {
    return reverse_relocate_path(_path);
}

__BEGIN_DECLS

#define FREE(ptr, org_ptr) { if ((void*) ptr != NULL && (void*) ptr != (void*) org_ptr) { free((void*) ptr); } }

// int faccessat(int dirfd, const char *pathname, int mode, int flags);
HOOK_DEF(int, faccessat, int dirfd, const char *pathname, int mode, int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_faccessat, dirfd, redirect_path, mode, flags);

    /*zString op("faccessat mode %p flags %p ret %d err %s", mode, flags, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int fchmodat(int dirfd, const char *pathname, mode_t mode, int flags);
HOOK_DEF(int, fchmodat, int dirfd, const char *pathname, mode_t mode, int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_fchmodat, dirfd, redirect_path, mode, flags);

    /*zString op("fchmodat mode %p flags %p ret %d err %s", mode, flags, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}
// int fchmod(const char *pathname, mode_t mode);
HOOK_DEF(int, fchmod, const char *pathname, mode_t mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_chmod, redirect_path, mode);

    /*zString op("fchmod mode %p ret %d err %s", mode, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int fstatat(int dirfd, const char *pathname, struct stat *buf, int flags);
HOOK_DEF(int, fstatat, int dirfd, const char *pathname, struct stat *buf, int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_fstatat64, dirfd, redirect_path, buf, flags);

    /*zString op("fstatat flags %p ret %d err %s", flags, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

// int mknodat(int dirfd, const char *pathname, mode_t mode, dev_t dev);
HOOK_DEF(int, mknodat, int dirfd, const char *pathname, mode_t mode, dev_t dev) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_mknodat, dirfd, redirect_path, mode, dev);

    /*zString op("mknodat mode %p ret %d err %s", mode, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}
// int mknod(const char *pathname, mode_t mode, dev_t dev);
HOOK_DEF(int, mknod, const char *pathname, mode_t mode, dev_t dev) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_mknod, redirect_path, mode, dev);

    /*zString op("mknod mode %p ret %d err %s", mode, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int utimensat(int dirfd, const char *pathname, const struct timespec times[2], int flags);
HOOK_DEF(int, utimensat, int dirfd, const char *pathname, const struct timespec times[2],
         int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_utimensat, dirfd, redirect_path, times, flags);

    /*zString op("utimensat flags %p ret %d err %s", flags, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int fchownat(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags);
HOOK_DEF(int, fchownat, int dirfd, const char *pathname, uid_t owner, gid_t group, int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_fchownat, dirfd, redirect_path, owner, group, flags);

    /*zString op("fchownat ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

// int chroot(const char *pathname);
HOOK_DEF(int, chroot, const char *pathname) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_chroot, redirect_path);

    /*zString op("chroot ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int renameat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath);
HOOK_DEF(int, renameat, int olddirfd, const char *oldpath, int newdirfd, const char *newpath) {
    int res_old;
    int res_new;
    const char *redirect_path_old = relocate_path(oldpath, &res_old);
    const char *redirect_path_new = relocate_path(newpath, &res_new);
    xdja::zs::sp<virtualFile> * vf2 = virtualFileManager::getVFM().queryVF((char *) redirect_path_old);
    if(vf2 != NULL)
    {
        slog(" *** need to force translate virtual File [%s] *** ", vf2->get()->getPath());

        xdja::zs::sp<virtualFile> pvf2(vf2->get());
        pvf2->lockWhole();
        pvf2->forceTranslate();
        pvf2->unlockWhole();
        pvf2->delRef();
    }

    {
        /**？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？？**/
        virtualFileManager::getVFM().deleted((char *) redirect_path_old);
    }

    int ret = syscall(__NR_renameat, olddirfd, redirect_path_old, newdirfd, redirect_path_new);

    xdja::zs::sp<virtualFile> * vf3 = virtualFileManager::getVFM().queryVF((char *) redirect_path_new);
    if(vf3 != NULL)
    {
        xdja::zs::sp<virtualFile> pvf3(vf3->get());
        slog(" *** update virtual file [%s] *** ", pvf3->getPath());
        pvf3->lockWhole();
        virtualFileManager::getVFM().updateVF(*pvf3.get());
        pvf3->unlockWhole();
    }

    /*zString op("renameat to %s ret %d err %s", redirect_path_new, ret, getErr);
    doFileTrace(redirect_path_old, op.toString());*/

    FREE(redirect_path_old, oldpath);
    FREE(redirect_path_new, newpath);
    return ret;
}
// int rename(const char *oldpath, const char *newpath);
HOOK_DEF(int, rename, const char *oldpath, const char *newpath) {
    int res_old;
    int res_new;
    const char *redirect_path_old = relocate_path(oldpath, &res_old);
    const char *redirect_path_new = relocate_path(newpath, &res_new);
    int ret = syscall(__NR_rename, redirect_path_old, redirect_path_new);

    /*zString op("rename to %s ret %d err %s", redirect_path_new, ret, getErr);
    doFileTrace(redirect_path_old, op.toString());*/

    FREE(redirect_path_old, oldpath);
    FREE(redirect_path_new, newpath);
    return ret;
}


// int unlinkat(int dirfd, const char *pathname, int flags);
HOOK_DEF(int, unlinkat, int dirfd, const char *pathname, int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_unlinkat, dirfd, redirect_path, flags);

    if(ret == 0)
    {
        /***？？？？？？？？？？？？？？？？？？？？？？？？？？？？？***/
        virtualFileManager::getVFM().deleted((char *)redirect_path);
    }

    /*zString op("unlinkat ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}
// int unlink(const char *pathname);
HOOK_DEF(int, unlink, const char *pathname) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_unlink, redirect_path);

    /*zString op("unlink ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int symlinkat(const char *oldpath, int newdirfd, const char *newpath);
HOOK_DEF(int, symlinkat, const char *oldpath, int newdirfd, const char *newpath) {
    int res_old;
    int res_new;
    const char *redirect_path_old = relocate_path(oldpath, &res_old);
    const char *redirect_path_new = relocate_path(newpath, &res_new);
    int ret = syscall(__NR_symlinkat, redirect_path_old, newdirfd, redirect_path_new);

    /*zString op("symlinkat to %s ret %d err %s", redirect_path_new, ret, getErr);
    doFileTrace(redirect_path_old, op.toString());*/

    FREE(redirect_path_old, oldpath);
    FREE(redirect_path_new, newpath);
    return ret;
}
// int symlink(const char *oldpath, const char *newpath);
HOOK_DEF(int, symlink, const char *oldpath, const char *newpath) {
    int res_old;
    int res_new;
    const char *redirect_path_old = relocate_path(oldpath, &res_old);
    const char *redirect_path_new = relocate_path(newpath, &res_new);
    int ret = syscall(__NR_symlink, redirect_path_old, redirect_path_new);

    /*zString op("symlink to %s ret %d err %s", redirect_path_new, ret, getErr);
    doFileTrace(redirect_path_old, op.toString());*/

    FREE(redirect_path_old, oldpath);
    FREE(redirect_path_new, newpath);
    return ret;
}


// int linkat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath, int flags);
HOOK_DEF(int, linkat, int olddirfd, const char *oldpath, int newdirfd, const char *newpath,
         int flags) {
    int res_old;
    int res_new;
    const char *redirect_path_old = relocate_path(oldpath, &res_old);
    const char *redirect_path_new = relocate_path(newpath, &res_new);
    int ret = syscall(__NR_linkat, olddirfd, redirect_path_old, newdirfd, redirect_path_new, flags);

    /*zString op("linkat to %s ret %d err %s", redirect_path_new, ret, getErr);
    doFileTrace(redirect_path_old, op.toString());*/

    FREE(redirect_path_old, oldpath);
    FREE(redirect_path_new, newpath);
    return ret;
}
// int link(const char *oldpath, const char *newpath);
HOOK_DEF(int, link, const char *oldpath, const char *newpath) {
    int res_old;
    int res_new;
    const char *redirect_path_old = relocate_path(oldpath, &res_old);
    const char *redirect_path_new = relocate_path(newpath, &res_new);
    int ret = syscall(__NR_link, redirect_path_old, redirect_path_new);

    /*zString op("link to %s ret %d err %s", redirect_path_new, ret, getErr);
    doFileTrace(redirect_path_old, op.toString());*/

    FREE(redirect_path_old, oldpath);
    FREE(redirect_path_new, newpath);
    return ret;
}


// int utimes(const char *filename, const struct timeval *tvp);
HOOK_DEF(int, utimes, const char *pathname, const struct timeval *tvp) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_utimes, redirect_path, tvp);

    /*zString op("utimes ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int access(const char *pathname, int mode);
HOOK_DEF(int, access, const char *pathname, int mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_access, redirect_path, mode);

    /*zString op("access ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int chmod(const char *path, mode_t mode);
HOOK_DEF(int, chmod, const char *pathname, mode_t mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_chmod, redirect_path, mode);

    /*zString op("chmod ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int chown(const char *path, uid_t owner, gid_t group);
HOOK_DEF(int, chown, const char *pathname, uid_t owner, gid_t group) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_chown, redirect_path, owner, group);

    /*zString op("chown ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int lstat(const char *path, struct stat *buf);
HOOK_DEF(int, lstat, const char *pathname, struct stat *buf) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_lstat64, redirect_path, buf);

    /*zString op("lstat ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int stat(const char *path, struct stat *buf);
HOOK_DEF(int, stat, const char *pathname, struct stat *buf) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_stat64, redirect_path, buf);

    /*zString op("stat ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int mkdirat(int dirfd, const char *pathname, mode_t mode);
HOOK_DEF(int, mkdirat, int dirfd, const char *pathname, mode_t mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_mkdirat, dirfd, redirect_path, mode);

    /*zString op("mkdirat ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}
// int mkdir(const char *pathname, mode_t mode);
HOOK_DEF(int, mkdir, const char *pathname, mode_t mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_mkdir, redirect_path, mode);

    /*zString op("mkdir ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int rmdir(const char *pathname);
HOOK_DEF(int, rmdir, const char *pathname) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_rmdir, redirect_path);

    /*zString op("rmdir ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz);
HOOK_DEF(int, readlinkat, int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_readlinkat, dirfd, redirect_path, buf, bufsiz);

    /*zString op("readlinkat ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}
// ssize_t readlink(const char *path, char *buf, size_t bufsiz);
HOOK_DEF(ssize_t, readlink, const char *pathname, char *buf, size_t bufsiz) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    ssize_t ret = syscall(__NR_readlink, redirect_path, buf, bufsiz);

    /*zString op("readlink ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int __statfs64(const char *path, size_t size, struct statfs *stat);
HOOK_DEF(int, __statfs64, const char *pathname, size_t size, struct statfs *stat) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_statfs64, redirect_path, size, stat);

    /*zString op("__statfs64 ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int truncate(const char *path, off_t length);
HOOK_DEF(int, truncate, const char *pathname, off_t length) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_truncate, redirect_path, length);

    /*zString op("truncate length %ld ret %d err %s", length, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

#define RETURN_IF_FORBID if(res == FORBID) return -1;

// int truncate64(const char *pathname, off_t length);
HOOK_DEF(int, truncate64, const char *pathname, off_t length) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    RETURN_IF_FORBID
    int ret = syscall(__NR_truncate64, redirect_path, length);

    /*zString op("truncate64 length %ld ret %d err %s", length, ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int chdir(const char *path);
HOOK_DEF(int, chdir, const char *pathname) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    RETURN_IF_FORBID
    int ret = syscall(__NR_chdir, redirect_path);

    /*zString op("chdir ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}


// int __getcwd(char *buf, size_t size);
HOOK_DEF(int, __getcwd, char *buf, size_t size) {
    int ret = syscall(__NR_getcwd, buf, size);
    if (!ret) {

    }
    return ret;
}


// int __openat(int fd, const char *pathname, int flags, int mode);
HOOK_DEF(int, __openat, int fd, const char *pathname, int flags, int mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);

    if ((flags & O_ACCMODE) == O_WRONLY) {
        flags &= ~O_ACCMODE;
        flags |= O_RDWR;
    }

    int ret = syscall(__NR_openat, fd, redirect_path, flags, mode);
    /*zString op("openat fd = %d err = %s", ret, strerror(errno));
    doFileTrace(redirect_path, op.toString());*/

    if(ret > 0 && (is_TED_Enable()||changeDecryptState(false,1)) && isEncryptPath(redirect_path)) {


        /*******************only here**********************/
        virtualFileDescribe *pvfd = new virtualFileDescribe(ret);
        pvfd->incStrong(0);
        /***************************************************/
        virtualFileDescribeSet::getVFDSet().set(ret, pvfd);
        /*
        * 首先获取vfd，获取不到一定是发生异常，返回错误
        */
        xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(ret));

        if (vfd.get() == nullptr) {
            slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
            return -1;
        }

        int _Errno;
        xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), (char *) redirect_path, &_Errno));
        if (vf.get() != nullptr) {
            LOGE("judge : open vf [PATH %s] [VFS %d] [FD %d] [VFD %p]", vf->getPath(), vf->getVFS(), ret, vfd.get());
            if ((flags & O_APPEND) == O_APPEND) {
                vf->vlseek(vfd.get(), 0, SEEK_END);
            } else {
                vf->vlseek(vfd.get(), 0, SEEK_SET);
            }
        } else {
            virtualFileDescribeSet::getVFDSet().reset(ret);
            /******through this way to release vfd *********/
            virtualFileDescribeSet::getVFDSet().release(pvfd);
            /***********************************************/

            if(_Errno < 0)
            {
                //这种情况需要让openat 返回失败
                originalInterface::original_close(ret);
                ret = -1;
                errno = EACCES;

                if(flags & O_CREAT)
                {
                    originalInterface::original_unlinkat(AT_FDCWD, redirect_path, 0);
                }

                LOGE("judge : **** force openat fail !!! ****");
            }
        }
    }
    FREE(redirect_path, pathname);

    return ret;
}

HOOK_DEF(int, close, int __fd) {

    /*zString path;
    getPathFromFd(__fd, path);

    zString zlog("close %d", __fd);
    doFileTrace(path.toString(), zlog.toString());*/

    int ret;
    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(__fd));
    if (vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(__fd)) {
            log("close fd[%d] flag is closing", __fd);
            return -1;
        }
    } else {
        virtualFileDescribeSet::getVFDSet().setFlag(__fd, FD_CLOSING);

        virtualFileDescribeSet::getVFDSet().reset(__fd);
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            log("trace_close fd[%d]path[%s]vfd[%p]", __fd, vf->getPath(), vfd.get());
            virtualFileManager::getVFM().releaseVF(vf->getPath(), vfd.get());
        }

        /******through this way to release vfd *********/
        virtualFileDescribeSet::getVFDSet().release(vfd.get());
        /***********************************************/
    }

    ret = syscall(__NR_close, __fd);
    virtualFileDescribeSet::getVFDSet().clearFlag(__fd);
    return ret;
}

// int fstatat64(int dirfd, const char *pathname, struct stat *buf, int flags);
HOOK_DEF(int, fstatat64, int dirfd, const char *pathname, struct stat *buf, int flags) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_fstatat64, dirfd, redirect_path, buf, flags);
    if (is_TED_Enable()) {
        int fd = originalInterface::original_openat(AT_FDCWD, redirect_path, O_RDONLY, 0);

        if (fd > 0) {
            if (EncryptFile::isEncryptFile(fd)) {
                EncryptFile ef(redirect_path);
                if (ef.create(fd, ENCRYPT_READ)) {
                    ef.fstat(fd, buf);
                }
            }
            originalInterface::original_close(fd);
        }
    }
    /*zString op("fstatat64 ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

// int __open(const char *pathname, int flags, int mode);
HOOK_DEF(int, __open, const char *pathname, int flags, int mode) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_open, redirect_path, flags, mode);

    /*zString op("__open ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

// int __statfs (__const char *__file, struct statfs *__buf);
HOOK_DEF(int, __statfs, __const char *__file, struct statfs *__buf) {
    int res;
    const char *redirect_path = relocate_path(__file, &res);
    int ret = syscall(__NR_statfs, redirect_path, __buf);

    /*zString op("__statfs ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, __file);
    return ret;
}

// int lchown(const char *pathname, uid_t owner, gid_t group);
HOOK_DEF(int, lchown, const char *pathname, uid_t owner, gid_t group) {
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    int ret = syscall(__NR_lchown, redirect_path, owner, group);

    /*zString op("lchown ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

int inline getArrayItemCount(char *const array[]) {
    int i;
    for (i = 0; array[i]; ++i);
    return i;
}


char **build_new_env(char *const envp[]) {
    char *provided_ld_preload = NULL;
    int provided_ld_preload_index = -1;
    int orig_envp_count = getArrayItemCount(envp);

    for (int i = 0; i < orig_envp_count; i++) {
        if (strstr(envp[i], "LD_PRELOAD")) {
            provided_ld_preload = envp[i];
            provided_ld_preload_index = i;
        }
    }
    char ld_preload[200];
    char *so_path = getenv("V_SO_PATH");
    if (provided_ld_preload) {
        sprintf(ld_preload, "LD_PRELOAD=%s:%s", so_path, provided_ld_preload + 11);
    } else {
        sprintf(ld_preload, "LD_PRELOAD=%s", so_path);
    }
    int new_envp_count = orig_envp_count
                         + get_keep_item_count()
                         + get_forbidden_item_count()
                         + get_replace_item_count() * 2 + 1
                         + get_dlopen_keep_item_count();
    if (provided_ld_preload) {
        new_envp_count--;
    }
    char **new_envp = (char **) malloc(new_envp_count * sizeof(char *));
    int cur = 0;
    new_envp[cur++] = ld_preload;
    for (int i = 0; i < orig_envp_count; ++i) {
        if (i != provided_ld_preload_index) {
            new_envp[cur++] = envp[i];
        }
    }
    for (int i = 0; environ[i]; ++i) {
        if (environ[i][0] == 'V' && environ[i][1] == '_') {
            new_envp[cur++] = environ[i];
        }
    }
    new_envp[cur] = NULL;
    return new_envp;
}

// int (*origin_execve)(const char *pathname, char *const argv[], char *const envp[]);
HOOK_DEF(int, execve, const char *pathname, char *argv[], char *const envp[]) {
    /**
     * CANNOT LINK EXECUTABLE "/system/bin/cat": "/data/app/io.virtualapp-1/lib/arm/libva-native.so" is 32-bit instead of 64-bit.
     *
     * We will support 64Bit to adopt it.
     */
    int res;
    const char *redirect_path = relocate_path(pathname, &res);
    char *ld = getenv("LD_PRELOAD");
    if (ld) {
        if (strstr(ld, "libNimsWrap.so") || strstr(ld, "stamina.so")) {
            int ret = syscall(__NR_execve, redirect_path, argv, envp);
            FREE(redirect_path, pathname);
            return ret;
        }
    }
    if (strstr(pathname, "dex2oat")) {
        char **new_envp = build_new_env(envp);
        int ret = syscall(__NR_execve, redirect_path, argv, new_envp);
        FREE(redirect_path, pathname);
        //free mem
        free(new_envp);
        return ret;
    }
    int ret = syscall(__NR_execve, redirect_path, argv, envp);

    /*zString op("execve ret %d err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    FREE(redirect_path, pathname);
    return ret;
}

const char *relocate_path_dlopen(const char *path, int *result) {
    if (path[0] == '/' || path[0] == '.') {
        return relocate_path(path, result, 1);
    }
    *result = NOT_MATCH;
    return path;
}

HOOK_DEF(void*, dlopen, const char *filename, int flag) {
    int res;
    const char *redirect_path = relocate_path_dlopen(filename, &res);
    void *ret = orig_dlopen(redirect_path, flag);

    /*zString op("dlopen ret %p err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    onSoLoaded(filename, ret);
    ALOGD("dlopen : %s, return : %p.", redirect_path, ret);
    FREE(redirect_path, filename);
    return ret;
}

HOOK_DEF(void*, do_dlopen_V19, const char *filename, int flag, const void *extinfo) {
    int res;
    const char *redirect_path = relocate_path_dlopen(filename, &res);
    void *ret = orig_do_dlopen_V19(redirect_path, flag, extinfo);

    /*zString op("do_dlopen_V19 ret %p err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    onSoLoaded(filename, ret);
    ALOGD("do_dlopen : %s, return : %p.", redirect_path, ret);
    FREE(redirect_path, filename);
    return ret;
}

HOOK_DEF(void*, do_dlopen_V24, const char *name, int flags, const void *extinfo,
         void *caller_addr) {
    int res;
    const char *redirect_path = relocate_path_dlopen(name, &res);
    void *ret = orig_do_dlopen_V24(redirect_path, flags, extinfo, caller_addr);

    /*zString op("do_dlopen_V24 ret %p err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    onSoLoaded(name, ret);
    ALOGD("do_dlopen : %s, return : %p.", redirect_path, ret);
    FREE(redirect_path, name);
    return ret;
}

HOOK_DEF(void*, do_dlopen_V26, const char *name, int flags, const void *extinfo,
         void *caller_addr) {
    int res;
    const char *redirect_path = relocate_path_dlopen(name, &res);
    void *ret = orig_do_dlopen_V26(redirect_path, flags, extinfo, caller_addr);

    /*zString op("do_dlopen_V26 ret %p err %s", ret, getErr);
    doFileTrace(redirect_path, op.toString());*/

    onSoLoaded(name, ret);
    ALOGD("do_dlopen : %s, return : %p.", redirect_path, ret);
    FREE(redirect_path, name);
    return ret;
}


//void *dlsym(void *handle,const char *symbol)
HOOK_DEF(void*, dlsym, void *handle, char *symbol) {
    ALOGD("dlsym : %p %s.", handle, symbol);
    return orig_dlsym(handle, symbol);
}

// int kill(pid_t pid, int sig);
HOOK_DEF(int, kill, pid_t pid, int sig) {
    ALOGD(">>>>> kill >>> pid: %d, sig: %d.", pid, sig);
    int ret = syscall(__NR_kill, pid, sig);
    return ret;
}

HOOK_DEF(pid_t, vfork) {
    return fork();
}

HOOK_DEF(ssize_t, pread64, int fd, void* buf, size_t count, off64_t offset) {
    ssize_t ret = 0;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("pread64 fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vpread64(vfd.get(), (char *) buf, count, offset);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_pread64(fd, buf, count, offset);

    /*zMd5 ms;
    zString op("%cpread64 offset [fd %d] [%lld] count [%d] ret [%d] content[%s]", flag?'v':' ', fd, offset, count, ret, ms.getSig((char *)buf, ret));
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

HOOK_DEF(ssize_t, pwrite64, int fd, const void *buf, size_t count, off64_t offset) {
    ssize_t ret = 0;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("pwrite64 fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vpwrite64(vfd.get(), (char *) buf, count, offset);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_pwrite64(fd, buf, count, offset);

    /*zMd5 ms;
    zString op("%cpwrite64 offset [fd %d] [%lld] count [%d] ret [%d] content[%s]", flag?'v':' ', fd, offset, count, ret, ms.getSig((char *)buf, ret));
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

HOOK_DEF(ssize_t, read, int fd, void *buf, size_t count) {
    ssize_t ret = 0;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("read fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vread(vfd.get(), (char *) buf, count);
            flag = true;
        }
    }

    if(!flag)
        ret = syscall(__NR_read, fd, buf, count);

    /*zMd5 ms;
    zString op("%cread count [fd %d] [%d] ret [%d] content[%s]", flag?'v':' ', fd, count, ret, ms.getSig((char *)buf, ret));
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

HOOK_DEF(ssize_t, write, int fd, const void* buf, size_t count) {
    ssize_t ret = 0;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if (virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("write fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vwrite(vfd.get(), (char *) buf, count);
            flag = true;
        }
    }

    if(!flag)
        ret = syscall(__NR_write, fd, buf, count);

    /*zMd5 ms;
    zString op("%cwrite count [fd %d] [%d] ret [%d] content[%s]", flag?'v':' ', fd, count, ret, ms.getSig((char *)buf, ret));
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

HOOK_DEF(int, munmap, void *addr, size_t length) {
    int ret = -1;

    MmapFileInfo *fileInfo = 0;
    std::map<uint32_t , MmapFileInfo *>::iterator iter = MmapInfoMap.find(std::uint32_t(addr));
    if (iter != MmapInfoMap.end()) {
        MmapInfoMap.erase(iter);
        fileInfo = iter->second;
        if ((fileInfo->_flag & MAP_SHARED)) {
            int fd = syscall(__NR_openat, AT_FDCWD, fileInfo->_path, O_RDWR, 0);

            if (fd > 0) {
                virtualFileDescribe *pvfd = new virtualFileDescribe(fd);
                pvfd->incStrong(0);
                virtualFileDescribeSet::getVFDSet().set(fd, pvfd);
                xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));

                if (vfd.get() == nullptr) {
                    slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
                    return -1;
                }

                int _Errno;
                xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), fileInfo->_path,
                                                                     &_Errno));
                if (vf.get() != nullptr) {
                    vf->vpwrite64(vfd.get(), (char *) addr, length, fileInfo->_offsize);
                }

                virtualFileDescribeSet::getVFDSet().reset(fd);
                virtualFileDescribeSet::getVFDSet().release(pvfd);
                syscall(__NR_close, fd);
            }
        }
    }

    ret = syscall(__NR_munmap, addr, length);

    return ret;
}

HOOK_DEF(int, msync, void *addr, size_t size, int flags) {
    int ret = -1;

    MmapFileInfo *fileInfo = 0;
    std::map<uint32_t , MmapFileInfo *>::iterator iter = MmapInfoMap.find(std::uint32_t(addr));
    if (iter != MmapInfoMap.end()) {
        fileInfo = iter->second;
        if ((fileInfo->_flag & MAP_SHARED)) {
            int fd = syscall(__NR_openat, AT_FDCWD, fileInfo->_path, O_RDWR, 0);

            if (fd > 0) {
                virtualFileDescribe *pvfd = new virtualFileDescribe(fd);
                pvfd->incStrong(0);
                virtualFileDescribeSet::getVFDSet().set(fd, pvfd);
                xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));

                if (vfd.get() == nullptr) {
                    slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
                    return -1;
                }

                int _Errno;
                xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), fileInfo->_path,
                                                                     &_Errno));
                if (vf.get() != nullptr) {
                    vf->vpwrite64(vfd.get(), (char *) addr, size, fileInfo->_offsize);
                }

                virtualFileDescribeSet::getVFDSet().reset(fd);
                virtualFileDescribeSet::getVFDSet().release(pvfd);
                syscall(__NR_close, fd);
            }
        }
    }

    ret = syscall(__NR_msync, addr, size, flags);

    return ret;
}

HOOK_DEF(void *, __mmap2, void *addr, size_t length, int prot,int flags, int fd, size_t pgoffset) {
    void * ret = 0;
    bool flag = false;

    do {
        if (fd == -1) break;

        xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));

        if (vfd.get() == nullptr) {
            if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
                log("__mmap2 fd[%d] flag is closing", fd);
                return MAP_FAILED;
            }
        } else {
            xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
            if (vf.get() != nullptr) {
                if (vf->getVFS() == VFS_ENCRYPT) {
                    flags |= MAP_ANONYMOUS;     //申请匿名内存
                    ret = (void *) syscall(__NR_mmap2, addr, length, prot, flags, 0, 0);

                    bool nowrite = (prot & PROT_WRITE) == 0;
                    if (nowrite && -1 == mprotect(ret, length, prot | PROT_WRITE)) {
                        LOGE("__mmap2 mprotect failed.");
                    } else {
                        off64_t pos = pgoffset * 4096;
                        vf->vpread64(vfd.get(), (char *) ret, length, pos);

                        if (nowrite) {
                            if (0 != mprotect(ret, length, prot)) {
                                LOGE("__mmap2 mprotect restore prot fails.");
                            }
                        }
                        MmapFileInfo *fileInfo = new MmapFileInfo(vf->getPath(), pgoffset,
                                                                  flags);
                        MmapInfoMap.insert(
                                std::pair<uint32_t, MmapFileInfo *>(uint32_t(ret), fileInfo));

                        flag = true;
                    }
                }
            }
        }
    }while(false);

    if(fd > 0)
    {
        /*zString path;
        getPathFromFd(fd, path);

        zString op("%c__mmap2 length %d flags %p pgoffset %p", flag?'v':' ', length, flags, pgoffset);
        doFileTrace(path.toString(), op.toString());*/
    }

    if(!flag)
        ret = (void *) syscall(__NR_mmap2, addr, length, prot, flags, fd, pgoffset);

    return ret;
}

HOOK_DEF(int, fstat, int fd, struct stat *buf)
{
    int ret;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("fstat fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vfstat(vfd.get(), buf);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_fstat(fd, buf);

    /*zString op("%cfstat [fd %d] ret %d err %s", flag?'v':' ', fd, ret, getErr);
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

HOOK_DEF(off_t, lseek, int fd, off_t offset, int whence)
{
    off_t ret;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("lseek fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vlseek(vfd.get(), offset, whence);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_lseek(fd, offset, whence);

    /*zString op("%clseek [fd %d] offset %ld whence %d ret %ld err %s", flag?'v':' ', fd, offset, whence, ret, getErr);
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

HOOK_DEF(int, __llseek, unsigned int fd, unsigned long offset_high,
         unsigned long offset_low, off64_t *result,
         unsigned int whence)
{
    off64_t rel_offset = 0;
    bool flag = false;

    rel_offset |= offset_high;
    rel_offset <<= 32;
    rel_offset |= offset_low;

    int ret;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("__llseek fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vllseek(vfd.get(), offset_high, offset_low, result, whence);
            flag = true;
        }
    }

    if(!flag)
        ret = orig___llseek(fd, offset_high, offset_low, result, whence);

    /*zString op("%cllseek [fd %d] offset %lld whence %d ret %lld err %s", flag?'v':' ', fd, rel_offset, whence, *result, getErr);
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

//int ftruncate64(int, off_t)
HOOK_DEF(int, ftruncate64, int fd, off64_t length)
{
    int ret;
    bool flag = false;

    /*zString path;
    getPathFromFd(fd, path);*/

    xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(fd));
    if(vfd.get() == nullptr) {
        if(virtualFileDescribeSet::getVFDSet().getFlag(fd)) {
            log("ftruncate64 fd[%d] flag is closing", fd);
            return -1;
        }
    } else {
        /*path.format("%s", vfd->_vf->getPath());*/
        xdja::zs::sp<virtualFile> vf(vfd->_vf->get());
        if (vf.get() != nullptr) {
            ret = vf->vftruncate64(vfd.get(), length);
            flag = true;
        }
    }

    if(!flag)
        ret = orig_ftruncate64(fd, length);

    /*zString op("%cftruncate64 [fd %d] length %lld ret %d err %s", flag?'v':' ', fd, length, ret, getErr);
    doFileTrace(path.toString(), op.toString());*/

    return ret;
}

#define ENCRYPTFILE_HEADLENGTH 50

//ssize_t sendfile(int out_fd, int in_fd, off_t* offset, size_t count)
HOOK_DEF(ssize_t, sendfile, int out_fd, int in_fd, off_t* offset, size_t count)
{
    ssize_t ret;

    off_t off = 0;
    if(offset != 0)
        off = *offset;

    struct stat st;
    originalInterface::original_fstat(in_fd,&st);

    xdja::zs::sp<virtualFileDescribe> in_vfd(virtualFileDescribeSet::getVFDSet().get(in_fd));
    xdja::zs::sp<virtualFileDescribe> out_vfd(virtualFileDescribeSet::getVFDSet().get(out_fd));
    if(in_vfd.get() == nullptr && out_vfd.get() == nullptr) {
        if((virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) &&
           (virtualFileDescribeSet::getVFDSet().getFlag(in_fd))) {
            log("sendfile out_fd[%d] and in_fd[%d] flag is closing", out_fd, in_fd);
            return -1;
        }
        //完全不管
        ret = orig_sendfile(out_fd, in_fd, offset, count);
    } else {

        size_t real_count = 0;
        if(off + count > (st.st_size - ENCRYPTFILE_HEADLENGTH)) {
            real_count = (size_t)(st.st_size - ENCRYPTFILE_HEADLENGTH - off);
        } else {
            real_count = count;
        }

        if(in_vfd.get() != nullptr && out_vfd.get() != nullptr) //完全管理
        {
            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());
            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());
            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            }
        }
        else if(in_vfd.get() == nullptr && out_vfd.get() != nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(in_fd)) {
                log("sendfile in_fd[%d] flag is closing", in_fd);
                return -1;
            }

            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());
            if(offset != 0)
            {
                ignoreFile::lseek(in_fd, off, SEEK_SET);
            } else {
                ignoreFile::lseek(in_fd, 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = ignoreFile::read(in_fd,buf,real_count % 1024);
                } else {
                    rl = ignoreFile::read(in_fd,buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                ignoreFile::lseek(in_fd, off, SEEK_SET);
            }
        }
        else if(in_vfd.get() != nullptr && out_vfd.get() == nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) {
                log("sendfile out_fd[%d] flag is closing", out_fd);
                return -1;
            }

            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());
            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024];
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                ignoreFile::write(out_fd,buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                in_vf->vlseek(in_vfd.get(), off, SEEK_SET);
            }
        }
    }

    /*zString path1, path2;
    getPathFromFd(out_fd, path1);
    getPathFromFd(in_fd, path2);

    zString zlog("from %s to %s, count = %d, ret = %d", path2.toString(), path1.toString(), count, ret);
    doFileTrace("sendfile", zlog.toString());*/

    return ret;
}

//ssize_t sendfile64(int out_fd, int in_fd, off64_t* offset, size_t count)
HOOK_DEF(ssize_t, sendfile64, int out_fd, int in_fd, off64_t* offset, size_t count)
{
    ssize_t ret;

    off64_t off = 0;
    if(offset != 0)
        off = *offset;

    struct stat st;
    originalInterface::original_fstat(in_fd,&st);

    unsigned long off_hi = static_cast<unsigned long>(off >> 32);
    unsigned long off_lo = static_cast<unsigned long>(off);

    xdja::zs::sp<virtualFileDescribe> in_vfd(virtualFileDescribeSet::getVFDSet().get(in_fd));
    xdja::zs::sp<virtualFileDescribe> out_vfd(virtualFileDescribeSet::getVFDSet().get(out_fd));
    if(in_vfd.get() == nullptr && out_vfd.get() == nullptr) {
        if((virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) &&
           (virtualFileDescribeSet::getVFDSet().getFlag(in_fd))) {
            log("sendfile64 out_fd[%d] and in_fd[%d] flag is closing", out_fd, in_fd);
            return -1;
        }
        //完全不管
        ret = orig_sendfile64(out_fd, in_fd, offset, count);
    } else {

        size_t real_count = 0;
        if(off + count > (st.st_size - ENCRYPTFILE_HEADLENGTH)) {
            real_count = (size_t)(st.st_size - ENCRYPTFILE_HEADLENGTH - off);
        } else {
            real_count = count;
        }

        if(in_vfd.get() != nullptr && out_vfd.get() != nullptr) //完全管理
        {
            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());
            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());
            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            }
        }
        else if(in_vfd.get() == nullptr && out_vfd.get() != nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(in_fd)) {
                log("sendfile64 in_fd[%d] flag is closing", in_fd);
                return -1;
            }
            xdja::zs::sp<virtualFile> out_vf(out_vfd->_vf->get());
            if(offset != 0)
            {
                loff_t result;
                ignoreFile::llseek(in_fd, off_hi, off_lo, &result, SEEK_SET);
            } else {
                ignoreFile::lseek(in_fd, 0, SEEK_CUR);
            }

            char * buf = new char[1024]{0};
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = ignoreFile::read(in_fd,buf,real_count % 1024);
                } else {
                    rl = ignoreFile::read(in_fd,buf,1024);
                }
                out_vf->vwrite(out_vfd.get(),buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                loff_t result;
                ignoreFile::llseek(in_fd, off_hi, off_lo, &result, SEEK_SET);
            }
        }
        else if(in_vfd.get() != nullptr && out_vfd.get() == nullptr)
        {
            if (virtualFileDescribeSet::getVFDSet().getFlag(out_fd)) {
                log("sendfile64 out_fd[%d] flag is closing", out_fd);
                return -1;
            }

            xdja::zs::sp<virtualFile> in_vf(in_vfd->_vf->get());
            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            } else {
                in_vf->vlseek(in_vfd.get(), 0, SEEK_CUR);
            }

            char * buf = new char[1024];
            ret = 0;
            int rl = 0;
            int size = 0;
            while(size < real_count) {
                size += 1024;
                if(size > real_count) {
                    rl = in_vf->vread(in_vfd.get(),buf,real_count % 1024);
                } else {
                    rl = in_vf->vread(in_vfd.get(),buf,1024);
                }
                ignoreFile::write(out_fd,buf,rl);
                ret += rl;
            }

            delete []buf;

            if(offset != 0)
            {
                loff_t result;
                in_vf->vllseek(in_vfd.get(), off_hi, off_lo, &result, SEEK_SET);
            }
        }
    }

    /*zString path1, path2;
    getPathFromFd(out_fd, path1);
    getPathFromFd(in_fd, path2);

    zString zlog("from %s to %s, count = %d, ret = %d", path2.toString(), path1.toString(), count, ret);
    doFileTrace("sendfile64", zlog.toString());*/

    return ret;
}

//int dup(int oldfd);
HOOK_DEF(int, dup, int oldfd)
{
    int ret = syscall(__NR_dup, oldfd);

    zString path, path2;
    getPathFromFd(oldfd, path);
    getPathFromFd(ret, path2);

    /*zString zlog("dup from %s [%d] to %s [%d]", path.toString(), oldfd, path2.toString(), ret);

    doFileTrace(path.toString(), zlog.toString());*/

    if(ret > 0 && (is_TED_Enable()||changeDecryptState(false,1)) && isEncryptPath(path2.toString())) {
        /*******************only here**********************/
        virtualFileDescribe *pvfd = new virtualFileDescribe(ret);
        pvfd->incStrong(0);
        /***************************************************/
        virtualFileDescribeSet::getVFDSet().set(ret, pvfd);

        /*
        * 首先获取vfd，获取不到一定是发生异常，返回错误
        */
        xdja::zs::sp<virtualFileDescribe> vfd(virtualFileDescribeSet::getVFDSet().get(ret));
        if (vfd.get() == nullptr) {
            slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
            return -1;
        }

        int _Errno;
        xdja::zs::sp<virtualFile> vf(virtualFileManager::getVFM().getVF(vfd.get(), path2.toString(), &_Errno));
        if (vf.get() != nullptr) {
            LOGE("judge : open vf [PATH %s] [VFS %d] [FD %d]", vf->getPath(), vf->getVFS(), ret);
            vf->vlseek(vfd.get(), 0, SEEK_SET);
        } else {
            virtualFileDescribeSet::getVFDSet().reset(ret);
            /******through this way to release vfd *********/
            virtualFileDescribeSet::getVFDSet().release(pvfd);
            /***********************************************/

            if(_Errno < 0)
            {
                //这种情况需要让openat 返回失败
                /*originalInterface::original_close(ret);
                ret = -1;
                errno = EACCES;

                if(flags & O_CREAT)
                {
                    originalInterface::original_unlinkat(AT_FDCWD, redirect_path, 0);
                }

                LOGE("judge : **** force openat fail !!! ****");*/
            }
        }
    }

    return ret;
}

//int dup3(int oldfd, int newfd, int flags);
HOOK_DEF(int, dup3, int oldfd, int newfd, int flags)
{
    /*zString path;
    getPathFromFd(oldfd, path);
    doFileTrace(path.toString(), (char *)"dup3");*/

    return syscall(__NR_dup3, oldfd, newfd, flags);
}

HOOK_DEF(int, connect ,int sd, struct sockaddr* addr, socklen_t socklen) {
    int ret = -1;
    if(!controllerManagerNative::isNetworkEnable()){
        errno = ENETUNREACH;//无法传送数据包至指定的主机.
        return -1;
    }

    ret = syscall(__NR_connect, sd, addr, socklen);
    return ret;
}

HOOK_DEF(void, xlogger_Write, void* _info, const char* _log)
{
    slog_wx("%s", _log);

    orig_xlogger_Write(_info, _log);
}

__END_DECLS
// end IO DEF


void onSoLoaded(const char *name, void *handle) {
    /*if(strcmp(name, "/data/user/0/io.virtualapp/virtual/data/app/com.tencent.mm/lib/libwechatxlog.so") == 0) {

            slog("fuck, hook libwechatxlog");
            HOOK_SYMBOL(handle, xlogger_Write);
    }*/
}

int findSymbol(const char *name, const char *libn,
               unsigned long *addr) {
    return find_name(getpid(), name, libn, addr);
}

void hook_dlopen(int api_level) {
    void *symbol = NULL;
    if (api_level > 25) {
        if (findSymbol("__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv", "linker",
                       (unsigned long *) &symbol) == 0) {
            MSHookFunction(symbol, (void *) new_do_dlopen_V26,
                           (void **) &orig_do_dlopen_V26);
        }
    } else if (api_level > 23) {
        if (findSymbol("__dl__Z9do_dlopenPKciPK17android_dlextinfoPv", "linker",
                       (unsigned long *) &symbol) == 0) {
            MSHookFunction(symbol, (void *) new_do_dlopen_V24,
                           (void **) &orig_do_dlopen_V24);
        }
    } else if (api_level >= 19) {
        if (findSymbol("__dl__Z9do_dlopenPKciPK17android_dlextinfo", "linker",
                       (unsigned long *) &symbol) == 0) {
            MSHookFunction(symbol, (void *) new_do_dlopen_V19,
                           (void **) &orig_do_dlopen_V19);
        }
    } else {
        if (findSymbol("__dl_dlopen", "linker",
                       (unsigned long *) &symbol) == 0) {
            MSHookFunction(symbol, (void *) new_dlopen, (void **) &orig_dlopen);
        }
    }
}


void IOUniformer::startUniformer(const char *so_path, int api_level, int preview_api_level,int need_dlopen) {
    bool ret = ff_Recognizer::getFFR().init(getMagicPath());
    LOGE("FFR path %s init %s", getMagicPath(), ret ? "success" : "fail");
    char api_level_chars[5];
    setenv("V_SO_PATH", so_path, 1);
    sprintf(api_level_chars, "%i", need_dlopen);
    setenv("V_NEED_DLOPEN", api_level_chars, 1);
    sprintf(api_level_chars, "%i", api_level);
    setenv("V_API_LEVEL", api_level_chars, 1);
    sprintf(api_level_chars, "%i", preview_api_level);
    setenv("V_PREVIEW_API_LEVEL", api_level_chars, 1);

    void *handle = dlopen("libc.so", RTLD_NOW);
    if (handle) {
        HOOK_SYMBOL(handle, faccessat);
        HOOK_SYMBOL(handle, __openat);
        HOOK_SYMBOL(handle, fchmodat);
        HOOK_SYMBOL(handle, fchownat);
        HOOK_SYMBOL(handle, renameat);
        HOOK_SYMBOL(handle, fstatat64);
        HOOK_SYMBOL(handle, __statfs);
        HOOK_SYMBOL(handle, __statfs64);
        HOOK_SYMBOL(handle, mkdirat);
        HOOK_SYMBOL(handle, mknodat);
        HOOK_SYMBOL(handle, truncate);
        HOOK_SYMBOL(handle, linkat);
        HOOK_SYMBOL(handle, readlinkat);
        HOOK_SYMBOL(handle, unlinkat);
        HOOK_SYMBOL(handle, symlinkat);
        HOOK_SYMBOL(handle, utimensat);
        HOOK_SYMBOL(handle, __getcwd);
//        HOOK_SYMBOL(handle, __getdents64);
        HOOK_SYMBOL(handle, chdir);
        HOOK_SYMBOL(handle, execve);
        HOOK_SYMBOL(handle, close);
        HOOK_SYMBOL(handle, read);
        HOOK_SYMBOL(handle, write);
        HOOK_SYMBOL(handle, __mmap2);
        HOOK_SYMBOL(handle, munmap);
        HOOK_SYMBOL(handle, pread64);
        HOOK_SYMBOL(handle, pwrite64);
        HOOK_SYMBOL(handle, fstat);
        HOOK_SYMBOL(handle, __llseek);
        HOOK_SYMBOL(handle, lseek);
        HOOK_SYMBOL(handle, ftruncate64);
        HOOK_SYMBOL(handle, sendfile);
        HOOK_SYMBOL(handle, sendfile64);
        HOOK_SYMBOL(handle, dup);
        HOOK_SYMBOL(handle, dup3);
        HOOK_SYMBOL(handle, connect);
        HOOK_SYMBOL(handle, msync);
        if (api_level <= 20) {
            HOOK_SYMBOL(handle, access);
            HOOK_SYMBOL(handle, __open);
            HOOK_SYMBOL(handle, stat);
            HOOK_SYMBOL(handle, lstat);
            HOOK_SYMBOL(handle, fstatat);
            HOOK_SYMBOL(handle, chmod);
            HOOK_SYMBOL(handle, chown);
            HOOK_SYMBOL(handle, rename);
            HOOK_SYMBOL(handle, rmdir);
            HOOK_SYMBOL(handle, mkdir);
            HOOK_SYMBOL(handle, mknod);
            HOOK_SYMBOL(handle, link);
            HOOK_SYMBOL(handle, unlink);
            HOOK_SYMBOL(handle, readlink);
            HOOK_SYMBOL(handle, symlink);
//            HOOK_SYMBOL(handle, getdents);
//            HOOK_SYMBOL(handle, execv);
        }
        dlclose(handle);
    }

    if(need_dlopen == 1){
        hook_dlopen(api_level);
    }

    originalInterface::original_lseek = orig_lseek;
    originalInterface::original_llseek = orig___llseek;
    originalInterface::original_fstat = orig_fstat;
    originalInterface::original_pwrite64 = orig_pwrite64;
    originalInterface::original_pread64 = orig_pread64;
    originalInterface::original_ftruncate64 = orig_ftruncate64;
    originalInterface::original_sendfile = orig_sendfile;
}
