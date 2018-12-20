#ifndef Tracer_REG_H
#define Tracer_REG_H

#include "arch.h"
#include "tracer.h"

typedef enum {
	SYSARG_NUM = 0,
	SYSARG_1,
	SYSARG_2,
	SYSARG_3,
	SYSARG_4,
	SYSARG_5,
	SYSARG_6,
	SYSARG_RESULT,
	STACK_POINTER,
	INSTR_POINTER,
	RTLD_FINI,
	STATE_FLAGS,
	USERARG_1,
} Reg;

int fetch_regs(Tracer *tracer);
int push_specific_regs(Tracer *Tracer, bool including_sysnum);
int push_regs(Tracer *Tracer);

word_t peek_reg(const Tracer *Tracer, RegVersion version, Reg reg);
void poke_reg(Tracer *Tracer, Reg reg, word_t value);

void print_current_regs(Tracer *Tracer, int verbose_level, const char *message);
void save_current_regs(Tracer *Tracer, RegVersion version);

#endif /* Tracer_REG_H */
