#include <stdlib.h>
#include "SandboxFs.h"
#include "Path.h"
#include "Log.h"

//#define FORCE_CLOSE_NORMALIZE_PATH

PathItem *keep_items;
PathItem *forbidden_items;
PathItem *readonly_items;
ReplaceItem *replace_items;
int keep_item_count;
int forbidden_item_count;
int readonly_item_count;
int replace_item_count;

int add_keep_item(const char *path) {
    char keep_env_name[KEY_MAX];
    sprintf(keep_env_name, "V_KEEP_ITEM_%d", keep_item_count);
    setenv(keep_env_name, path, 1);
    keep_items = (PathItem *) realloc(keep_items,
                                      keep_item_count * sizeof(PathItem) + sizeof(PathItem));
    PathItem &item = keep_items[keep_item_count];
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    return ++keep_item_count;
}


int add_forbidden_item(const char *path) {
    char forbidden_env_name[KEY_MAX];
    sprintf(forbidden_env_name, "V_FORBID_ITEM_%d", forbidden_item_count);
    setenv(forbidden_env_name, path, 1);
    forbidden_items = (PathItem *) realloc(forbidden_items,
                                           forbidden_item_count * sizeof(PathItem) +
                                           sizeof(PathItem));
    PathItem &item = forbidden_items[forbidden_item_count];
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    return ++forbidden_item_count;
}

int add_readonly_item(const char *path) {
    char readonly_env_name[KEY_MAX];
    sprintf(readonly_env_name, "V_READONLY_ITEM_%d", readonly_item_count);
    setenv(readonly_env_name, path, 1);
    readonly_items = (PathItem *) realloc(readonly_items,
                                          readonly_item_count * sizeof(PathItem) +
                                          sizeof(PathItem));
    PathItem &item = readonly_items[readonly_item_count];
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    return ++readonly_item_count;
}

int add_replace_item(const char *orig_path, const char *new_path) {
    ALOGE("add replace item : %s -> %s", orig_path, new_path);
    char src_env_name[KEY_MAX];
    char dst_env_name[KEY_MAX];
    sprintf(src_env_name, "V_REPLACE_ITEM_SRC_%d", replace_item_count);
    sprintf(dst_env_name, "V_REPLACE_ITEM_DST_%d", replace_item_count);
    setenv(src_env_name, orig_path, 1);
    setenv(dst_env_name, new_path, 1);

    replace_items = (ReplaceItem *) realloc(replace_items,
                                            replace_item_count * sizeof(ReplaceItem) +
                                            sizeof(ReplaceItem));
    ReplaceItem &item = replace_items[replace_item_count];
    item.orig_path = strdup(orig_path);
    item.orig_size = strlen(orig_path);
    item.new_path = strdup(new_path);
    item.new_size = strlen(new_path);
    item.is_folder = (orig_path[strlen(orig_path) - 1] == '/');
    return ++replace_item_count;
}

PathItem *get_keep_items() {
    return keep_items;
}

PathItem *get_forbidden_item() {
    return forbidden_items;
}

PathItem *get_readonly_item() {
    return readonly_items;
}

ReplaceItem *get_replace_items() {
    return replace_items;
}

int get_keep_item_count() {
    return keep_item_count;
}

int get_forbidden_item_count() {
    return forbidden_item_count;
}

int get_replace_item_count() {
    return replace_item_count;
}


inline bool
match_path(bool is_folder, size_t size, const char *item_path, const char *path, size_t path_len) {
    if (is_folder) {
        if (path_len < size) {
            // ignore the last '/'
            return strncmp(item_path, path, size - 1) == 0;
        } else {
            return strncmp(item_path, path, size) == 0;
        }
    } else {
        return strcmp(item_path, path) == 0;
    }
}

bool isReadOnly(const char *path) {
    for (int i = 0; i < readonly_item_count; ++i) {
        PathItem &item = readonly_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, strlen(path))) {
            return true;
        }
    }
    return false;
}

const char *relocate_path(const char *path, bool normalize_path) {
#ifdef FORCE_CLOSE_NORMALIZE_PATH
    normalize_path = false;
#endif
    if (NULL == path) {
        return path;
    }
    if (normalize_path) {
        path = canonicalize_filename(path);
    }
    const char *result = path;
    size_t len = strlen(result);

    for (int i = 0; i < keep_item_count; ++i) {
        PathItem &item = keep_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, len)) {
            result = path;
            goto finally;
        }
    }

    for (int i = 0; i < forbidden_item_count; ++i) {
        PathItem &item = forbidden_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, len)) {
            result = NULL;
            goto finally;
        }
    }

    for (int i = 0; i < replace_item_count; ++i) {
        ReplaceItem &item = replace_items[i];
        if (match_path(item.is_folder, item.orig_size, item.orig_path, path, len)) {
            if (len < item.orig_size) {
                //remove last /
                result = strdup(item.new_path);
                goto finally;
            } else {
                char *relocated_path = (char *) malloc(PATH_MAX);
                memset(relocated_path, 0, PATH_MAX);
                strcat(relocated_path, item.new_path);
                strcat(relocated_path, path + item.orig_size);
                result = relocated_path;
                goto finally;
            }
        }
    }
    finally:
    if (normalize_path && result != path) {
        free((void *) path);
    }
    return result;
}

int relocate_path_inplace(char *_path, size_t size, bool normalize_path) {
#ifdef FORCE_CLOSE_NORMALIZE_PATH
    normalize_path = false;
#endif
    int ret = -1;
    const char *redirect_path = relocate_path(_path, normalize_path);
    if (redirect_path) {
        ret = 0;
        if (redirect_path != _path) {
            if (strlen(redirect_path) <= size) {
                strcpy(_path, redirect_path);
            }
        }
    }
    free((void *) redirect_path);
    return ret;
}

const char *reverse_relocate_path(const char *path, bool normalize_path) {
#ifdef FORCE_CLOSE_NORMALIZE_PATH
    normalize_path = false;
#endif
    if (path == NULL) {
        return NULL;
    }
    if (normalize_path) {
        path = canonicalize_filename(path);
    }
    const char *result = path;
    size_t len = strlen(path);
    for (int i = 0; i < keep_item_count; ++i) {
        PathItem &item = keep_items[i];
        if (match_path(item.is_folder, item.size, item.path, path, len)) {
            result = path;
            goto finally;
        }
    }
    for (int i = 0; i < replace_item_count; ++i) {
        ReplaceItem &item = replace_items[i];
        if (match_path(item.is_folder, item.new_size, item.new_path, path, len)) {
            int len = strlen(path);
            if (len < item.new_size) {
                result = strdup(item.orig_path);
                goto finally;
            } else {
                char *reverse_path = (char *) malloc(PATH_MAX);
                memset(reverse_path, 0, PATH_MAX);
                strcat(reverse_path, item.orig_path);
                strcat(reverse_path, path + item.new_size);
                result = reverse_path;
                goto finally;
            }
        }
    }
    finally:
    if (normalize_path && result != path) {
        free((void *) path);
    }
    return result;
}


int reverse_relocate_path_inplace(char *path, size_t size, bool normalize_path) {
#ifdef FORCE_CLOSE_NORMALIZE_PATH
    normalize_path = false;
#endif
    int ret = -1;
    const char *redirect_path = reverse_relocate_path(path, normalize_path);
    if (redirect_path) {
        ret = 0;
        if (redirect_path != path) {
            if (strlen(redirect_path) <= size) {
                strcpy(path, redirect_path);
            }
            free((void *) redirect_path);
        }
    }
    return ret;
}