#include "tracer.h"

#ifndef ENVENT_H
#define ENVENT_H

#endif //ENVENT_H

bool use_process_vm_api();

int trace_current_process(int sdkVersion);

bool restart_tracee(Tracer *tracee, int signal);