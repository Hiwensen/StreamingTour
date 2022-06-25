
#include "FFMpegPlayer.h"

FFMpegPlayer::FFMpegPlayer(PlayerStatus *pStatus, NativeCallJava *pJava, const char *source) {
    this->playstatus = pStatus;
    this->callJava = pJava;
    this->url = source;
}

void *decodeWithFFmpeg(void *data)
{
    FFMpegPlayer *wlFFmpeg = (FFMpegPlayer *) data;
    wlFFmpeg->decodeDataWithNewThread();
    pthread_exit(&wlFFmpeg->decodeThread);
}

void FFMpegPlayer::prepare() {
    pthread_create(&decodeThread, NULL, decodeWithFFmpeg, this);
}

void FFMpegPlayer::decodeDataWithNewThread() {

}

void FFMpegPlayer::startPlay() {

}
