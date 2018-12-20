#ifndef SYS_NUM_H
#define SYS_NUM_H

#include <syscall/tracer/tracer.h>
#include "abi.h"

#define SYSNUM(item) SC_ ## item,

typedef enum {
    SC_void = 0,

#    include "sysnums.list"

    SC_NB_SYSNUM
} Sysnum;

Sysnum get_sysnum(const Tracer *tracee, RegVersion version);

extern void set_sysnum(Tracer *tracee, Sysnum sysnum);

extern word_t detranslate_sysnum(Abi abi, Sysnum sysnum);

const char *stringify_sysnum(Sysnum sysnum);

#undef SYSNUM

#endif //SYS_NUM_H
