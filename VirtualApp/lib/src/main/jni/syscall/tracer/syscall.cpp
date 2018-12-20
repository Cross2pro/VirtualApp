#include <Foundation/Log.h>
#include <syscall/sysnum.h>
#include <cerrno>
#include <cstring>
#include <climits>
#include "syscall.h"
#include "reg.h"
#include "mem.h"
#include "enter.h"
#include "exit.h"


/**
 * Copy in @path a C string (PATH_MAX bytes max.) from the @tracee's
 * memory address space pointed to by the @reg argument of the
 * current syscall.  This function returns -errno if an error occured,
 * otherwise it returns the size in bytes put into the @path.
 */
int get_sysarg_path(const Tracer *tracee, char path[PATH_MAX], Reg reg) {
    int size;
    word_t src;

    src = peek_reg(tracee, CURRENT, reg);

    /* Check if the parameter is not NULL. Technically we should
     * not return an -EFAULT for this special value since it is
     * allowed for some syscall, utimensat(2) for instance. */
    if (src == 0) {
        path[0] = '\0';
        return 0;
    }

    /* Get the path from the tracee's memory space. */
    size = read_path(tracee, path, src);
    if (size < 0)
        return size;

    path[size] = '\0';
    return size;
}

/**
 * Copy @size bytes of the data pointed to by @tracer_ptr into a
 * @tracee's memory block and make the @reg argument of the current
 * syscall points to this new block.  This function returns -errno if
 * an error occured, otherwise 0.
 */
int set_sysarg_data(Tracer *tracee, const void *tracer_ptr, word_t size, Reg reg) {
    word_t tracee_ptr;
    int status;

    /* Allocate space into the tracee's memory to host the new data. */
    tracee_ptr = alloc_mem(tracee, size);
    if (tracee_ptr == 0)
        return -EFAULT;

    /* Copy the new data into the previously allocated space. */
    status = write_data(tracee, tracee_ptr, tracer_ptr, size);
    if (status < 0)
        return status;

    /* Make this argument point to the new data. */
    poke_reg(tracee, reg, tracee_ptr);

    return 0;
}

/**
 * Copy @path to a @tracee's memory block and make the @reg argument
 * of the current syscall points to this new block.  This function
 * returns -errno if an error occured, otherwise 0.
 */
int set_sysarg_path(Tracer *tracee, const char path[PATH_MAX], Reg reg) {
    return set_sysarg_data(tracee, path, strlen(path) + 1, reg);
}



void translate_syscall(Tracer *tracee) {
    const bool is_enter_stage = IS_IN_SYSENTER(tracee);
    int status;
    status = fetch_regs(tracee);
    if (status < 0)
        return;
    Sysnum sysnum = get_sysnum(tracee, CURRENT);
    if (is_enter_stage) {
        tracee->restore_original_regs = false;
        save_current_regs(tracee, ORIGINAL);
        status = translate_syscall_enter(tracee);
        save_current_regs(tracee, MODIFIED);

        /* Remember the tracee status for the "exit" stage and
         * avoid the actual syscall if an error was reported
         * by the translation/extension. */
        if (status < 0) {
            set_sysnum(tracee, SC_void);
            poke_reg(tracee, SYSARG_RESULT, (word_t) status);
            tracee->status = status;
#if defined(ARCH_ARM_EABI)
            tracee->restart_how = PTRACE_SYSCALL;
#endif
        } else {
            tracee->status = 1;
        }
    } else {
        /* By default, restore original register values at the
		 * end of this stage.  */
        tracee->restore_original_regs = true;
        translate_syscall_exit(tracee);
        /* Reset the tracee's status. */
        tracee->status = 0;
    }
    bool override_sysnum = is_enter_stage;
    int push_regs_status = push_specific_regs(tracee, override_sysnum);
    /* Handle inability to change syscall number */
    if (push_regs_status < 0) {
        ALOGE("error: modifiy reg: %s", stringify_sysnum(sysnum));
    }
}