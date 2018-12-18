
// VirtualApp Native Project
//
#include <Foundation/IORelocator.h>
#include "VAJni.h"

#include <utils/controllerManagerNative.h>
#include <utils/zJNIEnv.h>
#include <utils/utils.h>

static void jni_nativeLaunchEngine(JNIEnv *env, jclass clazz, jobjectArray javaMethods,
                                   jstring packageName,
                                   jboolean isArt, jint apiLevel, jint cameraMethodType,
                                   jint audioRecordMethodType) {
    hookAndroidVM(env, javaMethods, packageName, isArt, apiLevel, cameraMethodType,
                  audioRecordMethodType);
}


static void
jni_nativeEnableIORedirect(JNIEnv *env, jclass, jstring soPath, jstring soPath64, jstring nativePath, jint apiLevel,
                           jint preview_api_level) {
    ScopeUtfString so_path(soPath);
    ScopeUtfString so_path_64(soPath64);
    ScopeUtfString native_path(nativePath);
    IOUniformer::startUniformer(so_path.c_str(), so_path_64.c_str(), native_path.c_str(), apiLevel, preview_api_level);
}

static void jni_nativeIOWhitelist(JNIEnv *env, jclass jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::whitelist(path.c_str());
}

static void jni_nativeIOForbid(JNIEnv *env, jclass jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::forbid(path.c_str());
}

static void jni_nativeIOReadOnly(JNIEnv *env, jclass jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::readOnly(path.c_str());
}


static void jni_nativeIORedirect(JNIEnv *env, jclass jclazz, jstring origPath, jstring newPath) {
    ScopeUtfString orig_path(origPath);
    ScopeUtfString new_path(newPath);
    IOUniformer::relocate(orig_path.c_str(), new_path.c_str());

}

static jstring jni_nativeGetRedirectedPath(JNIEnv *env, jclass jclazz, jstring origPath) {
    ScopeUtfString orig_path(origPath);
    char buffer[PATH_MAX];
    const char *redirected_path = IOUniformer::query(orig_path.c_str(), buffer, sizeof(buffer));
    if (redirected_path != NULL) {
        return env->NewStringUTF(redirected_path);
    }
    return NULL;
}

static jstring jni_nativeReverseRedirectedPath(JNIEnv *env, jclass jclazz, jstring redirectedPath) {
    ScopeUtfString redirected_path(redirectedPath);
    char buffer[PATH_MAX];
    const char *orig_path = IOUniformer::reverse(redirected_path.c_str(), buffer, sizeof(buffer));
    return env->NewStringUTF(orig_path);
}

static void jni_bypassHiddenAPIEnforcementPolicy(JNIEnv *env, jclass jclazz) {
    bypassHiddenAPIEnforcementPolicy();
}

static jboolean jni_nativeCloseAllSocket(JNIEnv *env, jclass jclazz){
    return (jboolean)closeAllSockets();
}
static void jni_nativeChangeDecryptState(JNIEnv *env,jclass jclazz,jboolean state){
    changeDecryptState(state,0);
}
static jboolean jni_nativeGetDecryptState(JNIEnv *env,jclass jclazz){
    return getDecryptState();
}

jclass nativeEngineClass;
JavaVM *vm;

jclass vskmClass;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *_vm, void *) {
    vm = _vm;
    JNIEnv *env;
    _vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    nativeEngineClass = (jclass) env->NewGlobalRef(env->FindClass(JNI_CLASS_NAME));
    static JNINativeMethod methods[] = {
            {"nativeLaunchEngine",                     "([Ljava/lang/Object;Ljava/lang/String;ZIII)V",                (void *) jni_nativeLaunchEngine},
            {"nativeReverseRedirectedPath",            "(Ljava/lang/String;)Ljava/lang/String;",                      (void *) jni_nativeReverseRedirectedPath},
            {"nativeGetRedirectedPath",                "(Ljava/lang/String;)Ljava/lang/String;",                      (void *) jni_nativeGetRedirectedPath},
            {"nativeIORedirect",                       "(Ljava/lang/String;Ljava/lang/String;)V",                     (void *) jni_nativeIORedirect},
            {"nativeIOWhitelist",                      "(Ljava/lang/String;)V",                                       (void *) jni_nativeIOWhitelist},
            {"nativeIOForbid",                         "(Ljava/lang/String;)V",                                       (void *) jni_nativeIOForbid},
            {"nativeIOReadOnly",                       "(Ljava/lang/String;)V",                                       (void *) jni_nativeIOReadOnly},
            {"nativeEnableIORedirect",                 "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", (void *) jni_nativeEnableIORedirect},
            {"nativeBypassHiddenAPIEnforcementPolicy", "()V",                                                         (void *) jni_bypassHiddenAPIEnforcementPolicy},
            {"nativeGetDecryptState",                  "()Z",                                                         (void *) jni_nativeGetDecryptState},
            {"nativeChangeDecryptState",               "(Z)V",                                                        (void *) jni_nativeChangeDecryptState},
            {"nativeCloseAllSocket",                   "()Z",                                                         (void *) jni_nativeCloseAllSocket},
    };

    if (env->RegisterNatives(nativeEngineClass, methods, 9) < 0) {
        return JNI_ERR;
    }

    jclass VSKMClass = env->FindClass("com/lody/virtual/client/ipc/VSafekeyManager");
    vskmClass = (jclass) env->NewGlobalRef(VSKMClass);
    env->DeleteLocalRef(VSKMClass);

    zJNIEnv::initial(vm);
    controllerManagerNative::initial();

    return JNI_VERSION_1_6;
}

JNIEnv *getEnv() {
    JNIEnv *env;
    vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *ensureEnvCreated() {
    JNIEnv *env = getEnv();
    if (env == NULL) {
        vm->AttachCurrentThread(&env, NULL);
    }
    return env;
}

extern "C" __attribute__((constructor)) void _init(void) {
    IOUniformer::init_env_before_all();
}


