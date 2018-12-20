#ifndef MEM_H
#define MEM_H

#include "tracer.h"

int write_data(Tracer *tracee, word_t dest_tracee, const void *src_tracer, word_t size);

int writev_data(Tracer *tracee, word_t dest_tracee, const struct iovec *src_tracer,
                int src_tracer_count);

int read_data(const Tracer *tracee, void *dest_tracer, word_t src_tracee, word_t size);

int read_string(const Tracer *tracee, char *dest_tracer, word_t src_tracee, word_t max_size);

word_t alloc_mem(Tracer *tracee, ssize_t size);

static inline int read_path(const Tracer *tracee, char dest_tracer[PATH_MAX], word_t src_tracee) {
    int status;

    status = read_string(tracee, dest_tracer, src_tracee, PATH_MAX);
    if (status < 0)
        return status;
    if (status >= PATH_MAX)
        return -ENAMETOOLONG;

    return status;
}

#endif //MEM_H
