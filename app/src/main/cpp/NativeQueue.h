
#ifndef STREAMING_NATIVEQUEUE_H
#define STREAMING_NATIVEQUEUE_H

#include "queue"
#include <libavcodec/packet.h>
#include <cstdio>
#include "AndroidLog.h"
#include "PlayerStatus.h"

extern "C" {
#include "libavcodec/avcodec.h"
}

class NativeQueue {

public:
    std::queue<AVPacket *> queuePacket;
    pthread_mutex_t mutexPacket;
    pthread_cond_t condPacket;
    PlayerStatus *playstatus = NULL;

public:
    NativeQueue(PlayerStatus *pStatus);

    ~NativeQueue();

    int putAvpacket(AVPacket *packet);

    int getAvpacket(AVPacket *packet);

    int getQueueSize();

};


#endif //STREAMING_NATIVEQUEUE_H
