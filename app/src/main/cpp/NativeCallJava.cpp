
#include <syslog.h>
#include "NativeCallJava.h"
#include <android/log.h>

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, "ffmpegDebug", __VA_ARGS__)

NativeCallJava::NativeCallJava(_JavaVM *pVm, _JNIEnv *env, jobject pJobject) {
    this->javaVM = pVm;
    this->jniEnv = env;
    this->jobj = pJobject;
    this->jobj = env->NewGlobalRef(jobj);

    jclass jlz = jniEnv->GetObjectClass(jobj);
    if (!jlz) {
        if (LOG_DEBUG) {
            LOGE("get jclass wrong");
        }
        return;
    }

    jmid_prepared = env->GetMethodID(jlz, "onPrepared", "()V");
}

void NativeCallJava::onPrepared(int type) {
    if(type == MAIN_THREAD)
    {
        jniEnv->CallVoidMethod(jobj, jmid_prepared);
    }
    else if(type == CHILD_THREAD)
    {
        JNIEnv *jniEnv;
        if(javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK)
        {
            if(LOG_DEBUG)
            {
                LOGE("get child thread jnienv worng");
            }
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_prepared);
        javaVM->DetachCurrentThread();
    }

}
