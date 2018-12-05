//
// VirtualApp Native Project
//

#ifndef FOUNDATION_PATH
#define FOUNDATION_PATH

#include <unistd.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>
#include <sys/stat.h>
#include <syscall.h>

char *canonicalize_filename(const char *filename);

char *canonicalize_filename(const char *filename,
                            const char *relative_to);

#endif //FOUNDATION_PATH
