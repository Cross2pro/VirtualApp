#ifndef ARCH_H
#define ARCH_H

#include <sys/ptrace.h>
#include <sys/user.h>

typedef unsigned long word_t;
typedef unsigned char byte_t;

#define SYSCALL_AVOIDER ((word_t) -2)
#define SYSTRAP_NUM SYSARG_NUM

#if !defined(ARCH_X86_64) && !defined(ARCH_ARM_EABI) && !defined(ARCH_X86) && !defined(ARCH_SH4)
#    if defined(__x86_64__)
#         define SYSNUMS_HEADER1 "syscall/sysnums-x86_64.h"
#         define SYSNUMS_HEADER2 "syscall/sysnums-i386.h"
#         define SYSNUMS_HEADER3 "syscall/sysnums-x32.h"
#         define ARCH_X86_64 1
#         define RED_ZONE_SIZE 128
#    elif defined(__ARM_EABI__)
#         define SYSNUMS_HEADER1 "syscall/sysnums-arm.h"
#         define ARCH_ARM_EABI 1
#         define RED_ZONE_SIZE 0
#    elif defined(__aarch64__)
#         define SYSNUMS_HEADER1 "syscall/sysnums-arm64.h"
#         define ARCH_ARM64 1
#         define RED_ZONE_SIZE 0
#    elif defined(__i386__)
#         define SYSNUMS_HEADER1 "syscall/sysnums-i386.h"
#         define ARCH_X86 1
#         define RED_ZONE_SIZE 0
#    else
#         error "Unsupported architecture"
#    endif
#endif

#if defined(ARCH_X86_64)
#    define SYSNUMS_ABI1 sysnums_x86_64
#    define SYSNUMS_ABI2 sysnums_i386
#    define SYSNUMS_ABI3 sysnums_x32
#elif defined(ARCH_ARM_EABI)
#    define SYSNUMS_ABI1    sysnums_arm
#    define user_regs_struct user_regs
#elif defined(ARCH_ARM64)
#    define SYSNUMS_ABI1    sysnums_arm64
#elif defined(ARCH_X86)
#    define SYSNUMS_ABI1    sysnums_i386
#else
#    error "Unsupported architecture"
#endif

#endif //ARCH_H
