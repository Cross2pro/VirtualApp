#ifndef TRACER_PATH_H
#define TRACER_PATH_H

#include <climits>
#include "tracer.h"

/* File type.  */
typedef enum {
    REGULAR,
    SYMLINK,
} Type;

int translate_path(Tracer *tracee, char host_path[PATH_MAX],
                          int dir_fd, const char *guest_path, bool deref_final);

#endif //TRACER_PATH_H
