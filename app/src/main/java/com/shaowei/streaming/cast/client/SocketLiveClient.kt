package com.shaowei.streaming.cast.client

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

class SocketLiveClient(val socketCallback: SocketCallbackClient, val port: Int) {
    private val TAG = SocketLiveClient::class.java.simpleName
    private val serverURI = URI("ws://192.168.31.57:12001")
    private lateinit var mWebSocketClient: WebSocketClient

    fun start() {
        try {
            mWebSocketClient = CastWebSocketClient(serverURI)
            mWebSocketClient.connect()
        } catch (e: Exception) {
            Log.e(TAG, "startSocket:$e")
        }
    }

    fun close() {
        try {
            mWebSocketClient.close()
        } catch (e: Exception) {
            Log.d(TAG,"close socket:$e")
        }
    }


    inner class CastWebSocketClient(private val serverURI: URI) : WebSocketClient(serverURI) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.d(TAG, "onWebSocketOpen")
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            Log.d(TAG, "onWebSocketClose")
        }

        override fun onMessage(message: String?) {
            Log.d(TAG, "onWebSocketMessage:$message")
        }

        override fun onMessage(bytes: ByteBuffer) {
            Log.d(TAG, "onWebSocketMessage length:${bytes.remaining()}")
            val buffer = ByteArray(bytes.remaining())
            bytes.get(buffer)
            socketCallback.onMessage(buffer)
        }

        override fun onError(ex: Exception?) {
            Log.d(TAG, "onWebSocketError")
        }

    }

    interface SocketCallbackClient {
        fun onMessage(data: ByteArray)
    }
}