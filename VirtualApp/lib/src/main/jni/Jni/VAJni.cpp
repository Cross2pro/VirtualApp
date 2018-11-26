#include <elf.h>//
// VirtualApp Native Project
//
#include <Foundation/IOUniformer.h>
#include <fb/include/fb/Build.h>
#include <fb/include/fb/ALog.h>
#include <fb/include/fb/fbjni.h>
#include <utils/controllerManagerNative.h>
#include <utils/zJNIEnv.h>
#include <utils/utils.h>
#include "VAJni.h"

using namespace facebook::jni;

jclass vskmClass;

static void jni_nativeLaunchEngine(alias_ref<jclass> clazz,JArrayClass<jobject> javaMethods,
                                   jstring packageName,
                                   jboolean isArt, jint apiLevel, jint cameraMethodType) {
    hookAndroidVM(javaMethods, packageName, isArt, apiLevel, cameraMethodType);
}


static void jni_nativeEnableIORedirect(alias_ref<jclass>, jstring selfSoPath, jint apiLevel,
                                       jint preview_api_level, jboolean need_dlopen) {
    ScopeUtfString so_path(selfSoPath);
    IOUniformer::startUniformer(so_path.c_str(), apiLevel, preview_api_level, need_dlopen ? 1 : 0);
}

static void jni_nativeIOWhitelist(alias_ref<jclass> jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::whitelist(path.c_str());
}

static void jni_nativeIOForbid(alias_ref<jclass> jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::forbid(path.c_str());
}


static void jni_nativeIORedirect(alias_ref<jclass> jclazz, jstring origPath, jstring newPath) {
    ScopeUtfString orig_path(origPath);
    ScopeUtfString new_path(newPath);
    IOUniformer::redirect(orig_path.c_str(), new_path.c_str());

}

static jstring jni_nativeGetRedirectedPath(alias_ref<jclass> jclazz, jstring origPath) {
    ScopeUtfString orig_path(origPath);
    const char *redirected_path = IOUniformer::query(orig_path.c_str());
    if (redirected_path != NULL) {
        return Environment::current()->NewStringUTF(redirected_path);
    }
    return NULL;
}

static jstring jni_nativeReverseRedirectedPath(alias_ref<jclass> jclazz, jstring redirectedPath) {
    ScopeUtfString redirected_path(redirectedPath);
    const char *orig_path = IOUniformer::reverse(redirected_path.c_str());
    return Environment::current()->NewStringUTF(orig_path);
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

alias_ref<jclass> nativeEngineClass;


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {

    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass VSKMClass = env->FindClass("com/lody/virtual/client/ipc/VSafekeyManager");
    vskmClass = (jclass) env->NewGlobalRef(VSKMClass);
    env->DeleteLocalRef(VSKMClass);

    zJNIEnv::initial(vm);
    controllerManagerNative::initial();

    return initialize(vm, [] {
        nativeEngineClass = findClassStatic("com/lody/virtual/client/NativeEngine");
        nativeEngineClass->registerNatives({
                        makeNativeMethod("nativeEnableIORedirect",
                                         jni_nativeEnableIORedirect),
                        makeNativeMethod("nativeIOWhitelist",
                                         jni_nativeIOWhitelist),
                        makeNativeMethod("nativeIOForbid",
                                         jni_nativeIOForbid),
                        makeNativeMethod("nativeIORedirect",
                                         jni_nativeIORedirect),
                        makeNativeMethod("nativeGetRedirectedPath",
                                         jni_nativeGetRedirectedPath),
                        makeNativeMethod("nativeReverseRedirectedPath",
                                         jni_nativeReverseRedirectedPath),
                        makeNativeMethod("nativeLaunchEngine",
                                         jni_nativeLaunchEngine),
                        makeNativeMethod("nativeCloseAllSocket",
                                         jni_nativeCloseAllSocket),
                        makeNativeMethod("nativeChangeDecryptState",
                                         jni_nativeChangeDecryptState),
                        makeNativeMethod("nativeGetDecryptState",
                                         jni_nativeGetDecryptState),

                }
        );
    });
}

extern "C" __attribute__((constructor)) void _init(void) {
    IOUniformer::init_env_before_all();
}


