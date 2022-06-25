
#ifndef STREAMING_FFMPEGPLAYER_H
#define STREAMING_FFMPEGPLAYER_H


#include <cstdio>
#include <libavformat/avformat.h>
#include "PlayerStatus.h"
#include "NativeCallJava.h"
#include "OpenslAudio.h"
#include "pthread.h"

extern "C" {
#include "libavformat/avformat.h"
}

class FFMpegPlayer {

public:
    NativeCallJava *callJava = NULL;
    const char *url = NULL;
    pthread_t decodeThread;
    AVFormatContext *pFormatCtx = NULL;
    OpenslAudio *audio = NULL;
    PlayerStatus *playstatus = NULL;

public:
    FFMpegPlayer(PlayerStatus *pStatus, NativeCallJava *pJava, const char *string);

    ~FFMpegPlayer();

    void prepare();

    void decodeDataWithNewThread();

    void startPlay();

};


#endif //STREAMING_FFMPEGPLAYER_H
