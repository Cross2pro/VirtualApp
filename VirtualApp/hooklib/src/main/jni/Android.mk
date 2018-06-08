LOCAL_PATH := $(call my-dir)
MAIN_LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := vposed_art

LOCAL_CFLAGS := -Wno-error=format-security -fpermissive -DLOG_TAG=\"VA++\"
LOCAL_CFLAGS += -fno-rtti -fno-exceptions

LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Foundation
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Jni

LOCAL_SRC_FILES := ArtHook.cpp \
                   ArtDlfcn.cpp  \

LOCAL_LDLIBS := -llog -latomic

include $(BUILD_SHARED_LIBRARY)