#include <climits>
#include <syscall/sysnum.h>
#include <Foundation/SandboxFs.h>
#include <Foundation/Log.h>
#include <linux/fcntl.h>
#include <syscall/compat.h>
#include <linux/sched.h>
#include "enter.h"
#include "reg.h"
#include "syscall.h"
#include "path.h"

static int translate_path2(Tracer *tracee, int dir_fd, char path[PATH_MAX], Reg reg, Type type) {
    char temp[PATH_MAX];
    if (dir_fd != AT_FDCWD) {
        return 0;
    }
    const char *redirect_path = relocate_path(path, temp, sizeof(temp));
    if (redirect_path != path) {
        set_sysarg_path(tracee, redirect_path, reg);
    }
    return 0;
}

static int translate_sysarg(Tracer *tracee, Reg reg, Type type) {
    char old_path[PATH_MAX];
    int status;

    /* Extract the original path. */
    status = get_sysarg_path(tracee, old_path, reg);
    if (status < 0)
        return status;

    return translate_path2(tracee, AT_FDCWD, old_path, reg, type);
}

#pragma clang diagnostic ignored "-Wconversion"

int translate_syscall_enter(Tracer *tracee) {
    int flags;
    int dirfd;
    int olddirfd;
    int newdirfd;

    int status;
    int status2;

    char path[PATH_MAX];
    char oldpath[PATH_MAX];
    char newpath[PATH_MAX];

    Sysnum syscall_number;

    syscall_number = get_sysnum(tracee, ORIGINAL);
    switch (syscall_number) {
        case SC_fchdir:
        case SC_chdir: {
            // TODO: support it
            status = 0;
            break;
        }
        case SC_access:
        case SC_acct:
        case SC_chmod:
        case SC_chown:
        case SC_chown32:
        case SC_chroot:
        case SC_getxattr:
        case SC_listxattr:
        case SC_mknod:
        case SC_oldstat:
        case SC_creat:
        case SC_removexattr:
        case SC_setxattr:
        case SC_stat:
        case SC_stat64:
        case SC_statfs:
        case SC_statfs64:
        case SC_swapoff:
        case SC_swapon:
        case SC_truncate:
        case SC_truncate64:
        case SC_umount:
        case SC_umount2:
        case SC_uselib:
        case SC_utime:
        case SC_utimes:
            status = translate_sysarg(tracee, SYSARG_1, REGULAR);
            break;

        case SC_open:
            flags = peek_reg(tracee, CURRENT, SYSARG_2);

            if (((flags & O_NOFOLLOW) != 0)
                || ((flags & O_EXCL) != 0 && (flags & O_CREAT) != 0))
                status = translate_sysarg(tracee, SYSARG_1, SYMLINK);
            else
                status = translate_sysarg(tracee, SYSARG_1, REGULAR);
            break;

        case SC_fchownat:
        case SC_fstatat64:
        case SC_newfstatat:
        case SC_utimensat:
        case SC_name_to_handle_at:
            dirfd = peek_reg(tracee, CURRENT, SYSARG_1);

            status = get_sysarg_path(tracee, path, SYSARG_2);
            if (status < 0)
                break;

            flags = (syscall_number == SC_fchownat
                     || syscall_number == SC_name_to_handle_at)
                    ? peek_reg(tracee, CURRENT, SYSARG_5)
                    : peek_reg(tracee, CURRENT, SYSARG_4);

            if ((flags & AT_SYMLINK_NOFOLLOW) != 0)
                status = translate_path2(tracee, dirfd, path, SYSARG_2, SYMLINK);
            else
                status = translate_path2(tracee, dirfd, path, SYSARG_2, REGULAR);
            break;

        case SC_fchmodat:
        case SC_faccessat:
        case SC_futimesat:
        case SC_mknodat:
            dirfd = peek_reg(tracee, CURRENT, SYSARG_1);

            status = get_sysarg_path(tracee, path, SYSARG_2);
            if (status < 0)
                break;

            status = translate_path2(tracee, dirfd, path, SYSARG_2, REGULAR);
            break;

        case SC_inotify_add_watch:
            flags = peek_reg(tracee, CURRENT, SYSARG_3);

            if ((flags & IN_DONT_FOLLOW) != 0)
                status = translate_sysarg(tracee, SYSARG_2, SYMLINK);
            else
                status = translate_sysarg(tracee, SYSARG_2, REGULAR);
            break;
        case SC_readlink:
        case SC_lchown:
        case SC_lchown32:
        case SC_lgetxattr:
        case SC_llistxattr:
        case SC_lremovexattr:
        case SC_lsetxattr:
        case SC_lstat:
        case SC_lstat64:
        case SC_oldlstat:
        case SC_unlink:
        case SC_rmdir:
        case SC_mkdir:
            status = translate_sysarg(tracee, SYSARG_1, SYMLINK);
            break;
        case SC_linkat:
            olddirfd = peek_reg(tracee, CURRENT, SYSARG_1);
            newdirfd = peek_reg(tracee, CURRENT, SYSARG_3);
            flags = peek_reg(tracee, CURRENT, SYSARG_5);

            status = get_sysarg_path(tracee, oldpath, SYSARG_2);
            if (status < 0)
                break;

            status = get_sysarg_path(tracee, newpath, SYSARG_4);
            if (status < 0)
                break;

            if ((flags & AT_SYMLINK_FOLLOW) != 0)
                status = translate_path2(tracee, olddirfd, oldpath, SYSARG_2, REGULAR);
            else
                status = translate_path2(tracee, olddirfd, oldpath, SYSARG_2, SYMLINK);
            if (status < 0)
                break;

            status = translate_path2(tracee, newdirfd, newpath, SYSARG_4, SYMLINK);
            break;
        case SC_openat:
            dirfd = peek_reg(tracee, CURRENT, SYSARG_1);
            flags = peek_reg(tracee, CURRENT, SYSARG_3);

            status = get_sysarg_path(tracee, path, SYSARG_2);
            if (status < 0)
                break;

            if (((flags & O_NOFOLLOW) != 0)
                || ((flags & O_EXCL) != 0 && (flags & O_CREAT) != 0))
                status = translate_path2(tracee, dirfd, path, SYSARG_2, SYMLINK);
            else
                status = translate_path2(tracee, dirfd, path, SYSARG_2, REGULAR);
            break;

        case SC_readlinkat:
        case SC_unlinkat:
        case SC_mkdirat:
            dirfd = peek_reg(tracee, CURRENT, SYSARG_1);

            status = get_sysarg_path(tracee, path, SYSARG_2);
            if (status < 0)
                break;

            status = translate_path2(tracee, dirfd, path, SYSARG_2, SYMLINK);
            break;
        case SC_link:
        case SC_rename:
            status = translate_sysarg(tracee, SYSARG_1, SYMLINK);
            if (status < 0)
                break;

            status = translate_sysarg(tracee, SYSARG_2, SYMLINK);
            break;

        case SC_renameat:
        case SC_renameat2:
            olddirfd = peek_reg(tracee, CURRENT, SYSARG_1);
            newdirfd = peek_reg(tracee, CURRENT, SYSARG_3);

            status = get_sysarg_path(tracee, oldpath, SYSARG_2);
            if (status < 0)
                break;

            status = get_sysarg_path(tracee, newpath, SYSARG_4);
            if (status < 0)
                break;

            status = translate_path2(tracee, olddirfd, oldpath, SYSARG_2, SYMLINK);
            if (status < 0)
                break;

            status = translate_path2(tracee, newdirfd, newpath, SYSARG_4, SYMLINK);
            break;

        case SC_symlink:
            status = translate_sysarg(tracee, SYSARG_2, SYMLINK);
            break;

        case SC_symlinkat:
            newdirfd = peek_reg(tracee, CURRENT, SYSARG_2);

            status = get_sysarg_path(tracee, newpath, SYSARG_3);
            if (status < 0)
                break;

            status = translate_path2(tracee, newdirfd, newpath, SYSARG_3, SYMLINK);
            break;
        default: {
            status = 0;
            break;
        }

    }
    return status;
}

#pragma clang diagnostic pop