#ifndef ART_HOOK
#define ART_HOOK

#include <jni.h>

bool initializeArtHookEnv(JavaVM *vm, JNIEnv *env, int apiLevel);

int getJniCodeOffset();

jobject
hookMethod(JNIEnv *env, jclass methodClass, jobject javaMethod, bool isStatic, int paramCount,
           int *typeIds, int returnType);

#endif //ART_HOOK
