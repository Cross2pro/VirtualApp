//
// Created by zhangsong on 17-11-24.
//

#include <cassert>
#include "virtualFileSystem.h"
#include "originalInterface.h"
#include "utils/Autolock.h"
#include "utils/mylog.h"

virtualFileDescribeSet g_VFDS;
virtualFileDescribeSet& virtualFileDescribeSet::getVFDSet() {
    return g_VFDS;
}

void virtualFileDescribeSet::reset(int idx) {
    assert(idx >= 0 && idx < 1024);

    items[idx].reset();
}
void virtualFileDescribeSet::set(int idx, virtualFileDescribe *vfd) {
    assert(idx >= 0 && idx < 1024);
    assert(items->get(idx) == 0);

    items[idx].set((uint32_t)vfd);
}

virtualFileDescribe* virtualFileDescribeSet::get(int idx) {
    assert(idx >= 0 && idx < 1024);

    virtualFileDescribe * vfd = (virtualFileDescribe*)items[idx].get();

    return vfd;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
virtualFileManager g_VFM;
/*
void virtualFileManager::forceClean(char * path) {
    virtualFileDescribeSet &vfds = virtualFileDescribeSet::getVFDSet();

    Autolock at_lock(_lock);
    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end())
    {
        virtualFile * vf = iter->second;
        for(int i = 0; i < 1024; i++)
        {
            virtualFileDescribe * vfd = vfds.get(i);
            if(vfd != 0 && vfd->_vf == vf)
            {
                delete vfd;
                vfds.set(i, 0);
            }
        }
        {
            log("judge : force clean [path %s]", vf->getPath());
            delete vf;
            _vfmap.erase(iter);
        }
    }
}
*/
void virtualFileManager::deleted(char *path) {
    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);
    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end())
    {
        virtualFile * vf = iter->second;
        {
            log("judge : [path %s] deleted", vf->getPath());

            int len = strlen(path) + 20;

            char *tmp = new char[len];
            memset(tmp, 0, len);

            snprintf(tmp, len, "%s deleted", vf->getPath());
            vf->setPath(tmp);

            _vfmap.erase(iter);     //删掉原来的节点
            _vfmap.insert(std::pair<std::string, virtualFile *>(std::string(tmp), vf)); //以新文件名从新插入

            delete []tmp;
        }
    }
}

virtualFileManager & virtualFileManager::getVFM() {
    return g_VFM;
}

virtualFile* virtualFileManager::queryVF(char *path) {
    virtualFile *vf = 0;

    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);
    do {
        VFMap::iterator iterator = _vfmap.find(std::string(path));
        if(iterator != _vfmap.end()) {
            vf = iterator->second;
            vf->addRef();

            LOGE("judge : query virtualFile ");

            break;
        }
    }while(false);

    return vf;
}

void virtualFileManager::updateVF(virtualFile &vf) {

    vfileState vfs = VFS_IGNORE;
    do {
        int fd = originalInterface::original_openat(AT_FDCWD, vf.getPath(), O_RDONLY, 0);
        if(fd <= 0)
        {
            slog("judge : updateVF openat [%s] fail", vf.getPath());
            break;
        }

        struct stat sb;
        originalInterface::original_fstat(fd, &sb);

        if (!S_ISREG(sb.st_mode)) {
            //LOGE("judge : S_ISREG return false");
            break;      //不处理
        }

        if (sb.st_size == 0) {
            //设置为 ‘待判断’
            vfs = VFS_TESTING;

            slog("judge : updateVF file size = 0");
        } else if (sb.st_size > 0) {
            //是加密文件 是遏制为 ‘处理’
            //不是加密文件 不管

            if (EncryptFile::isEncryptFile(fd)) {
                vfs = VFS_ENCRYPT;

                slog("judge : updateVF find Encrypt File ");
            } else {
                slog("judge : updateVF not EF ignore");
            }

        }

        virtualFileDescribe vfd(fd);
        vf.setVFS(vfs);
        if(!vf.create(&vfd))
        {
            slog("judge :  **** updateVF  [%s] fail **** ", vf.getPath());
            slog("judge :  **** updateVF  [%s] fail **** ", vf.getPath());
            slog("judge :  **** updateVF  [%s] fail **** ", vf.getPath());

            vf.setVFS(VFS_IGNORE);
        }

    } while (false);
}

virtualFile* virtualFileManager::getVF(virtualFileDescribe* pvfd, char *path, int * pErrno) {

    virtualFile *vf = 0;
    vfileState vfs = VFS_IGNORE;
    *pErrno = 0;                                //默认无错误发生

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);
    do {
        VFMap::iterator iterator = _vfmap.find(std::string(path));
        if(iterator != _vfmap.end()) {
            vf = iterator->second;
            vf->addRef();

            LOGE("judge : found virtualFile [%s]", vf->getPath());

            vfd->_vf = vf;
            vfd->cur_state = vf->getVFS();  //记录最初的状态

            break;
        }

        do {
            /*if (strncmp(path, "/data", 5) != 0
                && strncmp(path, "/sdcard", 7) != 0
                && strncmp(path, "/storage", 8) != 0
                    ) {
                break;
            }*/

            struct stat sb;
            originalInterface::original_fstat(vfd->_fd, &sb);

            if (!S_ISREG(sb.st_mode)) {
                //LOGE("judge : S_ISREG return false");
                break;      //不处理
            }

            if (sb.st_size == 0) {
                //设置为 ‘待判断’
                vfs = VFS_TESTING;

                LOGE("judge : file size = 0");
            } else if (sb.st_size > 0) {
                //是加密文件 是遏制为 ‘处理’
                //不是加密文件 不管

                if (EncryptFile::isEncryptFile(vfd->_fd)) {
                    vfs = VFS_ENCRYPT;

                    LOGE("judge : find Encrypt File ");
                } else {
                    LOGE("judge : not EF ignore");
                }

            }

            if(vfs != VFS_IGNORE)
            {
                {
                    vf = new virtualFile(path);
                    vf->addRef();
                    vf->setVFS(vfs);

                    if(!vf->create(vfd.get()))
                    {
                        delete vf;
                        vf = 0;

                        *pErrno = -1;

                        LOGE("************* virtualFile::create fail ! *************");
                        LOGE("************* virtualFile::create fail ! *************");
                        LOGE("************* virtualFile::create fail ! *************");
                        LOGE("************* virtualFile::create fail ! *************");
                    }
                    else {
                        LOGE("judge : create virtualFile [%s]", vf->getPath());

                        vfd->_vf = vf;
                        vfd->cur_state = vf->getVFS();  //记录最初的状态

                        _vfmap.insert(std::pair<std::string, virtualFile *>(std::string(path), vf));
                    }
                }
            }

        } while (false);

    }while(false);

    return vf;
}

void virtualFileManager::releaseVF(char *path, virtualFileDescribe* pvfd) {
    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end())
    {
        virtualFile * vf = iter->second;
        if(vf->delRef() == 0) {
//            struct stat buf;
//            buf.st_size = 0;
//
//            int fd = originalInterface::original_openat(AT_FDCWD, vf->getPath(), O_RDONLY, 0);
//            if(fd > 0) {
//                originalInterface::original_fstat(fd, &buf);
//                originalInterface::original_close(fd);
//            }
//            log("judge : file [path %s] [size %lld] real closed", vf->getPath(), buf.st_size);
            vf->vclose(vfd.get());
            delete vf;
            _vfmap.erase(iter);
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
void virtualFile::lockWhole() {
    ManualWLock::lock(_rw_lock);
}

void virtualFile::unlockWhole() {
    ManualWLock::unlock(_rw_lock);
}

unsigned int virtualFile::addRef() {
    Autolock at_lock(_ref_lock, (char*)__FUNCTION__, __LINE__);
    ++refrence;

    log("virtualFile::addRef [refrence %u][%s]", refrence, _path);

    return refrence;
}

unsigned int virtualFile::delRef() {
    Autolock at_lock(_ref_lock, (char*)__FUNCTION__, __LINE__);
    if(refrence > 0)
        --refrence;

    log("virtualFile::delRef [refrence %u] [%s]", refrence, _path);

    return refrence;
}

vfileState virtualFile::getVFS() {
    Autolock at_lock(_vfs_lock, (char*)__FUNCTION__, __LINE__);
    return _vfs;
}

void virtualFile::setVFS(vfileState vfs) {
    Autolock at_lock(_vfs_lock, (char*)__FUNCTION__, __LINE__);
    _vfs = vfs;
}

bool virtualFile::create(virtualFileDescribe* pvfd) {
    vfileState vfs = getVFS();

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT) {
        if(ef != NULL) {
            delete ef;
            ef = NULL;
        }
        if(ef == NULL)
        {
            ef = new EncryptFile(_path);
            bool ret = ef->create(vfd->_fd, ENCRYPT_READ);
            if(!ret) {
                delete ef;
                ef = 0;
            }

            return ret;
        }
    } else if(vfs == VFS_TESTING) {
        if(tf != NULL)
        {
            delete tf;
            tf = NULL;
        }
        if(tf == NULL)
        {
            tf = new TemplateFile();
            bool ret = tf->create(_path);
            if(!ret)
            {
                delete tf;
                tf = 0;
            }

            return ret;
        }
    } else if (vfs == VFS_IGNORE) {

    } else {
        slog("virtualFile::create vfs UNKNOW");
        slog("virtualFile::create vfs UNKNOW");
        slog("virtualFile::create vfs UNKNOW");
    }

    return false;
}

int virtualFile::vclose(virtualFileDescribe* pvfd) {
    /*
     * 如果VFS == VFS_TESTING
     * 那么这里需要做最后一次检查
     */
    vfileState vfs = getVFS();

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT) {
    } else if(vfs == VFS_TESTING) {
        if(tf != NULL)
        {
            tf->close(true, vfd->_fd);
        }
    } else {
    }

    return 0;
}

void virtualFile::forceTranslate() {
    vfileState vfs = getVFS();
    if(vfs == VFS_TESTING) {
        if(tf != NULL)
        {
            tf->forceTranslate();
        }
    }
}

int virtualFile::vpread64(virtualFileDescribe* pvfd, char * buf, size_t len, off64_t from) {
    vfileState vfs = getVFS();

    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->pread64(vfd->_fd, buf, len, from);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::pread64(vfd->_fd, buf, len, from);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::pread64(vfd->_fd, buf, len, from);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->pread64(vfd->_fd, buf, len, from);
            }
            case VFS_TESTING:
                return tf->pread64(vfd->_fd, buf, len, from);
        }
    }

    return 0;
}

int virtualFile::vpwrite64(virtualFileDescribe* pvfd, char * buf, size_t len, off64_t offset) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->pwrite64(vfd->_fd, buf, len, offset);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::pwrite64(vfd->_fd, buf, len, offset);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::pwrite64(vfd->_fd, buf, len, offset);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->pwrite64(vfd->_fd, buf, len, offset);
            }
            case VFS_TESTING: {
                int ret =  tf->pwrite64(vfd->_fd, buf, len, offset);

                if(tf->canCheck())
                {
                    if(tf->doControl())
                    {
                        tf->translate(vfd->_fd);
                        setVFS(VFS_ENCRYPT);

                        if(ef == NULL) {
                            ef = new EncryptFile(*tf->getBK());
                        }
                    }
                    else {
                        setVFS(VFS_IGNORE);
                    }

                    tf->close(false, vfd->_fd);
                    delete tf;
                    tf = 0;
                }

                return ret;
            }
        }
    }

    return 0;
}

int virtualFile::vread(virtualFileDescribe* pvfd, char * buf, size_t len) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->read(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::read(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::read(vfd->_fd, buf, len);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->read(vfd->_fd, buf, len);
            }
            case VFS_TESTING:
                return tf->read(vfd->_fd, buf, len);
        }
    }

    return 0;
}

int virtualFile::vwrite(virtualFileDescribe* pvfd, char * buf, size_t len) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->write(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::write(vfd->_fd, buf, len);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::write(vfd->_fd, buf, len);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->write(vfd->_fd, buf, len);
            }
            case VFS_TESTING: {
                int ret =  tf->write(vfd->_fd, buf, len);

                if(tf->canCheck())
                {
                    if(tf->doControl())
                    {
                        tf->translate(vfd->_fd);
                        setVFS(VFS_ENCRYPT);

                        if(ef == NULL) {
                            ef = new EncryptFile(*tf->getBK());
                        }
                    }
                    else {
                        setVFS(VFS_IGNORE);
                    }

                    tf->close(false, vfd->_fd);
                    delete tf;
                    tf = 0;
                }

                return ret;
            }
        }
    }

    return 0;
}

int virtualFile::vfstat(virtualFileDescribe* pvfd, struct stat *buf) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->fstat(vfd->_fd, buf);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::fstat(vfd->_fd, buf);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::fstat(vfd->_fd, buf);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->fstat(vfd->_fd, buf);
            }
            case VFS_TESTING:
                return tf->fstat(vfd->_fd, buf);
        }
    }

    return 0;
}

off_t virtualFile::vlseek(virtualFileDescribe* pvfd, off_t offset, int whence){
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->lseek(vfd->_fd, offset, whence);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::lseek(vfd->_fd, offset, whence);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::lseek(vfd->_fd, offset, whence);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->lseek(vfd->_fd, offset, whence);
            }
            case VFS_TESTING:
                return tf->lseek(vfd->_fd, offset, whence);
        }
    }

    return 0;
}

int virtualFile::vllseek(virtualFileDescribe* pvfd, unsigned long offset_high, unsigned long offset_low, loff_t *result,
                         unsigned int whence) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->llseek(vfd->_fd, offset_high, offset_low, result, whence);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::llseek(vfd->_fd, offset_high, offset_low, result, whence);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::llseek(vfd->_fd, offset_high, offset_low, result, whence);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->llseek(vfd->_fd, offset_high, offset_low, result, whence);
            }
            case VFS_TESTING:
                return tf->llseek(vfd->_fd, offset_high, offset_low, result, whence);
        }
    }

    return 0;
}

int virtualFile::vftruncate(virtualFileDescribe* pvfd, off_t length) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->ftruncate(vfd->_fd, length);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::ftruncate(vfd->_fd, length);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::ftruncate(vfd->_fd, length);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->ftruncate(vfd->_fd, length);
            }
            case VFS_TESTING:
                return tf->ftruncate(vfd->_fd, length);
        }
    }

    return 0;
}

int virtualFile::vftruncate64(virtualFileDescribe* pvfd, off64_t length) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd(pvfd);

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->ftruncate64(vfd->_fd, length);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::ftruncate64(vfd->_fd, length);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::ftruncate64(vfd->_fd, length);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(vfd->_fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->ftruncate64(vfd->_fd, length);
            }
            case VFS_TESTING:
                return tf->ftruncate64(vfd->_fd, length);
        }
    }

    return 0;
}
