#ifndef TRACER_H
#define TRACER_H

#include <arch.h>
#include <sys/queue.h>

typedef enum {
    CURRENT = 0,
    ORIGINAL = 1,
    MODIFIED = 2,
    NB_REG_VERSION
} RegVersion;

#define IS_IN_SYSENTER(tracee) ((tracee)->status == 0)
#define IS_IN_SYSEXIT(tracee) (!IS_IN_SYSENTER(tracee))
#define IS_IN_SYSEXIT2(tracee, sysnum) (IS_IN_SYSEXIT(tracee) \
                     && get_sysnum((tracee), ORIGINAL) == sysnum)


typedef struct tracer {
    /* Link for the list of all tracees.  */
    LIST_ENTRY(tracer) link;
    pid_t pid;
    /* Unique tracee identifier. */
    uint64_t vpid;
    struct user_regs_struct _regs[NB_REG_VERSION];
    int restart_how, last_restart_how;
    bool _regs_were_changed;
    bool restore_original_regs;
    /*
     * Current status:
     *        0: enter syscall
     *        1: exit syscall no error
     *   -errno: exit syscall with error.
     */
    int status;
    /* Parent of this tracee, NULL if none.  */
    struct tracer *parent;
    /* Is this tracee ready to be freed?
     * dedicated to terminated tracees instead.  */
    bool terminated;

    /* Whether termination of this tracee implies an immediate kill
     * of all tracees. */
    bool killall_on_exit;

    /* Is it a "clone", i.e has the same parent as its creator.  */
    bool clone;

    /* Path to the executable  */
    char *exe;
    char *new_exe;

    /* State for the special handling of SIGSTOP.  */
    enum {
        SIGSTOP_IGNORED = 0,  /* Ignore SIGSTOP (once the parent is known).  */
        SIGSTOP_ALLOWED,      /* Allow SIGSTOP (once the parent is known).   */
        SIGSTOP_PENDING,      /* Block SIGSTOP until the parent is unknown.  */
    } sigstop;

    bool wait_sigcont;
} Tracer;

Tracer *get_tracer(const Tracer *current_tracee, pid_t pid, bool create);

int new_child(Tracer *parent, word_t clone_flags);

extern void terminate_tracee(Tracer *tracee);

extern void free_terminated_tracees();

extern void kill_all_tracees();

#endif //TRACER_H
