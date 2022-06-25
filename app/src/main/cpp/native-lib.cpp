#include <jni.h>
#include <string>
#include <android/log.h>
#include "NativeCallJava.h"
#include "FFMpegPlayer.h"
#include "PlayerStatus.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include <libavutil/time.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include "libswresample/swresample.h"
}

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, "ffmpegDebug", __VA_ARGS__)

static AVFormatContext *avFormatContext;
static AVCodecContext *avCodecContext;
AVCodec *vCodec;
ANativeWindow *nativeWindow;
ANativeWindow_Buffer windowBuffer;

static AVPacket *avPacket;
static AVFrame *avFrame, *rgbFrame;
struct SwsContext *swsContext;
uint8_t *outBuffer;

_JavaVM *javaVM = NULL;
NativeCallJava *callJava = NULL;
FFMpegPlayer *ffMpegPlayer = NULL;
PlayerStatus *playerStatus = NULL;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_shaowei_streaming_ffmpeg_FFMpegActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    const char *conf = avcodec_configuration();
    return env->NewStringUTF(conf);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_shaowei_streaming_ffmpeg_FFMpegActivity_playRemoteVideoWithFFMpeg1(JNIEnv *env, jobject thiz, jstring url_,
                                                                            jobject surface) {
    const char *url = env->GetStringUTFChars(url_, 0);
    // Deprecated after FFMpeg 4.0
//    av_register_all();

    // 打开地址并获取里面的内容
    // avFormatContext是内容的一个上下文
    AVFormatContext *avFormatContext = avformat_alloc_context();
    avformat_open_input(&avFormatContext, url, NULL, NULL);
    avformat_find_stream_info(avFormatContext, NULL);

    // Find vide index
    int video_index = -1;
    for (int i = 0; i < avFormatContext->nb_streams; ++i) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        }
    }

    /**
     * Set decode, transform, render
     */
    // Set up AVCodecContext
    AVCodecContext *avCodecContext = avFormatContext->streams[video_index]->codec;
    // get AVCodec
    AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);
    // Open codec
    if (avcodec_open2(avCodecContext, avCodec, nullptr) < 0) {
        // return if fail to open codec
        return;
    }

    /**
     * Apply for AVPacket and AVFrame
     *
     * AVPacket is to save the data before decoding and some additional information, such as display timestamp (pts),
     * decoding timestamp (dts), data duration and the index of the media stream where it is located.
     *
     * AVFrame is to store the decoded data
     */
    auto *avPacket = static_cast<AVPacket *>(av_malloc(sizeof(AVPacket)));
    av_init_packet(avPacket);

    /**
     * Allocate an AVFrame
     * AVFrame is generally used to store the original data, pointing to the decoded original frame
     */
    AVFrame *avFrame = av_frame_alloc();
    /**
     * Allocate an AVFrame, pointing to the rgb frame
     */
    AVFrame *rgb_frame = av_frame_alloc();
    /**
     * rgb_frame is a cache and needs to set buffer
     */
    auto *out_buffer = static_cast<uint8_t *>(av_malloc(
            avpicture_get_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height)));
    /**
     * Set up rgb_frame buffer
     */
    avpicture_fill((AVPicture *) rgb_frame, out_buffer, AV_PIX_FMT_RGBA, avCodecContext->width,
                   avCodecContext->height);
    /**
     * Set up ANativeWindow to render the frames
     */
    ANativeWindow *pANativeWindow = ANativeWindow_fromSurface(env, surface);
    if (pANativeWindow == 0) { // 获取native window失败直接返回
        return;
    }

    SwsContext *swsContext = sws_getContext(avCodecContext->width, avCodecContext->height,
                                            avCodecContext->pix_fmt, avCodecContext->width,
                                            avCodecContext->height, AV_PIX_FMT_RGBA, SWS_BICUBIC,
                                            NULL, NULL, NULL);
    // Video buffer
    ANativeWindow_Buffer nativeWindow_outBuffer;

    /**
     * Start decoder
     */
    int frameCount;
    while (av_read_frame(avFormatContext, avPacket) >= 0) {
        if (avPacket->stream_index == video_index) {
            avcodec_decode_video2(avCodecContext, avFrame, &frameCount, avPacket);
            if (frameCount) {
                ANativeWindow_setBuffersGeometry(pANativeWindow, avCodecContext->width,
                                                 avCodecContext->height, WINDOW_FORMAT_RGBA_8888);
                /**
                 * Lock native window
                 */
                ANativeWindow_lock(pANativeWindow, &nativeWindow_outBuffer, NULL);
                /**
                 * Transform to RGB
                 */
                sws_scale(swsContext, (const uint8_t *const *) avFrame->data, avFrame->linesize, 0,
                          avFrame->height, rgb_frame->data, rgb_frame->linesize);
                uint8_t *dst = static_cast<uint8_t *>(nativeWindow_outBuffer.bits);
                int destStride = nativeWindow_outBuffer.stride * 4;
                uint8_t *src = rgb_frame->data[0];
                int srcStride = rgb_frame->linesize[0];
                for (int i = 0; i < avCodecContext->height; i++) {
                    memcpy(dst + i * destStride, src + i * srcStride, srcStride);
                }
                ANativeWindow_unlockAndPost(pANativeWindow);
            }
        }
        av_free_packet(avPacket);
    }

    ANativeWindow_release(pANativeWindow);
    av_frame_free(&avFrame);
    av_frame_free(&rgb_frame);
    avcodec_close(avCodecContext);
    avformat_free_context(avFormatContext);

    env->ReleaseStringUTFChars(url_, url);

}

extern "C"
JNIEXPORT int JNICALL
Java_com_shaowei_streaming_ffmpeg_FFMpegActivity_playLocalVideoWithFFMpeg2(JNIEnv *env, jobject thiz, jstring url_,
                                                                           jobject surface) {
    const char *url = env->GetStringUTFChars(url_, 0);
    // Register all for FFMpeg, unnecessary now
    // avcodec_register_all();

    // alloc avFormatContext
    avFormatContext = avformat_alloc_context();

    // Open file
    if (avformat_open_input(&avFormatContext, url, NULL, NULL) != 0) {
        return -1;
    }

    if (avformat_find_stream_info(avFormatContext, NULL) < 0) {
        return -1;
    }

    // Find video index
    int videoIndex = -1;
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }

    if (videoIndex == -1) {
        return -1;
    }

    //video/avc
    // Set up avCodecContext
    avCodecContext = avFormatContext->streams[videoIndex]->codec;
    vCodec = avcodec_find_decoder(avCodecContext->codec_id);

    //Open codec
    if (avcodec_open2(avCodecContext, vCodec, NULL) < 0) {
        return -1;
    }

    // Set up ANativeWindow
    nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (nullptr == nativeWindow) {
        // Couldn't get native window from surface
        return -1;
    }

    // Setup AVFrame YUV
    avFrame = av_frame_alloc();
    // Setup AVPacket
    avPacket = av_packet_alloc();
    // Setup AVFrame rgb
    rgbFrame = av_frame_alloc();

    int width = avCodecContext->width;
    int height = avCodecContext->height;
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, width, height, 1);
    // Setup output buffer
    outBuffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    // Set output buffer to rgbFrame
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, outBuffer, AV_PIX_FMT_RGBA, width,
                         height, 1);

    swsContext = sws_getContext(width, height, avCodecContext->pix_fmt,
                                width, height, AV_PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL, NULL);

    if (0 > ANativeWindow_setBuffersGeometry(nativeWindow, width, height, WINDOW_FORMAT_RGBA_8888)) {
        // Couldn't set buffers geometry
        ANativeWindow_release(nativeWindow);
        return -1;
    }

    while (av_read_frame(avFormatContext, avPacket) >= 0) {
        if (avPacket->stream_index == videoIndex) {
            int ret = avcodec_send_packet(avCodecContext, avPacket);
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                // Decode error
                return -1;
            }

            ret = avcodec_receive_frame(avCodecContext, avFrame);
            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0) {
                break;
            }

            sws_scale(swsContext, avFrame->data, avFrame->linesize, 0, avCodecContext->height,
                      rgbFrame->data, rgbFrame->linesize);

            if (ANativeWindow_lock(nativeWindow, &windowBuffer, NULL) < 0) {
                // cannot lock window
            } else {
                // render to the screen
                uint8_t *dst = (uint8_t *) windowBuffer.bits;
                for (int h = 0; h < height; h++) {
                    memcpy(dst + h * windowBuffer.stride * 4,
                           outBuffer + h * rgbFrame->linesize[0],
                           rgbFrame->linesize[0]);
                }
                switch (avFrame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        // I frame
                        break;
                    case AV_PICTURE_TYPE_P:
                        // P frame
                        break;
                    case AV_PICTURE_TYPE_B:
                        // B frame
                        break;
                    default:;
                        break;
                }
            }

            // av_usleep(1000 * 33);
            ANativeWindow_unlockAndPost(nativeWindow);

//avcodec_send_packet()
// avcodec_receive_frame();
        }
    }

    // release sources
    avformat_free_context(avFormatContext);
    env->ReleaseStringUTFChars(url_, url);
    return -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_shaowei_streaming_ffmpeg_FFMpegActivity_playAudioWithFFMpeg(JNIEnv *env, jobject thiz, jstring path) {
    const char *input = env->GetStringUTFChars(path, 0);

    AVFormatContext *pFormatCtx = avformat_alloc_context();
    if (avformat_open_input(&pFormatCtx, input, NULL, NULL) != 0) {
        LOGE("%s", "fail to open input file");
        return;
    }

    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("%s", "fail to find stream info");
        return;
    }

    // Find audio stream index
    int audio_stream_index = -1;
    for (int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            LOGE("Found audio index: %d", pFormatCtx->streams[i]->codec->codec_type);
            audio_stream_index = i;
            break;
        }
    }

    AVCodecContext *pCodecCtx = pFormatCtx->streams[audio_stream_index]->codec;
    AVCodec *pCodex = avcodec_find_decoder(pCodecCtx->codec_id);
    if (avcodec_open2(pCodecCtx, pCodex, NULL) < 0) {
        return;
    }

    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));
    AVFrame *frame = av_frame_alloc();
    int out_channel_number = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);

    SwrContext *swrContext = swr_alloc();
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_format = AV_SAMPLE_FMT_S16;
    int out_sample_rate = pCodecCtx->sample_rate;

    swr_alloc_set_opts(swrContext, out_ch_layout, out_format, out_sample_rate,
                       pCodecCtx->channel_layout, pCodecCtx->sample_fmt,
                       pCodecCtx->sample_rate, 0, NULL);

    swr_init(swrContext);
    uint8_t *out_buffer = (uint8_t *) av_malloc(44100 * 2);

    // Call java functions by reflection
    jclass ffmpegActivity = env->GetObjectClass(thiz);
    jmethodID createTrack = env->GetMethodID(ffmpegActivity, "createTrack", "(II)V");
    env->CallVoidMethod(thiz, createTrack, 44100, out_channel_number);

    jmethodID playTrack = env->GetMethodID(ffmpegActivity, "playTrack", "([BI)V");

    // decode audio
    int got_frame;
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        if (packet->stream_index == audio_stream_index) {
            avcodec_decode_audio4(pCodecCtx, frame, &got_frame, packet);
            if (got_frame >= 0) {
                swr_convert(swrContext, &out_buffer, 44100 * 2,
                            (const uint8_t **) (frame->data), frame->nb_samples);
                int size = av_samples_get_buffer_size(NULL, out_channel_number, frame->nb_samples,
                                                      AV_SAMPLE_FMT_S16, 1);
                jbyteArray audio_sample_array = env->NewByteArray(size);
                env->SetByteArrayRegion(audio_sample_array, 0, size,
                                        reinterpret_cast<const jbyte *>(out_buffer));
                env->CallVoidMethod(thiz, playTrack, audio_sample_array, size);
                env->DeleteLocalRef(audio_sample_array);
            }
        }
    }

    // release resources
    av_frame_free(&frame);
    av_free(packet);
    swr_free(&swrContext);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);
    env->ReleaseStringUTFChars(path, input);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_shaowei_streaming_ffmpeg_FFMpegPlayer_nativePrepare(JNIEnv *env, jobject instance, jstring m_uri) {
    const char *source = env->GetStringUTFChars(m_uri, 0);

    if (ffMpegPlayer == NULL) {
        if (callJava == NULL) {
            callJava = new NativeCallJava(javaVM, env, instance);
        }
        playerStatus = new PlayerStatus();
        ffMpegPlayer = new FFMpegPlayer(playerStatus, callJava, source);
        ffMpegPlayer->prepare();
    }

}
extern "C"
JNIEXPORT void JNICALL
Java_com_shaowei_streaming_ffmpeg_FFMpegPlayer_nativeStart(JNIEnv *env, jobject thiz) {

}