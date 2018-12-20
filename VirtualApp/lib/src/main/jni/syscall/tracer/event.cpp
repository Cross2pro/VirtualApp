#include <unistd.h>
#include <string.h>
#include <Foundation/Log.h>
#include <linux/prctl.h>
#include <sys/prctl.h>
#include <linux/wait.h>
#include <wait.h>
#include <stdlib.h>
#include <assert.h>
#include <errno.h>
#include <linux/sched.h>
#include <syscall/sysnum.h>
#include <fcntl.h>
#include <Foundation/SandboxFs.h>
#include "event.h"
#include "tracer.h"
#include "syscall.h"
#include "reg.h"

#define PARENT_EXE_NAME "zygote"

static bool g_use_process_vm_api = false;

int event_loop();

int handle_tracee_event(Tracer *tracee, int tracee_status);

bool restart_tracee(Tracer *tracee, int signal);

bool use_process_vm_api() {
    return g_use_process_vm_api;
}

int trace_current_process(int sdkVersion) {
    g_use_process_vm_api = sdkVersion >= 23;
    prctl(PR_SET_DUMPABLE, 1, 0, 0, 0);
    pid_t pid = getpid();
    pid_t child = fork();
    if (child < 0) {
        ALOGE("error: fork()");
        return -errno;
    }
    if (child == 0) {
        ALOGE("enter child process");
        // we use this child process attach the parent process
        int status = ptrace(PTRACE_ATTACH, pid, NULL, NULL);
        if (status != 0) {
            ALOGE("error: attach target process");
            return -errno;
        }
        Tracer *first = get_tracer(NULL, pid, true);
        first->wait_sigcont = true;
        first->exe = strdup(PARENT_EXE_NAME);
        exit(event_loop());
    } else {
        kill(getpid(), SIGSTOP);
        ALOGE("attached process: %d", pid);
    }
    return 0;
}

static int last_exit_status = -1;

int event_loop() {
    while (1) {
        int tracee_status;
        Tracer *tracee;
        int signal;
        pid_t pid;
        free_terminated_tracees();
        pid = waitpid(-1, &tracee_status, 0);
        if (pid < 0) {
            ALOGE("error: waitpid()");
            if (errno != ECHILD) {
                return EXIT_FAILURE;
            }
            break;
        }
        tracee = get_tracer(NULL, pid, true);
        if (tracee->vpid == 1) {
            static bool option_seted = false;
            const unsigned long default_ptrace_options = (
                    PTRACE_O_TRACESYSGOOD |
//                    PTRACE_O_TRACEFORK |
//                    PTRACE_O_TRACEVFORK |
//                    PTRACE_O_TRACEVFORKDONE |
//                    PTRACE_O_TRACEEXEC |
                    PTRACE_O_TRACECLONE |
                    PTRACE_O_TRACEEXIT
            );
            if (!option_seted) {
                if (ptrace(PTRACE_SETOPTIONS, pid, NULL,
                           default_ptrace_options) < 0) {
                    ALOGE("error: set_ptrace_options");
                }
                option_seted = true;
            }
        }
        signal = handle_tracee_event(tracee, tracee_status);
        (void) restart_tracee(tracee, signal);
    }
    return last_exit_status;
}

int handle_tracee_event(Tracer *tracee, int tracee_status) {
    int signal;
    if (tracee->restart_how == 0) {
        tracee->restart_how = PTRACE_SYSCALL;
    }
    /* Not a signal-stop by default.  */
    signal = 0;
    if (WIFEXITED(tracee_status)) {
        last_exit_status = WEXITSTATUS(tracee_status);
        ALOGE("[%d] exit with status: %d", tracee->pid, tracee_status);
        terminate_tracee(tracee);
    } else if (WIFSIGNALED(tracee_status)) {
        ALOGE("[%d] exit with signal: %d", tracee->pid, WTERMSIG(tracee_status));
        terminate_tracee(tracee);
    } else if (WIFSTOPPED(tracee_status)) {
        signal = (tracee_status & 0xfff00) >> 8;
        switch (signal) {
            case SIGTRAP | 0x80:
                signal = 0;
//                translate_syscall(tracee);
                break;
            case SIGTRAP | PTRACE_EVENT_VFORK << 8:
                signal = 0;
                (void) new_child(tracee, CLONE_VFORK);
                break;

            case SIGTRAP | PTRACE_EVENT_FORK << 8:
            case SIGTRAP | PTRACE_EVENT_CLONE << 8:
                signal = 0;
                (void) new_child(tracee, 0);
                break;

            case SIGTRAP | PTRACE_EVENT_VFORK_DONE << 8:
            case SIGTRAP | PTRACE_EVENT_EXEC << 8:
            case SIGTRAP | PTRACE_EVENT_EXIT << 8:
                signal = 0;
                if (tracee->last_restart_how) {
                    tracee->restart_how = tracee->last_restart_how;
                }
                break;
            case SIGSTOP: {
                if (tracee->sigstop == Tracer::SIGSTOP_IGNORED) {
                    if (tracee->wait_sigcont) {
                        tracee->wait_sigcont = false;
                        kill(tracee->pid, SIGCONT);
                        ALOGE("resume process: %d", tracee->pid);
                        signal = 0;
                    } else {
                        tracee->sigstop = Tracer::SIGSTOP_ALLOWED;
                        signal = 0;
                    }
                }
                break;
            }

            default:
                break;
        }
    }
    return signal;
}

bool restart_tracee(Tracer *tracee, int signal) {
    int status;

    /* Restart the tracee and stop it at the next instruction, or
     * at the next entry or exit of a system call. */
    assert(tracee->restart_how != 0);
    status = ptrace(tracee->restart_how, tracee->pid, 0, signal);
    if (status < 0)
        return false; /* The process likely died in a syscall.  */

    tracee->last_restart_how = tracee->restart_how;
    tracee->restart_how = 0;

    return true;
}