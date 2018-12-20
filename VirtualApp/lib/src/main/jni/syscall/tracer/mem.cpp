#include <cerrno>
#include <climits>
#include <syscall/arch.h>
#include <sys/uio.h>
#include <Foundation/Log.h>
#include <assert.h>
#include <string.h>
#include <bits/sysconf.h>
#include "mem.h"
#include "tracer.h"
#include "reg.h"
#include "event.h"

/**
 * Load the word at the given @address, potentially *not* aligned.
 */
static inline word_t load_word(const void *address) {
#ifdef NO_MISALIGNED_ACCESS
    if (((word_t)address) % sizeof(word_t) == 0)
        return *(word_t *)address;
    else {
        word_t value;
        memcpy(&value, address, sizeof(word_t));
        return value;
    }
#else
    return *(word_t *) address;
#endif
}

/**
 * Store the word with the given @value to the given @address,
 * potentially *not* aligned.
 */
static inline void store_word(void *address, word_t value) {
    *((word_t *) address) = value;
}

static int ptrace_pokedata_or_via_stub(Tracer *tracee, word_t addr, word_t word) {
    int status;
    status = ptrace(PTRACE_POKEDATA, tracee->pid, addr, word);
    return status;
}

/**
 * Copy @size bytes from the buffer @src_tracer to the address
 * @dest_tracee within the memory space of the @tracee process. It
 * returns -errno if an error occured, otherwise 0.
 */
int write_data(Tracer *tracee, word_t dest_tracee, const void *src_tracer, word_t size) {
    word_t *src = (word_t *) src_tracer;
    word_t *dest = (word_t *) dest_tracee;

    long status;
    word_t word, i, j;
    word_t nb_trailing_bytes;
    word_t nb_full_words;

    uint8_t *last_dest_word;
    uint8_t *last_src_word;

    if (use_process_vm_api()) {
        struct iovec local;
        struct iovec remote;

        local.iov_base = src;
        local.iov_len = size;

        remote.iov_base = dest;
        remote.iov_len = size;

        status = process_vm_writev(tracee->pid, &local, 1, &remote, 1, 0);
        if ((size_t) status == size)
            return 0;
    }
    /* Fallback to ptrace */
    nb_trailing_bytes = size % sizeof(word_t);
    nb_full_words = (size - nb_trailing_bytes) / sizeof(word_t);

    /* Clear errno so we won't detect previous syscall failure as ptrace one */
    errno = 0;

    /* Copy one word by one word, except for the last one. */
    for (i = 0; i < nb_full_words; i++) {
        status = ptrace_pokedata_or_via_stub(tracee, (word_t) (dest + i), load_word(&src[i]));
        if (status < 0) {
            ALOGE("error: ptrace(POKEDATA)");
            return -EFAULT;
        }
    }

    if (nb_trailing_bytes == 0)
        return 0;

    /* Copy the bytes in the last word carefully since we have to
     * overwrite only the relevant ones. */

    /* Clear errno so we won't detect previous syscall failure as ptrace one */
    errno = 0;

    word = (word_t) ptrace(PTRACE_PEEKDATA, tracee->pid, dest + i, NULL);
    if (errno != 0) {
        ALOGE("error: ptrace(PEEKDATA)");
        return -EFAULT;
    }

    last_dest_word = (uint8_t *) &word;
    last_src_word = (uint8_t *) &src[i];

    for (j = 0; j < nb_trailing_bytes; j++)
        last_dest_word[j] = last_src_word[j];

    status = ptrace_pokedata_or_via_stub(tracee, (word_t) (dest + i), word);
    if (status < 0) {
        ALOGE("error: ptrace(POKEDATA)");
        return -EFAULT;
    }

    return 0;
}


/**
 * Gather the @src_tracer_count buffers pointed to by @src_tracer to
 * the address @dest_tracee within the memory space of the @tracee
 * process.  This function returns -errno if an error occured,
 * otherwise 0.
 */
int writev_data(Tracer *tracee, word_t dest_tracee, const struct iovec *src_tracer,
                int src_tracer_count) {
    size_t size;
    int status;
    int i;

    if (use_process_vm_api()) {
        struct iovec remote;

        for (i = 0, size = 0; i < src_tracer_count; i++)
            size += src_tracer[i].iov_len;

        remote.iov_base = (word_t *) dest_tracee;
        remote.iov_len = size;

        status = process_vm_writev(tracee->pid, src_tracer, (unsigned long) src_tracer_count,
                                   &remote, 1, 0);
        if ((size_t) status == size)
            return 0;
    }
    /* Fallback to iterative-write if something went wrong.  */
    for (i = 0, size = 0; i < src_tracer_count; i++) {
        status = write_data(tracee, dest_tracee + size,
                            src_tracer[i].iov_base, src_tracer[i].iov_len);
        if (status < 0)
            return status;

        size += src_tracer[i].iov_len;
    }

    return 0;
}

/**
 * Copy @size bytes to the buffer @dest_tracer from the address
 * @src_tracee within the memory space of the @tracee process. It
 * returns -errno if an error occured, otherwise 0.
 */
int read_data(const Tracer *tracee, void *dest_tracer, word_t src_tracee, word_t size) {
    word_t *src = (word_t *) src_tracee;
    word_t *dest = (word_t *) dest_tracer;

    word_t nb_trailing_bytes;
    word_t nb_full_words;
    word_t word, i, j;

    uint8_t *last_src_word;
    uint8_t *last_dest_word;

    if (use_process_vm_api()) {
        long status;
        struct iovec local;
        struct iovec remote;

        local.iov_base = dest;
        local.iov_len = size;

        remote.iov_base = src;
        remote.iov_len = size;

        status = process_vm_readv(tracee->pid, &local, 1, &remote, 1, 0);
        if ((size_t) status == size)
            return 0;
        /* Fallback to ptrace if something went wrong.  */

    }

    nb_trailing_bytes = size % sizeof(word_t);
    nb_full_words = (size - nb_trailing_bytes) / sizeof(word_t);

    /* Clear errno so we won't detect previous syscall failure as ptrace one */
    errno = 0;

    /* Copy one word by one word, except for the last one. */
    for (i = 0; i < nb_full_words; i++) {
        word = (word_t) ptrace(PTRACE_PEEKDATA, tracee->pid, src + i, NULL);
        if (errno != 0) {
            ALOGE("error: ptrace(PEEKDATA)");
            return -EFAULT;
        }
        store_word(&dest[i], word);
    }

    if (nb_trailing_bytes == 0)
        return 0;

    /* Copy the bytes from the last word carefully since we have
     * to not overwrite the bytes lying beyond @dest_tracer. */

    word = (word_t) ptrace(PTRACE_PEEKDATA, tracee->pid, src + i, NULL);
    if (errno != 0) {
        ALOGE("error: ptrace(PEEKDATA)");
        return -EFAULT;
    }

    last_dest_word = (uint8_t *) &dest[i];
    last_src_word = (uint8_t *) &word;

    for (j = 0; j < nb_trailing_bytes; j++)
        last_dest_word[j] = last_src_word[j];

    return 0;
}

/**
 * Copy to @dest_tracer at most @max_size bytes from the string
 * pointed to by @src_tracee within the memory space of the @tracee
 * process. This function returns -errno on error, otherwise
 * it returns the number in bytes of the string, including the
 * end-of-string terminator.
 */
int read_string(const Tracer *tracee, char *dest_tracer, word_t src_tracee, word_t max_size) {
    word_t *src = (word_t *) src_tracee;
    word_t *dest = (word_t *) dest_tracer;

    word_t nb_trailing_bytes;
    word_t nb_full_words;
    word_t word, i, j;

    uint8_t *src_word;
    uint8_t *dest_word;

    if (use_process_vm_api()) {
        long status;
        size_t size;
        size_t offset;
        struct iovec local;
        struct iovec remote;

        static size_t chunk_size = 0;
        static uintptr_t chunk_mask;

        /* A chunk shall not cross a page boundary.  */
        if (chunk_size == 0) {
            chunk_size = (size_t) sysconf(_SC_PAGE_SIZE);
            chunk_size = (chunk_size > 0 && chunk_size < 1024 ? chunk_size : 1024);
            chunk_mask = ~(chunk_size - 1);
        }

        /* Read the string by chunk.  */
        offset = 0;
        do {
            uintptr_t current_chunk = (src_tracee + offset) & chunk_mask;
            uintptr_t next_chunk = current_chunk + chunk_size;

            /* Compute the number of bytes available up to the
             * next chunk or up to max_size.  */
            size = next_chunk - (src_tracee + offset);
            size = (size < max_size - offset ? size : max_size - offset);

            local.iov_base = (uint8_t *) dest + offset;
            local.iov_len = size;

            remote.iov_base = (uint8_t *) src + offset;
            remote.iov_len = size;

            status = process_vm_readv(tracee->pid, &local, 1, &remote, 1, 0);
            if ((size_t) status != size)
                goto fallback;

            status = (long) strnlen((const char *) local.iov_base, size);
            if ((size_t) status < size) {
                size = offset + status + 1;
                assert(size <= max_size);
                return size;
            }

            offset += size;
        } while (offset < max_size);
    }
    /* Fallback to ptrace if something went wrong.  */
    fallback:

    nb_trailing_bytes = max_size % sizeof(word_t);
    nb_full_words = (max_size - nb_trailing_bytes) / sizeof(word_t);

    /* Clear errno so we won't detect previous syscall failure as ptrace one */
    errno = 0;

    /* Copy one word by one word, except for the last one. */
    for (i = 0; i < nb_full_words; i++) {
        word = (word_t) ptrace(PTRACE_PEEKDATA, tracee->pid, src + i, NULL);
        if (errno != 0)
            return -EFAULT;

        store_word(&dest[i], word);

        /* Stop once an end-of-string is detected. */
        src_word = (uint8_t *) &word;
        for (j = 0; j < sizeof(word_t); j++)
            if (src_word[j] == '\0')
                return (int) i * sizeof(word_t) + j + 1;
    }

    /* Copy the bytes from the last word carefully since we have
     * to not overwrite the bytes lying beyond @dest_tracer. */

    word = (word_t) ptrace(PTRACE_PEEKDATA, tracee->pid, src + i, NULL);
    if (errno != 0)
        return -EFAULT;

    dest_word = (uint8_t *) &dest[i];
    src_word = (uint8_t *) &word;

    for (j = 0; j < nb_trailing_bytes; j++) {
        dest_word[j] = src_word[j];
        if (src_word[j] == '\0')
            break;
    }
    return (int) i * sizeof(word_t) + j + 1;
}

/**
 * Allocate @size bytes in the @tracee's memory space.  This function
 * returns the address of the allocated memory in the @tracee's memory
 * space, otherwise 0 if an error occured.
 */
word_t alloc_mem(Tracer *tracee, ssize_t size) {
    word_t stack_pointer;

    /* This function should be called in sysenter only since the
     * stack pointer is systematically restored at the end of
     * sysexit (except for execve, but in this case the stack
     * pointer should be handled with care since it is used by the
     * process to retrieve argc, argv, envp, and auxv).  */
    assert(IS_IN_SYSENTER(tracee));

    /* Get the current value of the stack pointer from the tracee's
     * USER area. */
    stack_pointer = peek_reg(tracee, CURRENT, STACK_POINTER);

    /* Some ABIs specify an amount of bytes after the stack
     * pointer that shall not be used by anything but the compiler
     * (for optimization purpose).  */
    if (stack_pointer == peek_reg(tracee, ORIGINAL, STACK_POINTER))
        size += RED_ZONE_SIZE;

    /* Sanity check. */
    if ((size > 0 && stack_pointer <= (word_t) size)
        || (size < 0 && stack_pointer >= ULONG_MAX + size)) {
        ALOGE("integer under/overflow detected in %s", __FUNCTION__);
        return 0;
    }

    /* Remember the stack grows downward. */
    stack_pointer -= size;

    /* Set the new value of the stack pointer in the tracee's USER
     * area. */
    poke_reg(tracee, STACK_POINTER, stack_pointer);
    return stack_pointer;
}