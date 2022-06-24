
#include <pthread.h>
#include <syslog.h>
#include "NativeQueue.h"
#include <android/log.h>

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, "ffmpegDebug", __VA_ARGS__)


NativeQueue::NativeQueue(PlayerStatus *playstatus) {
    this->playstatus = playstatus;
    pthread_mutex_init(&mutexPacket, NULL);
    pthread_cond_init(&condPacket, NULL);
}

NativeQueue::~NativeQueue() {

}

int NativeQueue::putAvpacket(AVPacket *packet) {

    pthread_mutex_lock(&mutexPacket);

    queuePacket.push(packet);
    if(LOG_DEBUG)
    {
        LOGD("放入一个AVpacket到队里里面， 个数为：%d", queuePacket.size());
    }
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);

    return 0;
}

int NativeQueue::getAvpacket(AVPacket *packet) {

    pthread_mutex_lock(&mutexPacket);

    while(playstatus != NULL && !playstatus->exit)
    {
        if(queuePacket.size() > 0)
        {
            AVPacket *avPacket =  queuePacket.front();
            if(av_packet_ref(packet, avPacket) == 0)
            {
                queuePacket.pop();
            }
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            if(LOG_DEBUG)
            {
                LOGD("从队列里面取出一个AVpacket，还剩下 %d 个", queuePacket.size());
            }
            break;
        } else{
            pthread_cond_wait(&condPacket, &mutexPacket);
        }
    }
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

int NativeQueue::getQueueSize() {

    int size = 0;
    pthread_mutex_lock(&mutexPacket);
    size = queuePacket.size();
    pthread_mutex_unlock(&mutexPacket);

    return size;
}