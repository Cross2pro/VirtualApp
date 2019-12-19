LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := sandhook
LOCAL_CFLAGS += -Wno-error=format-security -fpermissive -O2
LOCAL_CFLAGS += -fno-rtti -fno-exceptions
LOCAL_CPPFLAGS += -std=c++11

LOCAL_SRC_FILES := sandhook.cpp \
        trampoline/trampoline.cpp \
        trampoline/trampoline_manager.cpp \
        utils/dlfcn_nougat.cpp \
        utils/hide_api.cpp \
        utils/utils.cpp \
        utils/offset.cpp \
        utils/elf_util.cpp \
        casts/cast_art_method.cpp \
        casts/cast_compiler_options.cpp \
        art/art_method.cpp \
        art/art_compiler_options.cpp \
        trampoline/arch/arm32.S \
        trampoline/arch/arm64.S \
        inst/insts_arm32.cpp \
        inst/insts_arm64.cpp \
        nativehook/native_hook.cpp

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)