#ifndef SYSCALL_H
#define SYSCALL_H

#include "tracer.h"
#include "reg.h"

void translate_syscall(Tracer *tracee);

int get_sysarg_path(const Tracer *tracee, char path[PATH_MAX], Reg reg);

int set_sysarg_path(Tracer *tracee, const char path[PATH_MAX], Reg reg);

int set_sysarg_data(Tracer *tracee, const void *tracer_ptr, word_t size, Reg reg);

#endif //SYSCALL_H
