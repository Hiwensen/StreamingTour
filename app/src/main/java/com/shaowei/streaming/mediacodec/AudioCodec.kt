package com.shaowei.streaming.mediacodec

class AudioCodec {
    private val TAG = AudioCodec::class.java.simpleName

    fun audioToPCM(sourceAudioPath: String, desAudioPath: String, audioDecodeListener: AudioDecodeListener) {

    }

    fun pcmToACC() {

    }

    interface AudioDecodeListener {
        fun onSuccess()
        fun onError()
    }
}