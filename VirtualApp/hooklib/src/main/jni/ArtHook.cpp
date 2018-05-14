#ifndef ART_HOOK
#define ART_HOOK

#include <jni.h>
#include <stdio.h>
#include <cstring>
#include <sys/mman.h>
#include <ArtDlfcn.h>
#include <sys/user.h>
#include <malloc.h>
#include "ArtHook.h"
#include "ArtDlfcn.h"
#include <android/log.h>
#include <dlfcn.h>

#define TAG "VPosed"

#define ALOGD(format, ...) ((void)__android_log_print(ANDROID_LOG_INFO, TAG, format, __VA_ARGS__))
#define ALOGW(format, ...) ((void)__android_log_print(ANDROID_LOG_WARN, TAG, format, __VA_ARGS__))
#define ALOGE(format, ...) ((void)__android_log_print(ANDROID_LOG_ERROR, TAG, format, __VA_ARGS__))

#define kAccPublic 0x0001
#define kAccPrivate 0x0002
#define kAccStatic 0x0008
#define kAccFinal 0x0010
#define kAccNative 0x0100
#define kAccFastNative 0x00080000

#define STD_STRING_SIZE ((sizeof(void*) == 4) ? 12 : 24)

#define POINTER_SIZE sizeof(void*)
#define OFFSET_OF(object, offset) *(void**)((size_t)object + (offset))
#define SET_OF(object, offset, value) *(void**)((size_t)object + (offset)) = (void*)(value)
#define SET_OF_U2(object, offset, value) *(uint16_t*)((size_t)object + (offset)) = (uint16_t)(value)


typedef struct {
    intptr_t *sp;
    jclass methodClass;
    jobject hookedMethod;
    jobject backupMethod;
    bool isStatic;
    int paramCount;
    int *paramTypeIds;
    int returnType;
} HookInfo;

jobject handleJavaCallback(JNIEnv *env, HookInfo *info, jobjectArray paramArray);

static jobject
x86_convert_param_and_call_bridge(JNIEnv *env, HookInfo *info, intptr_t *args);

void *arm_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3);

jlong arm_long_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3);

jfloat arm_float_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3);

jdouble arm_double_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3);

void *x86_jni_bridge(HookInfo *info, int32_t *ebx);

jlong x86_long_jni_bridge(HookInfo *info, int32_t *ebx);

jfloat x86_float_jni_bridge(HookInfo *info, int32_t *ebx);

jdouble x86_double_jni_bridge(HookInfo *info, int32_t *ebx);

jobject boxArg(JNIEnv *env, int typeId, jvalue primitiveValue);

void *unboxArg(JNIEnv *env, HookInfo *info, jobject res);

jlong unboxLongArg(JNIEnv *env, HookInfo *info, jobject res);

class ScopedSuspendAll {
};


static struct {
    int apiLevel;
    JavaVM *vm;
    void *classLinker;
    void *quickGenericJniTrampoline;
    struct {
        size_t size;
        int hotnessCountOffset;
        int jniCodeOffset;
        int accessFlagsOffset;
        int quickCodeOffset;
    } ArtMethodSpec;
    struct {
        jclass artUtilsClass;
        jmethodID onCallback;
    } JavaEntry;

    struct {
        bool (*jit_compile_method_)(void *, void *, void *, bool);

        void (*suspendAll)(ScopedSuspendAll *, char *);

        void (*resumeAll)(ScopedSuspendAll *);
    } JitSpec;
} Instances;


enum TypeIds {
    Void = -1,
    Int = 0,
    Short = 1,
    Float = 2,
    Boolean = 3,
    Char = 4,
    Byte = 5,
    Long = 6,
    Double = 7,
    Object = 8,
};

ScopedSuspendAll *suspendAll() {
    ScopedSuspendAll *scope = (ScopedSuspendAll *) malloc(sizeof(ScopedSuspendAll));
    Instances.JitSpec.suspendAll(scope, "stop_jit");
    return scope;
}

void resumeAll(ScopedSuspendAll *scope) {
    Instances.JitSpec.resumeAll(scope);
}

void reserved0() {};

JNIEnv *getEnv() {
    JNIEnv *env;
    Instances.vm->AttachCurrentThread(&env, NULL);
    return env;
}

bool empty_jit_compile_method() {
    return false;
}


void *allocBuffer(int length) {
    return mmap(NULL, (size_t) length, PROT_READ | PROT_WRITE | PROT_EXEC,
                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);

}

int getTypeWidth(int type) {
    if (type == Long || type == Double) {
        return 2;
    }
    return 1;
}

inline int32_t getJniBridge(int type) {
#ifdef __arm__
    if (type == Double) {
        return (int32_t) arm_double_jni_bridge;
    }
    if (type == Float) {
        return (int32_t) arm_float_jni_bridge;
    }
    if (getTypeWidth(type) == 2) {
        return (int32_t) arm_long_jni_bridge;
    } else {
        return (int32_t) arm_jni_bridge;
    }
#elif defined(__i386__)
    if (type == Double) {
        return (int32_t) x86_double_jni_bridge;
    }
    if (type == Float) {
        return (int32_t) x86_float_jni_bridge;
    }
    if (getTypeWidth(type) == 2) {
        return (int32_t) x86_long_jni_bridge;
    } else {
        return (int32_t) x86_jni_bridge;
    }
#elif defined(__arm64__) || defined(__aarch64__)

#endif
}


jlong x86_long_jni_bridge(HookInfo *info, int32_t *ebx) {
    /**
        ebx[2] : JNIEnv *
        ebx[3] : jclass or jobject
     */
    JNIEnv *env = reinterpret_cast<JNIEnv *>(ebx[2]);
    int32_t *args = ebx + 3;
    jobject res = x86_convert_param_and_call_bridge(env, info, args);
    if (res == NULL) {
        return 0;
    }
    jclass clazz = env->FindClass("java/lang/Long");
    jmethodID method = env->GetMethodID(clazz, "longValue", "()J");
    return env->CallLongMethod(res, method);
}

jfloat x86_float_jni_bridge(HookInfo *info, int32_t *ebx) {
    /**
        ebx[2] : JNIEnv *
        ebx[3] : jclass or jobject
     */
    JNIEnv *env = reinterpret_cast<JNIEnv *>(ebx[2]);
    int32_t *args = ebx + 3;
    jobject res = x86_convert_param_and_call_bridge(env, info, args);
    if (res == NULL) {
        return 0;
    }
    jclass clazz = env->FindClass("java/lang/Float");
    jmethodID method = env->GetMethodID(clazz, "floatValue", "()F");
    return env->CallFloatMethod(res, method);
}

jdouble x86_double_jni_bridge(HookInfo *info, int32_t *ebx) {
    /**
        ebx[2] : JNIEnv *
        ebx[3] : jclass or jobject
     */
    JNIEnv *env = reinterpret_cast<JNIEnv *>(ebx[2]);
    int32_t *args = ebx + 3;
    jobject res = x86_convert_param_and_call_bridge(env, info, args);
    if (res == NULL) {
        return 0;
    }
    jclass clazz = env->FindClass("java/lang/Double");
    jmethodID method = env->GetMethodID(clazz, "doubleValue", "()D");
    return env->CallDoubleMethod(res, method);
}


void *x86_jni_bridge(HookInfo *info, int32_t *ebx) {
    /**
        ebx[2] : JNIEnv *
        ebx[3] : jclass or jobject
     */
    JNIEnv *env = reinterpret_cast<JNIEnv *>(ebx[2]);
    int32_t *args = ebx + 3;
    jobject res = x86_convert_param_and_call_bridge(env, info, args);
    void *value = unboxArg(env, info, res);
    return value;
}


void *generateJniBridge(HookInfo *hookInfo) {
#ifdef __arm__
    int32_t *buf = (int32_t *) allocBuffer(PAGE_SIZE);
    /**
        ldr r0, [pc, #4]
        str sp, [r0]
        ldr pc, [pc, #0]
        [HookInfo address]
        [jni_bridge address]
     */
    char code[] = "\x04\x00\x9f\xe5\x00\xd0\x80\xe5\x00\xf0\x9f\xe5\x00\xf0\x20\xe3\x00\xf0\x20\xe3";
    memcpy((void *) buf, code, sizeof(code));
    buf[3] = (int32_t) hookInfo;
    buf[4] = getJniBridge(hookInfo->returnType);
    return (void *) (buf);
#elif defined(__i386__)
    char *buf = (char *) allocBuffer(PAGE_SIZE);
        /**
            push ebx
            mov ebx, esp
            mov eax, x86_jni_bridge
            push ebx
            push eax
            mov eax, hookInfo
            call eax
            add esp, 8
            pop ebx
            ret
         */
        char code[] = "\x53\x89\xe3\xb8\x78\x56\x34\x12\x53\x50\xb8\x78\x56\x34\x12\xff\xd0\x83\xc4\x08\x5b\xc3";
        memcpy((void *) buf, code, sizeof(code));
        reinterpret_cast<int32_t *>(buf + 4)[0] = reinterpret_cast<int32_t>(hookInfo);
        reinterpret_cast<int32_t *>(buf + 11)[0] = reinterpret_cast<int32_t>(getJniBridge(hookInfo->returnType));
        return (void *) (buf);
#elif defined(__arm64__) || defined(__aarch64__)
    return 0;
#else
#error "Arch unknown, please port me"
#endif
}


jobject
hookMethod(JNIEnv *env, jclass clazz, jclass methodClass, jobject javaMethod, bool isStatic,
           int paramCount,
           jintArray jTypeIds, int returnType) {
    jint *typeIds = env->GetIntArrayElements(jTypeIds, NULL);
    HookInfo *info = new HookInfo;
    jmethodID method = env->FromReflectedMethod(javaMethod);
    void *backupMethod = malloc(Instances.ArtMethodSpec.size);
    memcpy(backupMethod, method, Instances.ArtMethodSpec.size);
    int accessFlags = (int) OFFSET_OF(method, Instances.ArtMethodSpec.accessFlagsOffset);
    jobject javaBackupMethod = env->ToReflectedMethod(methodClass, (jmethodID) backupMethod,
                                                      (jboolean) isStatic);
    info->backupMethod = env->NewGlobalRef(javaBackupMethod);
    info->hookedMethod = env->NewGlobalRef(javaMethod);
    info->isStatic = isStatic;
    info->paramCount = paramCount;
    info->paramTypeIds = typeIds;
    info->methodClass = (jclass) env->NewGlobalRef(methodClass);
    info->returnType = returnType;
    SET_OF(method, Instances.ArtMethodSpec.accessFlagsOffset,
           accessFlags | kAccNative | kAccFastNative);

    void *quickCode = OFFSET_OF(method, Instances.ArtMethodSpec.quickCodeOffset);
    if (quickCode != Instances.quickGenericJniTrampoline) {
        SET_OF(method, Instances.ArtMethodSpec.quickCodeOffset,
               Instances.quickGenericJniTrampoline);
    }
    SET_OF(method, Instances.ArtMethodSpec.jniCodeOffset, generateJniBridge(info));
    if (Instances.apiLevel >= 24) {
        /**
         * https://github.com/BBQDroid/art/blob/2db77c8ea444f4114a9c69958fb0190119721fc4/runtime/jit/profile_saver.cc#L359
         * hotness_count_ in native method must == 0.
         */
        SET_OF_U2(method, Instances.ArtMethodSpec.hotnessCountOffset, 0);
    }
    accessFlags &= ~kAccPublic;
    accessFlags |= kAccPrivate;
    SET_OF(backupMethod, Instances.ArtMethodSpec.accessFlagsOffset, accessFlags);
    return info->backupMethod;
}

bool initializeArtHookEnv(JavaVM *vm, JNIEnv *env, jclass artUtilsClass, int apiLevel) {
    Instances.apiLevel = apiLevel;
    Instances.JavaEntry.artUtilsClass = (jclass) env->NewGlobalRef(artUtilsClass);
    JNINativeMethod methods[] = {
            {"reserved0",  "()V",                                                          (void *) reserved0},
            {"hookMethod", "(Ljava/lang/Class;Ljava/lang/Object;ZI[II)Ljava/lang/Object;", (void *) hookMethod},
    };
    if (env->RegisterNatives(artUtilsClass, methods, 2) < 0) {
        return false;
    }
    if (apiLevel >= 24) {
        void *art_lib;
        void *jit_lib;
        if (POINTER_SIZE == sizeof(uint64_t)) {
            art_lib = fake_dlopen("/system/lib64/libart.so", RTLD_NOW);
            jit_lib = fake_dlopen("/system/lib64/libart-compiler.so", RTLD_NOW);
        } else {
            art_lib = fake_dlopen("/system/lib/libart.so", RTLD_NOW);
            jit_lib = fake_dlopen("/system/lib/libart-compiler.so", RTLD_NOW);
        }
        Instances.JitSpec.suspendAll = reinterpret_cast<void (*)(ScopedSuspendAll *,
                                                                 char *)>(fake_dlsym(art_lib,
                                                                                     "_ZN3art16ScopedSuspendAllC1EPKcb"));
        Instances.JitSpec.resumeAll = reinterpret_cast<void (*)(ScopedSuspendAll *)>(fake_dlsym(
                art_lib, "_ZN3art16ScopedSuspendAllD1Ev"));
        Instances.JitSpec.jit_compile_method_ = (bool (*)(void *, void *, void *, bool)) fake_dlsym(
                jit_lib, "jit_compile_method");
    }

    Instances.vm = vm;
    /*
     * class JavaVMExt {
     *      const struct JNIInvokeInterface_ *functions;
     *      Runtime* const runtime_;   <-- we need to find this
     *      ...
     * }
     */
    void *runtime = OFFSET_OF(vm, POINTER_SIZE);
    void *classLinker = NULL;
    void *internTable = NULL;

    /*
     * class Runtime {
     * ...
     * gc::Heap* heap_;                <-- we need to find this
     * std::unique_ptr<ArenaPool> jit_arena_pool_;     <----- API level >= 24
     * std::unique_ptr<ArenaPool> arena_pool_;             __
     * std::unique_ptr<ArenaPool> low_4gb_arena_pool_; <--|__ API level >= 23
     * std::unique_ptr<LinearAlloc> linear_alloc_;         \_
     * size_t max_spins_before_thin_lock_inflation_;
     * MonitorList* monitor_list_;
     * MonitorPool* monitor_pool_;
     * ThreadList* thread_list_;        <--- and these
     * InternTable* intern_table_;      <--/
     * ClassLinker* class_linker_;      <-/
     * SignalCatcher* signal_catcher_;
     * std::string stack_trace_file_;
     * JavaVMExt* java_vm_;             <-- so we find this then calculate our way backwards
     * ...
     * }
     */
    int startOffset = (POINTER_SIZE == 4) ? 200 : 384;
    int endOffset = startOffset + (100 * POINTER_SIZE);
    for (int offset = startOffset; offset != endOffset; offset += POINTER_SIZE) {
        if (OFFSET_OF(runtime, offset) == vm) {
            int classLinkerOffset = offset - STD_STRING_SIZE - (2 * POINTER_SIZE);
            if (apiLevel >= 27) {
                classLinkerOffset -= POINTER_SIZE;
            }
            int internTableOffset = classLinkerOffset - POINTER_SIZE;
            classLinker = OFFSET_OF(runtime, classLinkerOffset);
            internTable = OFFSET_OF(runtime, internTableOffset);
            break;
        }
    }
    if (classLinker == NULL || internTable == NULL) {
        return false;
    }
    Instances.classLinker = classLinker;
    /*
     * On Android 5.x:
     *
     * class ClassLinker {
     * ...
     * InternTable* intern_table_;                          <-- We find this then calculate our way forwards
     * const void* portable_resolution_trampoline_;
     * const void* quick_resolution_trampoline_;
     * const void* portable_imt_conflict_trampoline_;
     * const void* quick_imt_conflict_trampoline_;
     * const void* quick_generic_jni_trampoline_;           <-- ...to this
     * const void* quick_to_interpreter_bridge_trampoline_;
     * ...
     * }
     *
     * On Android 6.x and above:
     *
     * class ClassLinker {
     * ...
     * InternTable* intern_table_;                          <-- We find this then calculate our way forwards
     * const void* quick_resolution_trampoline_;
     * const void* quick_imt_conflict_trampoline_;
     * const void* quick_generic_jni_trampoline_;           <-- ...to this
     * const void* quick_to_interpreter_bridge_trampoline_;
     * ...
     * }
     */
    void *quickGenericJniTrampoline = NULL;
    startOffset = (POINTER_SIZE == 4) ? 100 : 200;
    endOffset = startOffset + (100 * POINTER_SIZE);
    for (int offset = startOffset; offset != endOffset; offset += POINTER_SIZE) {

        if (OFFSET_OF(classLinker, offset) == internTable) {
            int delta = (apiLevel >= 23) ? 3 : 5;
            int quickGenericJniTrampolineOffset = offset + (delta * POINTER_SIZE);
            quickGenericJniTrampoline = OFFSET_OF(classLinker, quickGenericJniTrampolineOffset);
            break;
        }
    }
    if (quickGenericJniTrampoline == NULL) {
        return false;
    }
    Instances.quickGenericJniTrampoline = quickGenericJniTrampoline;
    jclass processClass = env->FindClass("android/os/Process");
    size_t setArgV0 = (size_t) env->GetStaticMethodID(processClass, "setArgV0",
                                                      "(Ljava/lang/String;)V");
    int entrypointFieldSize = (apiLevel <= 21) ? 8 : POINTER_SIZE;
    int expectedAccessFlags = kAccPublic | kAccStatic | kAccFinal | kAccNative;
    int jniCodeOffset = -1;
    int accessFlagsOffset = -1;
    for (int offset = 0; offset != 64; offset += 4) {
        int *field = (int *) (setArgV0 + offset);
        int flags = *field;
        if (flags == expectedAccessFlags) {
            accessFlagsOffset = offset;
            break;
        }
    }
    if (accessFlagsOffset == -1) {
        return false;
    }
    size_t reserved0Method = (size_t) env->GetStaticMethodID(artUtilsClass, "reserved0", "()V");
    size_t reserved1Method = (size_t) env->GetStaticMethodID(artUtilsClass, "reserved1", "()V");
    for (int offset = 0; offset != 64; offset += 4) {
        if (OFFSET_OF(reserved0Method, offset) == reserved0) {
            jniCodeOffset = offset;
        }
    }
    if (jniCodeOffset == -1) {
        return false;
    }
    int quickCodeOffset = jniCodeOffset + entrypointFieldSize;
    size_t methodSize = reserved1Method - reserved0Method;
    int hotnessCountOffset = quickCodeOffset - POINTER_SIZE * 2 - sizeof(uint16_t);
    Instances.ArtMethodSpec.jniCodeOffset = jniCodeOffset;
    Instances.ArtMethodSpec.hotnessCountOffset = hotnessCountOffset;
    Instances.ArtMethodSpec.accessFlagsOffset = accessFlagsOffset;
    Instances.ArtMethodSpec.quickCodeOffset = quickCodeOffset;
    Instances.ArtMethodSpec.size = methodSize;
    Instances.JavaEntry.onCallback = env->GetStaticMethodID(artUtilsClass, "onCallback",
                                                            "(Ljava/lang/reflect/Member;[Ljava/lang/Object;)Ljava/lang/Object;");
    return true;
}


void readArg(jvalue *primitive, int typeId, intptr_t arg) {
    switch (typeId) {
        case Object:
            primitive->l = (jobject) arg;
            break;
        case Boolean:
            primitive->z = (jboolean) arg;
            break;
        case Char:
            primitive->c = (jchar) arg;
            break;
        case Int:
            primitive->i = (jint) arg;
            break;
        case Float:
            primitive->f = *reinterpret_cast<jfloat *>(&arg);
            break;
        case Byte:
            primitive->b = (jbyte) arg;
            break;
        case Short:
            primitive->s = (jshort) arg;
            break;
    }
}

void readLongArg(jvalue *primitive, int typeId, intptr_t arg1, intptr_t arg2) {
    if (typeId == Long) {
        jlong value;
        int32_t *buf = (int32_t *) &value;
        buf[0] = (int32_t) arg1;
        buf[1] = (int32_t) arg2;
        primitive->j = value;
    } else if (typeId == Double) {
        jdouble value;
        int32_t *buf = (int32_t *) &value;
        buf[0] = (int32_t) arg1;
        buf[1] = (int32_t) arg2;
        primitive->d = value;
    }
}

static jobject
x86_convert_param_and_call_bridge(JNIEnv *env, HookInfo *info, intptr_t *args) {
    int paramCount = info->paramCount;
    int *paramTypeIds = info->paramTypeIds;
    jobjectArray paramArray = env->NewObjectArray(paramCount, env->FindClass("java/lang/Object"),
                                                  NULL);
    int position = 0;
    for (int paramIndex = 0; paramIndex < paramCount; ++paramIndex) {
        jvalue primitive;
        jobject boxed;
        int typeId = paramTypeIds[paramIndex];
        if (getTypeWidth(typeId) == 2) {
            readLongArg(&primitive, typeId, args[position], args[position + 1]);
        } else {
            readArg(&primitive, typeId, args[position]);
        }
        boxed = boxArg(env, typeId, primitive);
        env->SetObjectArrayElement(paramArray, paramIndex, boxed);
        position += getTypeWidth(typeId);
    }
    return handleJavaCallback(env, info, paramArray);
}

inline intptr_t arm_getArg(int position, intptr_t r1, intptr_t r2, intptr_t r3, intptr_t *sp) {
    if (position == 0) {
        return r1;
    } else if (position == 1) {
        return r2;
    } else if (position == 2) {
        return r3;
    }
    return sp[position - 3];
}

#define ARM_GET_ARG(pos) arm_getArg((pos), r1, r2, r3, info->sp)

static jobject
arm_convert_param_and_call_bridge(JNIEnv *env, HookInfo *info, intptr_t r1, intptr_t r2,
                                  intptr_t r3) {
    int paramCount = info->paramCount;
    int *paramTypeIds = info->paramTypeIds;
    jobjectArray paramArray = env->NewObjectArray(paramCount, env->FindClass("java/lang/Object"),
                                                  NULL);
    int position = 0;
    for (int paramIndex = 0; paramIndex < paramCount; ++paramIndex) {
        jvalue primitive;
        jobject boxed;
        int typeId = paramTypeIds[paramIndex];
        if (getTypeWidth(typeId) == 2) {
            if (paramIndex > 1 && getTypeWidth(paramTypeIds[paramIndex - 1]) == 1) {
                if (position != 3 && position != 5) {
                    position++;
                }
            }
            readLongArg(&primitive, typeId, ARM_GET_ARG(position++), ARM_GET_ARG(position++));
        } else {
            readArg(&primitive, typeId, ARM_GET_ARG(position++));
        }
        boxed = boxArg(env, typeId, primitive);
        env->SetObjectArrayElement(paramArray, paramIndex, boxed);
    }
    return handleJavaCallback(env, info, paramArray);
}


jobject boxArg(JNIEnv *env, int typeId, jvalue primitiveValue) {
    switch (typeId) {
        case Object: {
            return primitiveValue.l;
        }
        case Boolean: {
            jclass clazz = env->FindClass("java/lang/Boolean");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(Z)Ljava/lang/Boolean;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.z);
        }
        case Char: {
            jclass clazz = env->FindClass("java/lang/Character");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf",
                                                       "(C)Ljava/lang/Character;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.c);
        }
        case Int: {
            jclass clazz = env->FindClass("java/lang/Integer");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.i);
        }
        case Float: {
            jclass clazz = env->FindClass("java/lang/Float");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(F)Ljava/lang/Float;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.f);
        }
        case Byte: {
            jclass clazz = env->FindClass("java/lang/Byte");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(B)Ljava/lang/Byte;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.b);
        }
        case Short: {
            jclass clazz = env->FindClass("java/lang/Short");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(S)Ljava/lang/Short;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.s);
        }
        case Double: {
            jclass clazz = env->FindClass("java/lang/Double");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(D)Ljava/lang/Double;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.d);
        }
        case Long: {
            jclass clazz = env->FindClass("java/lang/Long");
            jmethodID valueOf = env->GetStaticMethodID(clazz, "valueOf", "(J)Ljava/lang/Long;");
            return env->CallStaticObjectMethod(clazz, valueOf, primitiveValue.j);
        }
    }
}

void *unboxArg(JNIEnv *env, HookInfo *info, jobject res) {
    if (res == NULL) {
        return NULL;
    }
    switch (info->returnType) {
        case Void: {
            return NULL;
        }
        case Object: {
            return res;
        }
        case Int: {
            jclass clazz = env->FindClass("java/lang/Integer");
            jmethodID method = env->GetMethodID(clazz, "intValue", "()I");
            return (void *) env->CallIntMethod(res, method);
        }
        case Short: {
            jclass clazz = env->FindClass("java/lang/Short");
            jmethodID method = env->GetMethodID(clazz, "shortValue", "()S");
            return (void *) env->CallShortMethod(res, method);
        }
        case Boolean: {
            jclass clazz = env->FindClass("java/lang/Boolean");
            jmethodID method = env->GetMethodID(clazz, "booleanValue", "()Z");
            return (void *) env->CallBooleanMethod(res, method);
        }
        case Byte: {
            jclass clazz = env->FindClass("java/lang/Byte");
            jmethodID method = env->GetMethodID(clazz, "byteValue", "()B");
            return (void *) env->CallByteMethod(res, method);
        }
        case Char: {
            jclass clazz = env->FindClass("java/lang/Character");
            jmethodID method = env->GetMethodID(clazz, "charValue", "()C");
            return (void *) env->CallCharMethod(res, method);
        }
    }
    return NULL;
}

void *arm_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3) {
    JNIEnv *env = getEnv();
    jobject res = arm_convert_param_and_call_bridge(env, info, r1, r2, r3);
    return unboxArg(env, info, res);
}

jlong arm_long_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3) {
    JNIEnv *env = getEnv();
    jobject res = arm_convert_param_and_call_bridge(env, info, r1, r2, r3);
    if (res == NULL) {
        return 0;
    }
    jclass clazz = env->FindClass("java/lang/Long");
    jmethodID method = env->GetMethodID(clazz, "longValue", "()J");
    return env->CallLongMethod(res, method);
}

jdouble arm_double_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3) {
    JNIEnv *env = getEnv();
    jobject res = arm_convert_param_and_call_bridge(env, info, r1, r2, r3);
    if (res == NULL) {
        return 0;
    }
    jclass clazz = env->FindClass("java/lang/Double");
    jmethodID method = env->GetMethodID(clazz, "doubleValue", "()D");
    return env->CallDoubleMethod(res, method);
}

jfloat arm_float_jni_bridge(HookInfo *info, intptr_t r1, intptr_t r2, intptr_t r3) {
    JNIEnv *env = getEnv();
    jobject res = arm_convert_param_and_call_bridge(env, info, r1, r2, r3);
    if (res == NULL) {
        return 0;
    }
    jclass clazz = env->FindClass("java/lang/Float");
    jmethodID method = env->GetMethodID(clazz, "floatValue", "()F");
    return env->CallFloatMethod(res, method);
}

jobject handleJavaCallback(JNIEnv *env, HookInfo *info, jobjectArray paramArray) {
    jobject res = env->CallStaticObjectMethod(Instances.JavaEntry.artUtilsClass,
                                              Instances.JavaEntry.onCallback,
                                              info->hookedMethod, paramArray);
    return res;
}

void *getThreadSelf(JNIEnv *env) {
    jclass threadClass = env->FindClass("java/lang/Thread");
    jmethodID currentThreadMethod = env->GetStaticMethodID(threadClass, "currentThread",
                                                           "()Ljava/lang/Thread;");
    jobject currentThread = env->CallStaticObjectMethod(threadClass, currentThreadMethod);
    jfieldID nativePeerField = env->GetFieldID(threadClass, "nativePeer", "J");
    return (void *) env->GetLongField(currentThread, nativePeerField);
}

int getJniCodeOffset() {
    return Instances.ArtMethodSpec.jniCodeOffset;
}

static int32_t getApiLevel(JNIEnv *env) {
    jclass cls = env->FindClass("android/os/Build$VERSION");
    jfieldID SDK_INT_field = env->GetStaticFieldID(cls, "SDK_INT", "I");
    return env->GetStaticIntField(cls, SDK_INT_field);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    jclass clazz = env->FindClass("com/lody/hooklib/art/ArtUtils");
    initializeArtHookEnv(vm, env, clazz, getApiLevel(env));
    return JNI_VERSION_1_6;
}


#endif //ART_HOOK
