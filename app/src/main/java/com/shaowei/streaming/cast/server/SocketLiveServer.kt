package com.shaowei.streaming.cast.server

import android.media.projection.MediaProjection
import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class SocketLiveServer(private var port: Int) {
    private val TAG = SocketLiveServer::class.java.simpleName
    private val webSocketServer = ScreenWebSocketServer()
    private lateinit var webSocket: WebSocket
    private lateinit var codecLiveH265: CodecLiveH265

    fun start(mediaProjection: MediaProjection) {
        webSocketServer.start()
        codecLiveH265 = CodecLiveH265(this, mediaProjection)
        codecLiveH265.startLive()
    }

    fun sendData(data: ByteArray) {
        if (webSocket.isOpen) {
            webSocket.send(data)
        }
    }

    fun close() {
        try {
            webSocket.close()
            webSocketServer.stop()
        } catch (exception: java.lang.Exception) {
            Log.d(TAG, "close socket exception:$exception")
        }
    }

    inner class ScreenWebSocketServer : WebSocketServer(InetSocketAddress(port)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            Log.d(TAG, "WebSocket onOpen")
            webSocket = conn
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            Log.d(TAG, "WebSocket onClose")
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            Log.d(TAG, "WebSocket onMessage")
        }

        override fun onStart() {
            Log.d(TAG, "WebSocket onStart")
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            Log.d(TAG, "WebSocket onError:$ex")
        }

    }
}