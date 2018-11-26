#include "Path.h"
#include <unistd.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>


#ifdef PATH_DEBUG
#include <fb/include/fb/ALog.h>
#endif

int copy_cwd(char *resolved_path, int index) {
    char buffer[PATH_MAX];
    char *cwd = getcwd(buffer, PATH_MAX);
    if (cwd == NULL) {
        cwd = realpath(".", buffer);
    }
    if (cwd == NULL) {
        resolved_path[index++] = '.';
    } else {
        for (int j = 0; cwd[j]; ++j) {
            resolved_path[index++] = cwd[j];
        }
    }
    if (resolved_path[index - 1] != '/') {
        resolved_path[index++] = '/';
    }
    return index;
}

char *
canonicalize_filename(const char *pathname) {
    char *resolved_path = (char *) malloc(PATH_MAX);
    memset(resolved_path, 0, PATH_MAX);
    int index = 0;
    char prev_c = '\0';
    for (int i = 0; pathname[i] != NULL; ++i) {
        char c = pathname[i];
        char next_c = pathname[i + 1];
        switch (c) {
            case '/':
                if (prev_c != '/') {
                    resolved_path[index++] = c;
                }
                if (next_c == '/') {
                    while (pathname[++i] == '/');
                    i--;
                }
                break;
            case '.': {
                if (prev_c && prev_c != '/') {
                    resolved_path[index++] = c;
                    break;
                }
                if ((next_c == '/' || !next_c)) {
                    if (i == 0) {
                        index = copy_cwd(resolved_path, index);
                        if (next_c == '/') {
                            i++;
                            c = '/';
                        }
                    } else {
                        // eat '.' and next '/' if exist
                        if (next_c) {
                            i++;
                            c = '/';
                        }
                    }
                    break;
                } else if (next_c == '.'
                           && (pathname[i + 2] == '/' || pathname[i + 2] == '\0')) {
                    if (i == 0) {
                        index = copy_cwd(resolved_path, index);
                    }
                    int count = 0;
                    int inner_index = i + 2;
                    do {
                        while (pathname[inner_index++] == '/');
                        inner_index--;
                        count++;
                    } while (pathname[inner_index++] == '.' && pathname[inner_index++] == '.');

                    while (count-- > 0) {
                        while (resolved_path[index - 1] == '/') {
                            resolved_path[index - 1] = '\0';
                            index--;
                        }
                        char *last_slash = strrchr(resolved_path, '/');
                        size_t len = strlen(last_slash);
                        while (len-- > 0) {
                            resolved_path[index-- - 1] = '\0';
                        }
                    }
                    if (resolved_path[index - 1] != '/') {
                        resolved_path[index++] = '/';
                    }
                    i = inner_index - 2;
                    c = '/';
                } else {
                    resolved_path[index++] = c;
                }
                break;
            }
            default: {
                if (index == 0) {
                    index = copy_cwd(resolved_path, index);
                    if (resolved_path[index - 1] != '/') {
                        resolved_path[index++] = '/';
                    }
                }
                resolved_path[index++] = c;
                break;
            }
        }
        prev_c = c;
    }
    resolved_path[index] = '\0';
    return resolved_path;
}
