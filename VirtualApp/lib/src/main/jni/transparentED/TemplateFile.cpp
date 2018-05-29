//
// Created by zhangsong on 17-11-30.
//

#include "TemplateFile.h"
#include "originalInterface.h"
#include "ff_Recognizer.h"

#include <sys/sendfile.h>
#include <errno.h>

#include "utils/mylog.h"

TemplateFile::TemplateFile() {
    _ef_bk = 0;
    _ef_fd = 0;

    memset(flag_for_check, false, sizeof(flag_for_check));
    memset(buf_for_check, 0, sizeof(buf_for_check));
    memset(_path, 0, 260);
}

TemplateFile::~TemplateFile() {

    if(_ef_fd) {
        originalInterface::original_close(_ef_fd);
        //删除真实文件
        originalInterface::original_unlinkat(AT_FDCWD, _ef_bk->getPath(), 0);
        _ef_fd = 0;
    }

    if(_ef_bk)
    {
        delete _ef_bk;
        _ef_bk = 0;
    }
}

bool TemplateFile::canCheck() {
    bool ret = true;

    for(int i = 0; i < CHECK_BUF_SIZE; i++)
    {
        if(flag_for_check[i] == false)
        {
            ret = false;
            break;
        }
    }

    LOGE("TemplateFile::canCheck [%s] return %s ", _path, ret ? "TRUE" : "FALSE");

    return ret;
}

bool TemplateFile::doControl(int len) {
    ff_Recognizer ffr = ff_Recognizer::getFFR();

    bool isHit = ffr.hit(ffr.getFormat(buf_for_check, len));

    return isHit;
}

int TemplateFile::createTempFile(char *path, zString & tpath) {
    const char *dirs[] = {
            "/data/data/io.virtualapp/virtual/data",
            "/data/data/com.xdja.safetybox/virtual/data"
    };

    char *name = strrchr(path, '/');
    if (name == nullptr) {
        log("judge : TemplateFile::createTempFile get name fail!");
        return -1;
    }
    name += 1;

    int i, fd = -1;
    int num = sizeof(dirs) / sizeof(dirs[0]);
    for (i = 0; i < num; i++)
    {
        char newpath[260] = {0};
        sprintf(newpath, "%s/%s.xt", dirs[i], name);
        tpath.format("%s", newpath);

        log("judge : TemplateFile::createTempFile newpaht = %s", newpath);

        fd = originalInterface::original_openat(AT_FDCWD, newpath, O_RDWR|O_CREAT, S_IRWXU);

        if(fd > 0)
            break;
    }

    if(i == num)
    {
        log("judge : TemplateFile::createTempFile fail !!");
        fd = -1;
    }

    return fd;
}

bool TemplateFile::create(const char *path) {

    zString tpath;
    _ef_fd = createTempFile((char *)path, tpath);

    if(_ef_fd <= 0) {
        log("judge : openat fail , reason %s\n", strerror(errno));
        return false;
    }
    strcpy(_path, path);
    _ef_bk = new EncryptFile(tpath.toString());
    if(!_ef_bk->create(_ef_fd, ENCRYPT_WRITE))
    {
        log("judge : _ef_bk->create fail\n");

        delete _ef_bk;
        originalInterface::original_close(_ef_fd);
        //删除文件
        originalInterface::original_unlinkat(AT_FDCWD, _ef_bk->getPath(), 0);

        _ef_bk = 0;
        _ef_fd = 0;

        return false;
    }

    return true;
}

int TemplateFile::fstat(int fd, struct stat *buf) {
    //log("TemplateFile::fstat [fd %d]", fd);

    return originalInterface::original_fstat(fd, buf);
}

ssize_t TemplateFile::read(int fd, char *buf, size_t len) {
    //log("TemplateFile::read [fd %d] [len %zu]", fd, len);

    return originalInterface::original_read(fd, buf, len);
}

ssize_t TemplateFile::pread64(int fd, void *buf, size_t len, off64_t offset) {
    //log("TemplateFile::pread64 [fd %d] [len %zu] [offset %lld]", fd, len, offset);

    return originalInterface::original_pread64(fd, buf, len, offset);
}

off_t TemplateFile::lseek(int fd, off_t pos, int whence) {
    //log("TemplateFile::lseek [fd %d] [offset %d] [whence %d]", fd, pos, whence);

    _ef_bk->lseek(_ef_fd, pos, whence);

    return originalInterface::original_lseek(fd, pos, whence);
}

int TemplateFile::llseek(int fd, unsigned long offset_high, unsigned long offset_low,
                         loff_t *result, unsigned int whence) {
    off64_t rel_offset = 0; rel_offset |= offset_high; rel_offset <<= 32; rel_offset |= offset_low;
    //log("TemplateFile::llseek [fd %d] [offset %lld] [whence %d]", fd, rel_offset, whence);

    _ef_bk->llseek(_ef_fd, offset_high, offset_low, result, whence);

    return originalInterface::original_llseek(fd, offset_high, offset_low, result, whence);
}

ssize_t TemplateFile::pwrite64(int fd, void *buf, size_t len, off64_t offset) {
    //log("TemplateFile::pwrite64 [fd %d] [len %zu] [offset %lld]", fd, len, offset);

    _ef_bk->pwrite64(_ef_fd, buf, len, offset);

    if(offset < CHECK_BUF_SIZE)
    {
        off64_t end = offset + len;
        size_t rlen = my_min(end, CHECK_BUF_SIZE) - offset;

        for(int i = offset; i < offset + rlen; i++)
        {
            flag_for_check[i] = true;
            buf_for_check[i] = ((char *)buf)[i - offset];
        }
    }

    return originalInterface::original_pwrite64(fd, buf, len, offset);
}

ssize_t TemplateFile::write(int fd, char *buf, size_t len) {
    //log("TemplateFile::write [fd %d] [len %zu]", fd, len);

    off_t pos = originalInterface::original_lseek(fd, 0, SEEK_CUR);
    _ef_bk->lseek(_ef_fd, pos, SEEK_SET);
    _ef_bk->write(_ef_fd, buf, len);

    if(pos < CHECK_BUF_SIZE)
    {
        off64_t end = pos + len;
        size_t rlen = my_min(end, CHECK_BUF_SIZE) - pos;

        for(int i = pos; i < pos + rlen; i++)
        {
            flag_for_check[i] = true;
            buf_for_check[i] = ((char *)buf)[i - pos];
        }
    }

    return originalInterface::original_write(fd, buf, len);
}

bool TemplateFile::translate(int fd) {

    if(fd == 0) {
        fd = originalInterface::original_openat(AT_FDCWD, _path, O_WRONLY, 0);
        if (fd <= 0)
            return false;
    }

    int len = originalInterface::original_lseek(_ef_fd, 0, SEEK_END);
    originalInterface::original_lseek(_ef_fd, 0, SEEK_SET);
    int ori_pos = originalInterface::original_lseek(fd, 0, SEEK_CUR);
    originalInterface::original_lseek(fd, 0, SEEK_SET);
    log("judge : _ef_fd len = %d\n", len);
    int ret = originalInterface::original_sendfile(fd, _ef_fd, 0, len);
    originalInterface::original_lseek(fd, ori_pos, SEEK_SET);
    log("judge : translate [%s] sendfile return %d\n", _path, ret);
    fsync(fd);

    return ret == len;
}

void TemplateFile::close(bool flag) {

    if(flag) {
        int i;
        for (i = 0; i < CHECK_BUF_SIZE; i++) {
            if (flag_for_check[i] == false)
                break;
        }
        if (i == CHECK_BUF_SIZE)
            i--;

        if (doControl(i)) {
            translate(0);
        }
    }

    if(_ef_fd) {
        originalInterface::original_close(_ef_fd);
        //删除真实文件
        originalInterface::original_unlinkat(AT_FDCWD, _ef_bk->getPath(), 0);
        _ef_fd = 0;
    }

    if(_ef_bk)
    {
        delete _ef_bk;
        _ef_bk = 0;
    }
}

void TemplateFile::forceTranslate() {
    int i;
    for (i = 0; i < CHECK_BUF_SIZE; i++) {
        if (flag_for_check[i] == false)
            break;
    }
    if (i == CHECK_BUF_SIZE)
        i--;

    if (doControl(i)) {
        translate(0);
    }
}

int TemplateFile::ftruncate(int fd, off_t length) {
    //log("EncryptFile::ftruncate [fd %d] [length %ld", fd, length);

    _ef_bk->ftruncate(_ef_fd, length);

    return originalInterface::original_ftruncate(fd, length);
}

int TemplateFile::ftruncate64(int fd, off64_t length) {
    //log("EncryptFile::ftruncate64 [fd %d] [length %lld", fd, length);

    _ef_bk->ftruncate64(_ef_fd, length);

    return originalInterface::original_ftruncate64(fd, length);
}