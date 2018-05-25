//
// Created by zhangsong on 17-12-15.
//

#include "encryptInfoMgr.h"
#include "fileCoder1.h"

EncryptInfo * getEI(int version, EncryptInfo * ei)
{

    if(version == 0x01)
    {
        EncryptInfo_v1 * ei_v1 = 0;
        if(ei) {
            ei_v1 = new EncryptInfo_v1(*(EncryptInfo_v1 *)ei);
        } else {
            ei_v1 = new EncryptInfo_v1();
        }

        return ei_v1;
    }

    return 0;
}

fileCoder * getFC(int version, EncryptInfo * ei)
{
    if(version == 0x01)
    {
        if(ei == 0)
            return 0;

        EncryptInfo_v1 * ei_v1 = (EncryptInfo_v1*)ei;
        fc1 * fc = new fc1;
        char * key = ei_v1->getKey();
        fc->setKey(key);

        return fc;
    }

    return 0;
}