
#ifndef STREAMING_NATIVECALLJAVA_H
#define STREAMING_NATIVECALLJAVA_H


#include <jni.h>

class NativeCallJava {

public:
    _JavaVM *javaVm = NULL;
    JNIEnv *jniEnv = NULL;
    jobject jobj;
    jmethodID jmid_prepared;

public:
    NativeCallJava(_JavaVM *pVm, _JNIEnv *pEnv, _jobject *pJobject);
};


#endif //STREAMING_NATIVECALLJAVA_H
