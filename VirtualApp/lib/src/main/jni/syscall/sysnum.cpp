#include <assert.h>
#include <syscall/tracer/reg.h>
#include "sysnum.h"
#include "abi.h"
#include "arch.h"

#include SYSNUMS_HEADER1

#ifdef SYSNUMS_HEADER2
#include SYSNUMS_HEADER2
#endif

#ifdef SYSNUMS_HEADER3
#include SYSNUMS_HEADER3
#endif

typedef struct {
    const Sysnum *table;
    word_t offset;
    word_t length;
} Sysnums;

/**
 * Update @sysnums' fields with the sysnum table for the given @abi.
 */
static void get_sysnums(Abi abi, Sysnums *sysnums) {
    switch (abi) {
        case ABI_DEFAULT:
            sysnums->table = SYSNUMS_ABI1;
            sysnums->length = sizeof(SYSNUMS_ABI1) / sizeof(Sysnum);
            sysnums->offset = 0;
            return;
#ifdef SYSNUMS_ABI2
        case ABI_2:
        sysnums->table  = SYSNUMS_ABI2;
        sysnums->length = sizeof(SYSNUMS_ABI2) / sizeof(Sysnum);
        sysnums->offset = 0;
        return;
#endif
#ifdef SYSNUMS_ABI3
        case ABI_3:
        sysnums->table  = SYSNUMS_ABI3;
        sysnums->length = sizeof(SYSNUMS_ABI3) / sizeof(Sysnum);
        sysnums->offset = 0x40000000; /* x32 */
        return;
#endif
        default:
            assert(0);
    }
}

/**
 * Return the neutral value of @sysnum from the given @abi.
 */
static Sysnum translate_sysnum(Abi abi, word_t sysnum) {
    Sysnums sysnums;
    word_t index;

    get_sysnums(abi, &sysnums);

    /* Sanity checks.  */
    if (sysnum < sysnums.offset)
        return SC_void;

    index = sysnum - sysnums.offset;

    /* Sanity checks.  */
    if (index > sysnums.length)
        return SC_void;

    return sysnums.table[index];
}


/**
 * Return the neutral value of the @tracee's current syscall number.
 */
Sysnum get_sysnum(const Tracer *tracee, RegVersion version)
{
    return translate_sysnum(get_abi(tracee), peek_reg(tracee, version, SYSARG_NUM));
}

/**
 * Return the architecture value of @sysnum for the given @abi.
 */
word_t detranslate_sysnum(Abi abi, Sysnum sysnum)
{
    Sysnums sysnums;
    size_t i;

    /* Very special case.  */
    if (sysnum == SC_void)
        return SYSCALL_AVOIDER;

    get_sysnums(abi, &sysnums);

    for (i = 0; i < sysnums.length; i++) {
        if (sysnums.table[i] != sysnum)
            continue;

        return i + sysnums.offset;
    }

    return SYSCALL_AVOIDER;
}

/**
 * Overwrite the @tracee's current syscall number with @sysnum.  Note:
 * this neutral value is automatically converted into the architecture
 * value.
 */
void set_sysnum(Tracer *tracee, Sysnum sysnum)
{
    poke_reg(tracee, SYSARG_NUM, detranslate_sysnum(get_abi(tracee), sysnum));
}

/**
 * Return the human readable name of @sysnum.
 */
const char *stringify_sysnum(Sysnum sysnum) {
#define SYSNUM(item) [ SC_ ## item ] = #item,
    static const char *names[] = {
#include "sysnums.list"
    };
#undef SYSNUM

    if (sysnum == 0)
        return "void";

    if (sysnum >= SC_NB_SYSNUM)
        return "";

    return names[sysnum];
}