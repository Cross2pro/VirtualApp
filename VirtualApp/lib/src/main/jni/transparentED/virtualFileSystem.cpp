//
// Created by zhangsong on 17-11-24.
//

#include "virtualFileSystem.h"
#include "originalInterface.h"
#include "utils/Autolock.h"
#include "utils/mylog.h"

virtualFileDescribeSet g_VFDS;
virtualFileDescribeSet& virtualFileDescribeSet::getVFDSet() {
    return g_VFDS;
}

void virtualFileDescribeSet::reset(int idx) {
    if(idx < 0 || idx > 1023)
    {
        return ;
    }

    items[idx].reset();
}
void virtualFileDescribeSet::set(int idx, virtualFileDescribe *vfd) {
    if(idx < 0 || idx > 1023)
    {
        return;
    }

    items[idx].set((uint32_t)vfd);
}

virtualFileDescribe* virtualFileDescribeSet::get(int idx) {
    if(idx < 0 || idx > 1023)
    {
        return 0;
    }

    return (virtualFileDescribe*)items[idx].get();
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
            char tmp[MAX_PATH + 20] = {0};
            snprintf(tmp, MAX_PATH + 20, "%s deleted", vf->getPath());
            vf->setPath(tmp);

            _vfmap.erase(iter);     //删掉原来的节点
            _vfmap.insert(std::pair<std::string, virtualFile *>(std::string(tmp), vf)); //以新文件名从新插入
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

        vf.setVFS(vfs);
        if(!vf.create(fd))
        {
            slog("judge :  **** updateVF  [%s] fail **** ", vf.getPath());
            slog("judge :  **** updateVF  [%s] fail **** ", vf.getPath());
            slog("judge :  **** updateVF  [%s] fail **** ", vf.getPath());

            vf.setVFS(VFS_IGNORE);
        }

    } while (false);
}

virtualFile* virtualFileManager::getVF(int fd, char *path, int * pErrno) {

    virtualFile *vf = 0;
    vfileState vfs = VFS_IGNORE;
    *pErrno = 0;                                //默认无错误发生

    /*
     * 首先获取vfd，获取不到一定是发生异常，返回错误
     */
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr)
    {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        *pErrno = -1;
        return 0;
    }

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
            if (strncmp(path, "/data", 5) != 0
                && strncmp(path, "/sdcard", 7) != 0
                && strncmp(path, "/storage", 8) != 0
                    ) {
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

                LOGE("judge : file size = 0");
            } else if (sb.st_size > 0) {
                //是加密文件 是遏制为 ‘处理’
                //不是加密文件 不管

                if (EncryptFile::isEncryptFile(fd)) {
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

                    if(!vf->create(fd))
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

void virtualFileManager::releaseVF(char *path) {
    Autolock at_lock(_lock, (char*)__FUNCTION__, __LINE__);

    VFMap::iterator iter = _vfmap.find(std::string(path));
    if(iter != _vfmap.end())
    {
        virtualFile * vf = iter->second;
        if(vf->delRef() == 0) {
            struct stat buf;
            buf.st_size = 0;

            int fd = originalInterface::original_openat(AT_FDCWD, vf->getPath(), O_RDONLY, 0);
            if(fd > 0) {
                originalInterface::original_fstat(fd, &buf);
                originalInterface::original_close(fd);
            }
            log("judge : file [path %s] [size %lld] real closed", vf->getPath(), buf.st_size);
            vf->vclose();
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

bool virtualFile::create(int fd) {
    vfileState vfs = getVFS();

    if(vfs == VFS_ENCRYPT) {
        if(ef != NULL) {
            delete ef;
            ef = NULL;
        }
        if(ef == NULL)
        {
            ef = new EncryptFile(_path);
            bool ret = ef->create(fd, ENCRYPT_READ);
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

int virtualFile::vclose() {
    /*
     * 如果VFS == VFS_TESTING
     * 那么这里需要做最后一次检查
     */
    vfileState vfs = getVFS();

    if(vfs == VFS_ENCRYPT) {
    } else if(vfs == VFS_TESTING) {
        if(tf != NULL)
        {
            tf->close();
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

int virtualFile::vpread64(int fd, char * buf, size_t len, off64_t from) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->pread64(fd, buf, len, from);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::pread64(fd, buf, len, from);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::pread64(fd, buf, len, from);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->pread64(fd, buf, len, from);
            }
            case VFS_TESTING:
                return tf->pread64(fd, buf, len, from);
        }
    }

    return 0;
}

int virtualFile::vpwrite64(int fd, char * buf, size_t len, off64_t offset) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->pwrite64(fd, buf, len, offset);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::pwrite64(fd, buf, len, offset);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::pwrite64(fd, buf, len, offset);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->pwrite64(fd, buf, len, offset);
            }
            case VFS_TESTING: {
                int ret =  tf->pwrite64(fd, buf, len, offset);

                if(tf->canCheck())
                {
                    if(tf->doControl())
                    {
                        tf->translate(fd);
                        setVFS(VFS_ENCRYPT);

                        if(ef == NULL) {
                            ef = new EncryptFile(*tf->getBK());
                        }
                    }
                    else {
                        setVFS(VFS_IGNORE);
                    }

                    tf->close(false);
                    delete tf;
                    tf = 0;
                }

                return ret;
            }
        }
    }

    return 0;
}

int virtualFile::vread(int fd, char * buf, size_t len) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->read(fd, buf, len);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::read(fd, buf, len);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::read(fd, buf, len);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->read(fd, buf, len);
            }
            case VFS_TESTING:
                return tf->read(fd, buf, len);
        }
    }

    return 0;
}

int virtualFile::vwrite(int fd, char * buf, size_t len) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->write(fd, buf, len);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::write(fd, buf, len);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoWLock awl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::write(fd, buf, len);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->write(fd, buf, len);
            }
            case VFS_TESTING: {
                int ret =  tf->write(fd, buf, len);

                if(tf->canCheck())
                {
                    if(tf->doControl())
                    {
                        tf->translate(fd);
                        setVFS(VFS_ENCRYPT);

                        if(ef == NULL) {
                            ef = new EncryptFile(*tf->getBK());
                        }
                    }
                    else {
                        setVFS(VFS_IGNORE);
                    }

                    tf->close(false);
                    delete tf;
                    tf = 0;
                }

                return ret;
            }
        }
    }

    return 0;
}

int virtualFile::vfstat(int fd, struct stat *buf) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->fstat(fd, buf);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::fstat(fd, buf);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::fstat(fd, buf);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->fstat(fd, buf);
            }
            case VFS_TESTING:
                return tf->fstat(fd, buf);
        }
    }

    return 0;
}

off_t virtualFile::vlseek(int fd, off_t offset, int whence){
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }
        return ef->lseek(fd, offset, whence);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::lseek(fd, offset, whence);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::lseek(fd, offset, whence);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->lseek(fd, offset, whence);
            }
            case VFS_TESTING:
                return tf->lseek(fd, offset, whence);
        }
    }

    return 0;
}

int virtualFile::vllseek(int fd, unsigned long offset_high, unsigned long offset_low, loff_t *result,
                         unsigned int whence) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->llseek(fd, offset_high, offset_low, result, whence);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::llseek(fd, offset_high, offset_low, result, whence);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::llseek(fd, offset_high, offset_low, result, whence);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->llseek(fd, offset_high, offset_low, result, whence);
            }
            case VFS_TESTING:
                return tf->llseek(fd, offset_high, offset_low, result, whence);
        }
    }

    return 0;
}

int virtualFile::vftruncate(int fd, off_t length) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->ftruncate(fd, length);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::ftruncate(fd, length);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::ftruncate(fd, length);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->ftruncate(fd, length);
            }
            case VFS_TESTING:
                return tf->ftruncate(fd, length);
        }
    }

    return 0;
}

int virtualFile::vftruncate64(int fd, off64_t length) {
    vfileState vfs = getVFS();
    xdja::zs::sp<virtualFileDescribe> vfd = virtualFileDescribeSet::getVFDSet().get(fd);
    if(vfd.get() == nullptr) {
        slog("!!! get vfd fail in %s:%d !!!", __FILE__, __LINE__);
        return 0;
    }

    if(vfs == VFS_ENCRYPT)
    {
        if(vfd->cur_state != vfs)
        {
            ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
            vfd->cur_state = vfs;
        }

        return ef->ftruncate64(fd, length);
    }
    else if(vfs == VFS_IGNORE)
    {
        return ignoreFile::ftruncate64(fd, length);
    }
    else if(vfs == VFS_TESTING)
    {
        AutoRLock arl(_rw_lock, (char*)__FUNCTION__, __LINE__);
        vfileState subvfs = getVFS();
        switch (subvfs)
        {
            case VFS_IGNORE:
                return ignoreFile::ftruncate64(fd, length);
            case VFS_ENCRYPT: {
                if(vfd->cur_state != subvfs)
                {
                    ef->lseek(fd, ef->getHeadOffset(), SEEK_CUR);
                    vfd->cur_state = subvfs;
                }
                return ef->ftruncate64(fd, length);
            }
            case VFS_TESTING:
                return tf->ftruncate64(fd, length);
        }
    }

    return 0;
}
