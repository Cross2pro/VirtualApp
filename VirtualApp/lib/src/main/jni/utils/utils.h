//
// Created by zhangsong on 18-1-22.
//

#ifndef VIRTUALAPP_UTILS_H
#define VIRTUALAPP_UTILS_H

#include "zString.h"

bool getPathFromFd(int fd, zString & path);
bool closeAllSockets();

#endif //VIRTUALAPP_UTILS_H
