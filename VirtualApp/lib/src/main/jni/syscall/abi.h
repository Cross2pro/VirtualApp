#ifndef ABI_H
#define ABI_H

#include <syscall/tracer/tracer.h>
#include "arch.h"
#include "attribute.h"

typedef enum {
    ABI_DEFAULT = 0,
    ABI_2, /* x86_32 on x86_64.  */
    ABI_3, /* x32 on x86_64.  */
    NB_MAX_ABIS,
} Abi;

/**
 * Return the ABI currently used by the given @tracee.
 */
#if defined(ARCH_X86_64)
static inline Abi get_abi(const Tracer *tracee)
{
    /* The ABI can be changed by a syscall ("execve" typically),
     * however the change is only effective once the syscall has
     * *fully* returned, hence the use of _regs[ORIGINAL].  */
    switch (tracee->_regs[ORIGINAL].cs) {
    case 0x23:
        return ABI_2;

    case 0x33:
        if (tracee->_regs[ORIGINAL].ds == 0x2B)
            return ABI_3;
        /* Fall through.  */
    default:
        return ABI_DEFAULT;
    }
}

/**
 * Return true if @tracee is a 32-bit process running on a 64-bit
 * kernel.
 */
static inline bool is_32on64_mode(const Tracer *tracee)
{
    /* Unlike the ABI, 32-bit/64-bit mode change is effective
     * immediately, hence _regs[CURRENT].cs.  */
    switch (tracee->_regs[CURRENT].cs) {
    case 0x23:
        return true;

    case 0x33:
        if (tracee->_regs[CURRENT].ds == 0x2B)
            return true;
        /* Fall through.  */
    default:
        return false;
    }
}
#else

static inline Abi get_abi(const Tracer *tracee UNUSED) {
    return ABI_DEFAULT;
}

static inline bool is_32on64_mode(const Tracer *tracee UNUSED) {
    return false;
}

#endif


#endif ABI_H