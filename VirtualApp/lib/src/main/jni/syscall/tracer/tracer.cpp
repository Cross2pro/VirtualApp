#include "tracer.h"
#include "reg.h"
#include "event.h"
#include <sys/queue.h>
#include <cstdlib>
#include <signal.h>
#include <Foundation/Log.h>
#include <syscall/sysnum.h>
#include <linux/sched.h>
#include <errno.h>

typedef LIST_HEAD(tracees, tracer) Tracees;
static Tracees tracees;
static uint64_t next_vpid = 1;

/**
 * Remove @zombie from its parent's list of zombies.  Note: this is a
 * talloc destructor.
 */
static int remove_zombie(Tracer *zombie) {
    LIST_REMOVE(zombie, link);
    free(zombie);
    return 0;
}

static int remove_tracer(Tracer *tracer) {
    LIST_REMOVE(tracer, link);
    free(tracer);
    return 0;
}


Tracer *new_dummy_tracer() {
    Tracer *tracer = (Tracer *) calloc(1, sizeof(Tracer));
    if (tracer == NULL)
        return NULL;

    return tracer;
}

static Tracer *new_tracer(pid_t pid) {
    Tracer *tracer;

    tracer = new_dummy_tracer();
    if (tracer == NULL)
        return NULL;


    tracer->pid = pid;
    tracer->vpid = next_vpid++;

    LIST_INSERT_HEAD(&tracees, tracer, link);

    return tracer;
}

Tracer *get_tracer(const Tracer *current_tracee, pid_t pid, bool create) {
    Tracer *tracee;

    /* Don't reset the memory collector if the searched tracee is
     * the current one: there's likely pointers to the
     * sub-allocated data in the caller.  */
    if (current_tracee != NULL && current_tracee->pid == pid)
        return (Tracer *) current_tracee;

    LIST_FOREACH(tracee, &tracees, link) {
        if (tracee->pid == pid) {
            return tracee;
        }
    }

    return (create ? new_tracer(pid) : NULL);
}

/**
 * Mark tracee as terminated and optionally take action.
 */
void terminate_tracee(Tracer *tracee) {
    tracee->terminated = true;

    /* Case where the terminated tracee is marked
       to kill all tracees on exit.
    */
    if (tracee->killall_on_exit) {
        ALOGE("terminating all tracees on exit");
        kill_all_tracees();
    }
}

/**
 * Free all tracees marked as terminated.
 */
void free_terminated_tracees() {
    Tracer *next;

    /* Items can't be deleted when using LIST_FOREACH.  */
    next = tracees.lh_first;
    while (next != NULL) {
        Tracer *tracee = next;
        next = tracee->link.le_next;

        if (tracee->terminated)
            remove_tracer(tracee);
    }
}

/* Send the KILL signal to all tracees.  */
void kill_all_tracees() {
    Tracer *tracee;
    LIST_FOREACH(tracee, &tracees, link)kill(tracee->pid, SIGKILL);
}

int new_child(Tracer *parent, word_t clone_flags) {
    pid_t pid;
    Tracer *child;
    int status;
    status = fetch_regs(parent);
    if (status >= 0 && get_sysnum(parent, CURRENT) == SC_clone)
        clone_flags = peek_reg(parent, CURRENT, SYSARG_1);

    /* Get the pid of the parent's new child.  */
    status = ptrace(PTRACE_GETEVENTMSG, parent->pid, NULL, &pid);
    if (status < 0 || pid == 0) {
        ALOGE("error: ptrace(GETEVENTMSG)");
        return status;
    }
    child = get_tracer(parent, (pid_t) pid, true);
    if (child == NULL) {
        ALOGE("error: get child");
        return -ENOMEM;
    }
    if ((clone_flags & CLONE_PARENT) != 0)
        child->parent = parent->parent;
    else
        child->parent = parent;

    child->clone = ((clone_flags & CLONE_THREAD) != 0);
    child->exe = parent->exe;
    if (child->sigstop == Tracer::SIGSTOP_PENDING) {
        child->sigstop = Tracer::SIGSTOP_ALLOWED;
        restart_tracee(child, 0);
    }
    return 0;
}