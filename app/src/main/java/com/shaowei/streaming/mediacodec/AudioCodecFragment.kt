package com.shaowei.streaming.mediacodec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.shaowei.streaming.R

class AudioCodecFragment : Fragment() {
    private lateinit var mRecordAudio: Button
    private lateinit var mPlayAudio: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(
            R.layout.fragment_audio_codec, container
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRecordAudio = view.findViewById(R.id.record_audio)
        mRecordAudio.setOnClickListener{

        }

        mPlayAudio = view.findViewById(R.id.play_audio)
        mPlayAudio.setOnClickListener {

        }
    }
}