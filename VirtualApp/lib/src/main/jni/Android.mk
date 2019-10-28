LOCAL_PATH := $(call my-dir)
MAIN_LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_MODULE := v++_64
LOCAL_CFLAGS := -DCORE_SO_NAME=\"libv++_64.so\"
else
LOCAL_MODULE := v++
LOCAL_CFLAGS := -DCORE_SO_NAME=\"libv++.so\"
endif

#LOCAL_SHORT_COMMANDS := true

LOCAL_CFLAGS += -Wno-error=format-security -fpermissive -O2
LOCAL_CFLAGS += -DLOG_TAG=\"VA++\"
LOCAL_CFLAGS += -fno-rtti -fno-exceptions
LOCAL_CPPFLAGS += -std=c++11

LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Foundation
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Jni
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/asm
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/decoder
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/elf
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/utils
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/includes
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/buffer
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/antihook
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm64/inst
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm64/register
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm64/decoder
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm64/assembler
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm64/relocate
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm64/hook
else
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm32/inst
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm32/register
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm32/assembler
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm32/decoder
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm32/hook
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/sandhook/archs/arm/arm32/relocate
endif


LOCAL_SRC_FILES := Jni/VAJni.cpp \
				   Jni/Helper.cpp \
				   Foundation/syscall/BinarySyscallFinder.cpp \
				   Foundation/fake_dlfcn.cpp \
				   Foundation/canonicalize_md.c \
				   Foundation/MapsRedirector.cpp \
				   Foundation/IORelocator.cpp \
				   Foundation/VMHook.cpp \
				   Foundation/Symbol.cpp \
				   Foundation/SandboxFs.cpp \
				   Substrate/hde64.c \
                   Substrate/SubstrateDebug.cpp \
                   Substrate/SubstrateHook.cpp \
                   Substrate/SubstratePosixMemory.cpp \
                   Substrate/And64InlineHook.cpp \
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
                   sandhook/sandhook_native.cpp \
                   sandhook/decoder/decoder.cpp \
                   sandhook/relocate/code_relocate.cpp \
                   sandhook/elf/elf.cpp \
                   sandhook/assembler/assembler.cpp \
                   sandhook/buffer/code_buffer.cpp \
                   sandhook/utils/platform.cpp \
                   sandhook/hook/hook.cpp

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES += sandhook/archs/arm/arm64/assembler/assembler_arm64.cpp \
                      sandhook/archs/arm/arm64/inst/inst_arm64.cpp \
                      sandhook/archs/arm/arm64/register/register_arm64.cpp \
                      sandhook/archs/arm/arm64/register/register_list_arm64.cpp \
                      sandhook/archs/arm/arm64/decoder/decoder_arm64.cpp \
                      sandhook/archs/arm/arm64/relocate/code_relocate_arm64.cpp \
                      sandhook/archs/arm/arm64/hook/hook_arm64.cpp
else

LOCAL_SRC_FILES += sandhook/archs/arm/arm32/inst/inst_arm32.cpp \
                      sandhook/archs/arm/arm32/inst/inst_t32.cpp \
                      sandhook/archs/arm/arm32/inst/inst_t16.cpp \
                      sandhook/archs/arm/arm32/register/register_arm32.cpp \
                      sandhook/archs/arm/arm32/register/register_list_arm32.cpp \
                      sandhook/archs/arm/arm32/assembler/assembler_arm32.cpp \
                      sandhook/archs/arm/arm32/decoder/decoder_arm32.cpp \
                      sandhook/archs/arm/arm32/hook/hook_arm32.cpp \
                      sandhook/archs/arm/arm32/hook/breakpoint_shellcode.S \
                      sandhook/archs/arm/arm32/relocate/code_relocate_arm32.cpp
endif

LOCAL_LDLIBS := -llog -latomic

include $(BUILD_SHARED_LIBRARY)