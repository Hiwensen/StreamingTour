
#ifndef STREAMING_NATIVECALLJAVA_H
#define STREAMING_NATIVECALLJAVA_H

#include "jni.h"
#include <linux/stddef.h>
#include "AndroidLog.h"

#define MAIN_THREAD 0
#define CHILD_THREAD 1

#include <jni.h>

class NativeCallJava {

public:
    _JavaVM *javaVM = NULL;
    JNIEnv *jniEnv = NULL;
    jobject jobj;
    jmethodID jmid_prepared;

public:
    NativeCallJava(_JavaVM *pVm, _JNIEnv *env, _jobject *pJobject);

    ~NativeCallJava();

    void onPrepared(int type);
};


#endif //STREAMING_NATIVECALLJAVA_H
