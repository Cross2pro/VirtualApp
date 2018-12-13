#include "Path.h"

#define DIR_SEPARATOR '/'
#define DIR_SEPARATOR_S "/"
#define IS_DIR_SEPARATOR(c) ((c) == DIR_SEPARATOR || (c) == '/')


const char *
path_skip_root(const char *file_name) {


    /* Skip initial slashes */
    if (IS_DIR_SEPARATOR (file_name[0])) {
        while (IS_DIR_SEPARATOR (file_name[0]))
            file_name++;
        return (char *) file_name;
    }

    return NULL;
}

static char *
build_path_va(const char *separator,
              const char *first_element,
              va_list *args,
              char **str_array) {
    char *result;
    size_t separator_len = strlen(separator);
    bool is_first = true;
    bool have_leading = false;
    const char *single_element = NULL;
    const char *next_element;
    const char *last_trailing = NULL;
    int i = 0;

    result = (char *) malloc(PATH_MAX);
    memset(result, 0, PATH_MAX);

    if (str_array)
        next_element = str_array[i++];
    else
        next_element = first_element;

    while (true) {
        const char *element;
        const char *start;
        const char *end;

        if (next_element) {
            element = next_element;
            if (str_array)
                next_element = str_array[i++];
            else
                next_element = va_arg (*args, char *);
        } else
            break;

        /* Ignore empty elements */
        if (!*element)
            continue;

        start = element;

        if (separator_len) {
            while (strncmp(start, separator, separator_len) == 0)
                start += separator_len;
        }

        end = start + strlen(start);

        if (separator_len) {
            while (end >= start + separator_len &&
                   strncmp(end - separator_len, separator, separator_len) == 0)
                end -= separator_len;

            last_trailing = end;
            while (last_trailing >= element + separator_len &&
                   strncmp(last_trailing - separator_len, separator, separator_len) == 0)
                last_trailing -= separator_len;

            if (!have_leading) {
                /* If the leading and trailing separator strings are in the
                 * same element and overlap, the result is exactly that element
                 */
                if (last_trailing <= start)
                    single_element = element;
                strncat(result, element, start - element);
                have_leading = true;
            } else
                single_element = NULL;
        }

        if (end == start)
            continue;

        if (!is_first)
            strcat(result, separator);

        strncat(result, start, end - start);
        is_first = false;
    }

    if (single_element) {
        free(result);
        return strdup(single_element);
    } else {
        if (last_trailing)
            strcat(result, last_trailing);

        return result;
    }
}

static char *
build_filename_va(const char *first_argument,
                  va_list *args,
                  char **str_array) {
    char *str;

    str = build_path_va(DIR_SEPARATOR_S, first_argument, args, str_array);
    return str;
}

char *
build_filename(const char *first_element,
               ...) {
    char *str;
    va_list args;

    va_start (args, first_element);
    str = build_filename_va(first_element, &args, NULL);
    va_end (args);

    return str;
}

bool
path_is_absolute(const char *file_name) {
    if (IS_DIR_SEPARATOR (file_name[0]))
        return true;
    return false;
}

char *
canonicalize_filename(const char *filename) {
    char *canon, *start, *p, *q;
    unsigned int i;

    canon = strdup(filename);
    if (!path_is_absolute(canon)) {
        return canon;
    }

    start = (char *) path_skip_root(canon);

    if (start == NULL) {
        /* This shouldn't really happen, as get_current_dir() should
           return an absolute pathname, but bug 573843 shows this is
           not always happening */
        free(canon);
        return build_filename(DIR_SEPARATOR_S, filename, NULL);
    }

    /* POSIX allows double slashes at the start to
     * mean something special (as does windows too).
     * So, "//" != "/", but more than two slashes
     * is treated as "/".
     */
    i = 0;
    for (p = start - 1;
         (p >= canon) &&
         IS_DIR_SEPARATOR(*p);
         p--)
        i++;
    if (i > 2) {
        i -= 1;
        start -= i;
        memmove(start, start + i, strlen(start + i) + 1);
    }

    /* Make sure we're using the canonical dir separator */
    p++;
    while (p < start && IS_DIR_SEPARATOR(*p))
        *p++ = DIR_SEPARATOR;

    p = start;
    while (*p != 0) {
        if (p[0] == '.' && (p[1] == 0 || IS_DIR_SEPARATOR(p[1]))) {
            memmove(p, p + 1, strlen(p + 1) + 1);
        } else if (p[0] == '.' && p[1] == '.' && (p[2] == 0 || IS_DIR_SEPARATOR(p[2]))) {
            q = p + 2;
            /* Skip previous separator */
            p = p - 2;
            if (p < start)
                p = start;
            while (p > start && !IS_DIR_SEPARATOR(*p))
                p--;
            if (IS_DIR_SEPARATOR(*p))
                *p++ = DIR_SEPARATOR;
            memmove(p, q, strlen(q) + 1);
        } else {
            /* Skip until next separator */
            while (*p != 0 && !IS_DIR_SEPARATOR(*p))
                p++;

            if (*p != 0) {
                /* Canonicalize one separator */
                *p++ = DIR_SEPARATOR;
            }
        }

        /* Remove additional separators */
        q = p;
        while (*q && IS_DIR_SEPARATOR(*q))
            q++;

        if (p != q)
            memmove(p, q, strlen(q) + 1);
    }

    /* Remove trailing slashes */
    if (p > start && IS_DIR_SEPARATOR(*(p - 1)))
        *(p - 1) = 0;

    return canon;
}