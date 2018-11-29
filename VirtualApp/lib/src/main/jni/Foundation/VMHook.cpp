//
// VirtualApp Native Project
//
#include <Jni/VAJni.h>
#include <utils/controllerManagerNative.h>
#include "VMHook.h"
#include "SandboxFs.h"

namespace FunctionDef {
    typedef void (*Function_DalvikBridgeFunc)(const void **, void *, const void *, void *);

    typedef jobject (*JNI_openDexNativeFunc)(JNIEnv *, jclass, jstring, jstring, jint);

    typedef jobject (*JNI_openDexNativeFunc_N)(JNIEnv *, jclass, jstring, jstring, jint, jobject,
                                               jobject);

    typedef jint (*JNI_cameraNativeSetupFunc)(JNIEnv *, jobject,
                                              jobject, jobject, jobject, jobject, jobject,
                                              jobject, jobject, jobject);

    typedef void (*Function_cameraStartPreviewFunc)(JNIEnv *, jobject);
    typedef void (*Function_cameraNativeTakePictureFunc)(JNIEnv *, jobject, jint);
    typedef jint (*Function_audioRecordStartFunc)(JNIEnv *, jclass,jint,jint);
    typedef void (*Function_mediaRecorderPrepareFunc)(JNIEnv *, jclass);

    typedef jint (*JNI_getCallingUid)(JNIEnv *, jclass);

    typedef jint (*JNI_audioRecordNativeCheckPermission)(JNIEnv *, jobject, jstring);

    typedef jstring (*JNI_nativeLoad)(JNIEnv *env, jclass, jstring, jobject, jstring);
}

using namespace FunctionDef;


static struct {
    bool is_art;
    int native_offset;
    char *host_packageName;
    jint api_level;
    jmethodID method_onGetCallingUid;
    jmethodID method_onOpenDexFileNative;

    void *art_work_around_app_jni_bugs;

    char *(*GetCstrFromString)(void *);

    void *(*GetStringFromCstr)(const char *);

    int (*native_getCallingUid)(int);

    int (*IPCThreadState_self)(void);

    JNI_getCallingUid orig_getCallingUid;

    int cameraMethodType;
    int cameraMethodPkgIndex;
    Function_DalvikBridgeFunc orig_cameraNativeSetup_dvm;
    JNI_cameraNativeSetupFunc orig_cameraNativeSetupFunc;

    Function_cameraStartPreviewFunc orig_native_cameraStartPreviewFunc;
    Function_cameraNativeTakePictureFunc orig_native_cameraNativeTakePictureFunc;
    Function_audioRecordStartFunc orig_native_audioRecordStartFunc;
    Function_mediaRecorderPrepareFunc orig_native_mediaRecorderPrepareFunc;
    union {
        JNI_openDexNativeFunc beforeN;
        JNI_openDexNativeFunc_N afterN;
    } orig_openDexNativeFunc_art;

    Function_DalvikBridgeFunc orig_openDexFile_dvm;
    JNI_audioRecordNativeCheckPermission orig_audioRecordNativeCheckPermission;
    JNI_nativeLoad orig_nativeLoad;

} patchEnv;

jint dvm_getCallingUid(alias_ref<jclass> clazz) {
    jint uid = patchEnv.native_getCallingUid(patchEnv.IPCThreadState_self());
    uid = Environment::ensureCurrentThreadIsAttached()->CallStaticIntMethod(nativeEngineClass.get(),
                                                                            patchEnv.method_onGetCallingUid,
                                                                            uid);
    return uid;
}

jint new_getCallingUid(JNIEnv *env, jclass clazz) {
    int uid = patchEnv.orig_getCallingUid(Environment::ensureCurrentThreadIsAttached(), clazz);
    uid = Environment::ensureCurrentThreadIsAttached()->CallStaticIntMethod(nativeEngineClass.get(),
                                                                            patchEnv.method_onGetCallingUid,
                                                                            uid);
    return uid;
}


jstring new_nativeLoad(JNIEnv *env, jclass clazz, jstring _file, jobject classLoader, jstring _ld) {
    return patchEnv.orig_nativeLoad(env, clazz, _file, classLoader, _ld);
}

static void
new_bridge_openDexNativeFunc(const void **args, void *pResult, const void *method, void *self) {

    JNIEnv *env = Environment::ensureCurrentThreadIsAttached();

    const char *source = args[0] == NULL ? NULL : patchEnv.GetCstrFromString((void *) args[0]);
    const char *output = args[1] == NULL ? NULL : patchEnv.GetCstrFromString((void *) args[1]);

    jstring orgSource = source == NULL ? NULL : env->NewStringUTF(source);
    jstring orgOutput = output == NULL ? NULL : env->NewStringUTF(output);

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(2, stringClass, NULL);
    if (orgSource) {
        env->SetObjectArrayElement(array, 0, orgSource);
    }
    if (orgOutput) {
        env->SetObjectArrayElement(array, 1, orgOutput);
    }
    env->CallStaticVoidMethod(nativeEngineClass.get(), patchEnv.method_onOpenDexFileNative, array);

    jstring newSource = (jstring) env->GetObjectArrayElement(array, 0);
    jstring newOutput = (jstring) env->GetObjectArrayElement(array, 1);

    const char *_newSource = newSource == NULL ? NULL : env->GetStringUTFChars(newSource, NULL);
    const char *_newOutput = newOutput == NULL ? NULL : env->GetStringUTFChars(newOutput, NULL);

    args[0] = _newSource == NULL ? NULL : patchEnv.GetStringFromCstr(_newSource);
    args[1] = _newOutput == NULL ? NULL : patchEnv.GetStringFromCstr(_newOutput);

    if (source && orgSource) {
        env->ReleaseStringUTFChars(orgSource, source);
    }
    if (output && orgOutput) {
        env->ReleaseStringUTFChars(orgOutput, output);
    }

    patchEnv.orig_openDexFile_dvm(args, pResult, method, self);
}

static jobject new_native_openDexNativeFunc(JNIEnv *env, jclass jclazz, jstring javaSourceName,
                                            jstring javaOutputName, jint options) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(2, stringClass, NULL);

    if (javaSourceName) {
        env->SetObjectArrayElement(array, 0, javaSourceName);
    }
    if (javaOutputName) {
        env->SetObjectArrayElement(array, 1, javaOutputName);
    }
    env->CallStaticVoidMethod(nativeEngineClass.get(), patchEnv.method_onOpenDexFileNative, array);

    jstring newSource = (jstring) env->GetObjectArrayElement(array, 0);
    jstring newOutput = (jstring) env->GetObjectArrayElement(array, 1);

    return patchEnv.orig_openDexNativeFunc_art.beforeN(env, jclazz, newSource, newOutput,
                                                       options);
}

static jobject new_native_openDexNativeFunc_N(JNIEnv *env, jclass jclazz, jstring javaSourceName,
                                              jstring javaOutputName, jint options, jobject loader,
                                              jobject elements) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(2, stringClass, NULL);

    if (javaSourceName) {
        env->SetObjectArrayElement(array, 0, javaSourceName);
    }
    if (javaOutputName) {
        env->SetObjectArrayElement(array, 1, javaOutputName);
    }
    env->CallStaticVoidMethod(nativeEngineClass.get(), patchEnv.method_onOpenDexFileNative, array);

    jstring newSource = (jstring) env->GetObjectArrayElement(array, 0);
    jstring newOutput = (jstring) env->GetObjectArrayElement(array, 1);

    return patchEnv.orig_openDexNativeFunc_art.afterN(env, jclazz, newSource, newOutput, options,
                                                      loader, elements);
}

static void
new_bridge_cameraNativeSetupFunc(const void **args, void *pResult, const void *method, void *self) {
    jint index = patchEnv.cameraMethodPkgIndex + 1;
    args[index] = patchEnv.GetStringFromCstr(patchEnv.host_packageName);
    patchEnv.orig_cameraNativeSetup_dvm(args, pResult, method, self);
}

static jint new_native_cameraNativeSetupFunc_T(JNIEnv *env, jobject thiz,
                                               jobject o1, jobject o2, jobject o3, jobject o4,
                                               jobject o5,
                                               jobject o6, jobject o7, jobject o8) {
    jint index = patchEnv.cameraMethodPkgIndex;
    if (!controllerManagerNative::isCameraEnable()) {
        ALOGE("cameraNativeSetupFunc");
        return -19;
    }
    if (index >= 0) {
        jstring host = env->NewStringUTF(patchEnv.host_packageName);
        switch (index) {
            case 0:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, host, o2, o3, o4, o5, o6, o7,
                                                           o8);
            case 1:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, host, o3, o4, o5, o6, o7,
                                                           o8);
            case 2:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, host, o4, o5, o6, o7,
                                                           o8);
            case 3:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, o3, host, o5, o6, o7,
                                                           o8);
            case 4:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, o3, o4, host, o6, o7,
                                                           o8);
            case 5:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, o3, o4, o5, host, o7,
                                                           o8);
            case 6:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, o3, o4, o5, o6, host,
                                                           o8);
            case 7:
                return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, o3, o4, o5, o6, o7,
                                                           host);
        }
    }
    return patchEnv.orig_cameraNativeSetupFunc(env, thiz, o1, o2, o3, o4, o5, o6, o7, o8);
}


static void new_native_cameraStartPreviewFunc(JNIEnv *env, jobject thiz) {
    if (!controllerManagerNative::isCameraEnable()) {
        ALOGE("cameraStartPreviewFunc");
        return;
    }
    patchEnv.orig_native_cameraStartPreviewFunc(env, thiz);
}

static void new_native_cameraNativePictureFunc(JNIEnv *env, jobject thiz, jint msgType) {
    patchEnv.orig_native_cameraNativeTakePictureFunc(env, thiz, msgType);
}
static int new_native_audioRecordNativeStartFunc(JNIEnv *env, jclass thiz, jint syncEvent,jint sessionId) {
    ALOGE("audioRecordNativeStartFunc in");
    if (!controllerManagerNative::isSoundRecordEnable()) {
        ALOGE("audioRecordNativeStartFunc return");
        return -1;
    }
    return patchEnv.orig_native_audioRecordStartFunc(env, thiz, syncEvent,sessionId);
}
static void new_native_mediaRecorderNativePrepareFunc(JNIEnv *env, jclass thiz) {
    ALOGE("mediaRecorderNativePrepareFunc in");
    if (!controllerManagerNative::isSoundRecordEnable()) {
        ALOGE("mediaRecorderNativePrepareFunc return");
        jclass  newExcCls = env->FindClass("java/io/IOException");
        env->ThrowNew(newExcCls, "setOutputFile failed.");
        return;
    }
    patchEnv.orig_native_mediaRecorderPrepareFunc(env, thiz);
    ALOGE("mediaRecorderNativePrepareFunc out");
}

static jint
new_native_audioRecordNativeCheckPermission(JNIEnv *env, jobject thiz, jstring _packagename) {
    jstring host = env->NewStringUTF(patchEnv.host_packageName);
    return patchEnv.orig_audioRecordNativeCheckPermission(env, thiz, host);
}


void mark() {
    // Do nothing
};


void measureNativeOffset(bool isArt) {

    jmethodID markMethod = nativeEngineClass->getStaticMethod<void(void)>("nativeMark").getId();

    size_t start = (size_t) markMethod;
    size_t target = (size_t) mark;

    if (isArt && patchEnv.art_work_around_app_jni_bugs) {
        target = (size_t) patchEnv.art_work_around_app_jni_bugs;
    }

    int offset = 0;
    bool found = false;
    while (true) {
        if (*((size_t *) (start + offset)) == target) {
            found = true;
            break;
        }
        offset += 4;
        if (offset >= 100) {
            ALOGE("Error: Cannot find the jni function offset.");
            break;
        }
    }
    if (found) {
        patchEnv.native_offset = offset;
        if (!isArt) {
            patchEnv.native_offset += (sizeof(int) + sizeof(void *));
        }
    }
}

void vmUseJNIFunction(jmethodID method, void *jniFunction) {
    void **funPtr = (void **) (reinterpret_cast<size_t>(method) + patchEnv.native_offset);
    *funPtr = jniFunction;
}

void *vmGetJNIFunction(jmethodID method) {
    void **funPtr = (void **) (reinterpret_cast<size_t>(method) + patchEnv.native_offset);
    return *funPtr;
}


void hookJNIMethod(jmethodID method, void *new_jni_func, void **orig_jni_func) {
    *orig_jni_func = vmGetJNIFunction(method);
    vmUseJNIFunction(method, new_jni_func);
}


void hookGetCallingUid(jboolean isArt) {
    if (isArt) {
        JNIEnv *env = Environment::current();
        jclass binderClass = env->FindClass("android/os/Binder");
        jmethodID getCallingUid = env->GetStaticMethodID(binderClass, "getCallingUid", "()I");
        hookJNIMethod(getCallingUid,
                      (void *) new_getCallingUid,
                      (void **) &patchEnv.orig_getCallingUid
        );
    } else {
        auto binderClass = findClassLocal("android/os/Binder");
        binderClass->registerNatives({makeNativeMethod("getCallingUid", dvm_getCallingUid)});
    }
}

void hookOpenDexFileNative(jobject javaMethod, jboolean isArt, int apiLevel) {

    if (!isArt) {
        size_t mtd_openDexNative = (size_t) Environment::current()->FromReflectedMethod(javaMethod);
        int nativeFuncOffset = patchEnv.native_offset;
        void **jniFuncPtr = (void **) (mtd_openDexNative + nativeFuncOffset);
        patchEnv.orig_openDexFile_dvm = (Function_DalvikBridgeFunc) (*jniFuncPtr);
        *jniFuncPtr = (void *) new_bridge_openDexNativeFunc;
    } else {
        jmethodID method = Environment::current()->FromReflectedMethod(javaMethod);
        void *jniFunc = vmGetJNIFunction(method);
        if (apiLevel < 24) {
            patchEnv.orig_openDexNativeFunc_art.beforeN = (JNI_openDexNativeFunc) (jniFunc);
            vmUseJNIFunction(method, (void *) new_native_openDexNativeFunc);
        } else {
            patchEnv.orig_openDexNativeFunc_art.afterN = (JNI_openDexNativeFunc_N) (jniFunc);
            vmUseJNIFunction(method, (void *) new_native_openDexNativeFunc_N);
        }
    }
}

void hookRuntimeNativeLoad() {
    if (patchEnv.is_art) {
        JNIEnv *env = Environment::current();
        jclass runtimeClass = env->FindClass("java/lang/Runtime");
        jmethodID nativeLoad = env->GetStaticMethodID(runtimeClass, "nativeLoad",
                                                      "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/String;");
        env->ExceptionClear();
        if (nativeLoad) {
            hookJNIMethod(nativeLoad, (void *) new_nativeLoad, (void **) &patchEnv.orig_nativeLoad);
        } else {
            ALOGE("Error: cannot find nativeLoad method.");
        }
    }
}

inline void
hookCameraNativeSetup(jobject javaMethod, jboolean isArt, int apiLevel) {
    if (!javaMethod) {
        return;
    }
    if (!isArt) {
        size_t mtd_cameraNativeSetup = (size_t) Environment::current()->FromReflectedMethod(
                javaMethod);
        int nativeFuncOffset = patchEnv.native_offset;
        void **jniFuncPtr = (void **) (mtd_cameraNativeSetup + nativeFuncOffset);

        patchEnv.orig_cameraNativeSetup_dvm = (Function_DalvikBridgeFunc) (*jniFuncPtr);
        *jniFuncPtr = (void *) new_bridge_cameraNativeSetupFunc;
    } else {
        jmethodID method = Environment::current()->FromReflectedMethod(javaMethod);
        hookJNIMethod(method,
                      (void *) new_native_cameraNativeSetupFunc_T,
                      (void **) &patchEnv.orig_cameraNativeSetupFunc
        );
    }
}

inline void
hookCameraStartPreviewMethod(jobject javaMethod, jboolean isArt, int apiLevel) {

    if (!javaMethod) {
        return;
    }
    size_t mtd_cameraStartPreview = (size_t) Environment::current()->FromReflectedMethod(javaMethod);
    int nativeFuncOffset = patchEnv.native_offset;
    void **jniFuncPtr = (void **) (mtd_cameraStartPreview + nativeFuncOffset);

    if (!isArt) {

    } else {
        patchEnv.orig_native_cameraStartPreviewFunc = (Function_cameraStartPreviewFunc) (*jniFuncPtr);
        *jniFuncPtr = (void *) new_native_cameraStartPreviewFunc;
    }

}

inline void
hookCameraNativeTakePictureMethod(jobject javaMethod, jboolean isArt, int apiLevel) {

    if (!javaMethod) {
        return;
    }
    size_t mtd_cameraNativeTakePicture = (size_t) Environment::current()->FromReflectedMethod(javaMethod);
    int nativeFuncOffset = patchEnv.native_offset;
    void **jniFuncPtr = (void **) (mtd_cameraNativeTakePicture + nativeFuncOffset);

    if (!isArt) {

    } else {
        patchEnv.orig_native_cameraNativeTakePictureFunc = (Function_cameraNativeTakePictureFunc) (*jniFuncPtr);
        *jniFuncPtr = (void *) new_native_cameraNativePictureFunc;
    }

}
inline void
hookAudioRecordStartMethod(jobject javaMethod, jboolean isArt, int apiLevel) {

    if (!javaMethod) {
        return;
    }
    size_t mtd_audioRecordNativeStart = (size_t) Environment::current()->FromReflectedMethod(javaMethod);
    int nativeFuncOffset = patchEnv.native_offset;
    void **jniFuncPtr = (void **) (mtd_audioRecordNativeStart + nativeFuncOffset);

    if (!isArt) {

    } else {
        patchEnv.orig_native_audioRecordStartFunc = (Function_audioRecordStartFunc) (*jniFuncPtr);
        *jniFuncPtr = (void *) new_native_audioRecordNativeStartFunc;
    }

}

inline void
hookMediaRecorderPrepareMethod(jobject javaMethod, jboolean isArt, int apiLevel) {

    if (!javaMethod) {
        return;
    }
    size_t mtd_mediaRecorderNativePrepare = (size_t) Environment::current()->FromReflectedMethod(javaMethod);
    int nativeFuncOffset = patchEnv.native_offset;
    void **jniFuncPtr = (void **) (mtd_mediaRecorderNativePrepare + nativeFuncOffset);

    if (!isArt) {

    } else {
        patchEnv.orig_native_mediaRecorderPrepareFunc = (Function_mediaRecorderPrepareFunc) (*jniFuncPtr);
        *jniFuncPtr = (void *) new_native_mediaRecorderNativePrepareFunc;
    }

}

void
hookAudioRecordNativeCheckPermission(jobject javaMethod, jboolean isArt, int api) {
    if (!javaMethod || !isArt) {
        return;
    }
    jmethodID method = Environment::current()->FromReflectedMethod(javaMethod);
    hookJNIMethod(method,
                  (void *) new_native_audioRecordNativeCheckPermission,
                  (void **) &patchEnv.orig_audioRecordNativeCheckPermission
    );
}

void *getDvmOrArtSOHandle() {
    char so_name[25] = {0};
    __system_property_get("persist.sys.dalvik.vm.lib.2", so_name);
    if (strlen(so_name) == 0) {
        __system_property_get("persist.sys.dalvik.vm.lib", so_name);
    }
    void *soInfo = dlopen(so_name, 0);
    if (!soInfo) {
        soInfo = RTLD_DEFAULT;
    }
    return soInfo;
}

/**
 * Only called once.
 * @param javaMethod Method from Java
 * @param isArt Dalvik or Art
 * @param apiLevel Api level from Java
 */
void hookAndroidVM(JArrayClass<jobject> javaMethods,
                   jstring packageName, jboolean isArt, jint apiLevel,
                   jint cameraMethodType) {

    void (*libdl_set_sdk_version)(uint32_t) = (void (*)(uint32_t)) (dlsym(RTLD_DEFAULT,
                                                                          "android_set_application_target_sdk_version"));
    if (libdl_set_sdk_version && apiLevel > 23) {
        libdl_set_sdk_version(23);
    }

    JNIEnv *env = Environment::current();

    JNINativeMethod methods[] = {
            NATIVE_METHOD((void *) mark, "nativeMark", "()V"),
    };
    if (env->RegisterNatives(nativeEngineClass.get(), methods, 1) < 0) {
        return;
    }
    patchEnv.is_art = isArt;
    patchEnv.cameraMethodType = cameraMethodType;
    if (cameraMethodType >= 0x10) {
        patchEnv.cameraMethodPkgIndex = cameraMethodType - 0x10;
    } else {
        if (patchEnv.cameraMethodType == 2 || patchEnv.cameraMethodType == 3) {
            patchEnv.cameraMethodPkgIndex = 3;
        } else {
            patchEnv.cameraMethodPkgIndex = 2;
        }
    }

    patchEnv.host_packageName = (char *) env->GetStringUTFChars(packageName,
                                                                NULL);
    patchEnv.api_level = apiLevel;
    patchEnv.method_onGetCallingUid = nativeEngineClass->getStaticMethod<jint(jint)>(
            "onGetCallingUid").getId();
    patchEnv.method_onOpenDexFileNative = env->GetStaticMethodID(nativeEngineClass.get(),
                                                                 "onOpenDexFileNative",
                                                                 "([Ljava/lang/String;)V");

    if (!isArt) {
        // workaround for dlsym returns null when system has libhoudini
        void *h = dlopen("/system/lib/libandroid_runtime.so", RTLD_LAZY);
        {
            patchEnv.IPCThreadState_self = (int (*)(void)) dlsym(RTLD_DEFAULT,
                                                                 "_ZN7android14IPCThreadState4selfEv");
            patchEnv.native_getCallingUid = (int (*)(int)) dlsym(RTLD_DEFAULT,
                                                                 "_ZNK7android14IPCThreadState13getCallingUidEv");
            if (patchEnv.native_getCallingUid == NULL) {
                patchEnv.native_getCallingUid = (int (*)(int)) dlsym(RTLD_DEFAULT,
                                                                     "_ZN7android14IPCThreadState13getCallingUidEv");
            }
        }
        if (h != NULL) {
            dlclose(h);
        }
        void *soInfo = getDvmOrArtSOHandle();
        patchEnv.GetCstrFromString = (char *(*)(void *)) dlsym(soInfo,
                                                               "_Z23dvmCreateCstrFromStringPK12StringObject");
        if (!patchEnv.GetCstrFromString) {
            patchEnv.GetCstrFromString = (char *(*)(void *)) dlsym(soInfo,
                                                                   "dvmCreateCstrFromString");
        }
        patchEnv.GetStringFromCstr = (void *(*)(const char *)) dlsym(soInfo,
                                                                     "_Z23dvmCreateStringFromCstrPKc");
        if (!patchEnv.GetStringFromCstr) {
            patchEnv.GetStringFromCstr = (void *(*)(const char *)) dlsym(soInfo,
                                                                         "dvmCreateStringFromCstr");
        }
    }
    measureNativeOffset(isArt);
    hookGetCallingUid(isArt);
    hookOpenDexFileNative(javaMethods.getElement(OPEN_DEX).get(), isArt, apiLevel);
    hookCameraNativeSetup(javaMethods.getElement(CAMERA_SETUP).get(), isArt, apiLevel);
    hookCameraStartPreviewMethod(javaMethods.getElement(CAMERA_STARTPREVIEW).get(),
                                    isArt, apiLevel);
    hookCameraNativeTakePictureMethod(javaMethods.getElement(CAMERA_TAKEPICTURE).get(),
                                         isArt, apiLevel);
    hookAudioRecordStartMethod(javaMethods.getElement(AUDIO_RECORD_START).get(),isArt, apiLevel);
    hookMediaRecorderPrepareMethod(javaMethods.getElement(MEDIA_RECORDER_PREPARE).get(),isArt, apiLevel);
    hookAudioRecordNativeCheckPermission(
            javaMethods.getElement(AUDIO_NATIVE_CHECK_PERMISSION).get(), isArt, apiLevel);
    hookRuntimeNativeLoad();
}
