
#include <syslog.h>
#include "FFMpegPlayer.h"
#include <android/log.h>

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, "ffmpegDebug", __VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, "ffmpegDebug", __VA_ARGS__)

FFMpegPlayer::FFMpegPlayer(PlayerStatus *pStatus, NativeCallJava *pJava, const char *source) {
    this->playstatus = pStatus;
    this->callJava = pJava;
    this->url = source;
}

void *decodeWithFFmpeg(void *data) {
    FFMpegPlayer *wlFFmpeg = (FFMpegPlayer *) data;
    wlFFmpeg->decodeDataWithNewThread();
    pthread_exit(&wlFFmpeg->decodeThread);
}

void FFMpegPlayer::prepare() {
    pthread_create(&decodeThread, NULL, decodeWithFFmpeg, this);
}

void FFMpegPlayer::decodeDataWithNewThread() {
//    av_register_all();
    avformat_network_init();
    pFormatCtx = avformat_alloc_context();
    if (avformat_open_input(&pFormatCtx, url, NULL, NULL) != 0) {
        if (LOG_DEBUG) {
            LOGE("can not open url :%s", url);
        }
        return;
    }
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        if (LOG_DEBUG) {
            LOGE("can not find streams from %s", url);
        }
        return;
    }
    for (int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (audio == NULL) {
                audio = new OpenslAudio(playstatus, pFormatCtx->streams[i]->codecpar->sample_rate);
                audio->streamIndex = i;
                audio->codecpar = pFormatCtx->streams[i]->codecpar;
            }
        }
    }

    AVCodec *dec = avcodec_find_decoder(audio->codecpar->codec_id);
    if (!dec) {
        if (LOG_DEBUG) {
            LOGE("can not find decoder");
        }
        return;
    }

    audio->avCodecContext = avcodec_alloc_context3(dec);
    if (!audio->avCodecContext) {
        if (LOG_DEBUG) {
            LOGE("can not alloc new decodecctx");
        }
        return;
    }

    if (avcodec_parameters_to_context(audio->avCodecContext, audio->codecpar) < 0) {
        if (LOG_DEBUG) {
            LOGE("can not fill decodecctx");
        }
        return;
    }

    if (avcodec_open2(audio->avCodecContext, dec, 0) != 0) {
        if (LOG_DEBUG) {
            LOGE("cant not open audio strames");
        }
        return;
    }
    callJava->onPrepared(CHILD_THREAD);
}

void FFMpegPlayer::startPlay() {
    if (audio == NULL) {
        if (LOG_DEBUG) {
            LOGE("audio is null");
            return;
        }
    }
    audio->play();

    int count = 0;

    while (playstatus != NULL && !playstatus->exit) {
        AVPacket *avPacket = av_packet_alloc();
        if (av_read_frame(pFormatCtx, avPacket) == 0) {
            if (avPacket->stream_index == audio->streamIndex) {
                //解码操作
                count++;
                if (LOG_DEBUG) {
                    LOGE("解码第 %d 帧", count);
                }
                audio->queue->putAvpacket(avPacket);
            } else {
                av_packet_free(&avPacket);
                av_free(avPacket);
            }
        } else {
            av_packet_free(&avPacket);
            av_free(avPacket);
            while (playstatus != NULL && !playstatus->exit) {
                if (audio->queue->getQueueSize() > 0) {
                    continue;
                } else {
                    playstatus->exit = true;
                    break;
                }
            }
        }
    }

    if (LOG_DEBUG) {
        LOGD("解码完成");
    }

}
