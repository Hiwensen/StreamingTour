
#include "NativeCallJava.h"

NativeCallJava::NativeCallJava(_JavaVM *pVm, _JNIEnv *pEnv, jobject pJobject) {
    this->javaVm = pVm;
    this->jniEnv = pEnv;
    this->jobj = pJobject;
    this->jobj = pEnv->NewGlobalRef(jobj);

}
