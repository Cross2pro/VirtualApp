#include <climits>
#include <syscall/sysnum.h>
#include <Foundation/Log.h>
#include "exit.h"
#include "reg.h"
#include "syscall.h"

void translate_syscall_exit(Tracer *tracee) {
    char path[PATH_MAX];
    int status;
    Sysnum syscall_number;
    syscall_number = get_sysnum(tracee, ORIGINAL);
    switch (syscall_number) {


    }
}