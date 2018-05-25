LOCAL_PATH := $(call my-dir)
MAIN_LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := va++

LOCAL_CFLAGS := -Wno-error=format-security -fpermissive
LOCAL_CFLAGS += -DLOG_TAG=\"VA++\"
LOCAL_CFLAGS += -fno-rtti -fno-exceptions
LOCAL_CFLAGS += -D_DEBUG_

LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Foundation
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Jni

LOCAL_SRC_FILES := Jni/VAJni.cpp \
				   Foundation/IOUniformer.cpp \
				   Foundation/VMHook.cpp \
				   Foundation/SymbolFinder.cpp \
				   Foundation/Path.cpp \
				   Foundation/SandboxFs.cpp \
				   Substrate/hde64.c \
                   Substrate/SubstrateDebug.cpp \
                   Substrate/SubstrateHook.cpp \
                   Substrate/SubstratePosixMemory.cpp \
                   transparentED/ff_Recognizer.cpp \
                   transparentED/EncryptFile.cpp \
                   transparentED/originalInterface.cpp \
                   transparentED/ctr/caesar_cipher.cpp \
                   transparentED/ctr/crypter.cpp \
                   transparentED/ctr/ctr.cpp \
                   transparentED/ctr/rng.cpp \
                   transparentED/ctr/SpookyV2.cpp \
                   transparentED/ctr/util.cpp \
                   transparentED/ctr/sm4.c \
                   transparentED/ctr/sm4_cipher.cpp \
                   transparentED/virtualFileSystem.cpp \
                   transparentED/fileCoder1.cpp \
                   transparentED/TemplateFile.cpp \
                   transparentED/IgnoreFile.cpp \
                   transparentED/encryptInfoMgr.cpp \
                   safekey/safekey_jni.cpp \
                   utils/zJNIEnv.cpp \
                   utils/utils.cpp \
                   utils/md5.c \
                   utils/zMd5.cpp \
                   utils/controllerManagerNative.cpp \

LOCAL_LDLIBS := -llog -latomic
LOCAL_STATIC_LIBRARIES := fb

include $(BUILD_SHARED_LIBRARY)
include $(MAIN_LOCAL_PATH)/fb/Android.mk
