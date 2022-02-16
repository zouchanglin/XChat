package cn.tim.xchat.chat.ws;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.tim.xchat.chat.BuildConfig;
import cn.tim.xchat.chat.core.Constants;

public class WebSocketManager {
    private static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 3000;
    private static final int DEFAULT_SOCKET_RECONNECT_INTERVAL = 3000;
    private static final int FRAME_QUEUE_SIZE = 5;
    private final String token;

    WebSocketListener mWebSocketListener;
    WebSocketFactory mWebSocketFactory;
    WebSocket mWebSocket;

    private ConnectStatus mConnectStatus = ConnectStatus.CONNECT_DISCONNECT;
    private final Timer mReconnectTimer = new Timer();
    private TimerTask mReconnectTimerTask;

    private String mUri;

    public interface WebSocketListener {
        void onConnected(Map<String, List<String>> headers);
        void onTextMessage(String text);
        void onByteMessage(byte[] retByte);
    }

    public enum ConnectStatus {
        CONNECT_DISCONNECT, // 断开连接
        CONNECT_SUCCESS, //连接成功
        CONNECT_FAIL, //连接失败
        CONNECTING; //正在连接
    }

    public WebSocketManager(String token) {
        this(token, DEFAULT_SOCKET_CONNECT_TIMEOUT);
    }

    // ws://192.168.31.156:8088/ws;2189378973891321
    public WebSocketManager(String token, int timeout) {
        mUri = BuildConfig.WEBSOCKET_URL;
        this.token = token;
        mWebSocketFactory = new WebSocketFactory().setConnectionTimeout(timeout);
    }

    public void setWebSocketListener(WebSocketListener webSocketListener) {
        mWebSocketListener = webSocketListener;
    }

    public void connect() {
        try {
            mWebSocket = mWebSocketFactory.createSocket(mUri)
                    .addHeader("token", token)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(new NVWebSocketListener())
                    .connectAsynchronously();  // 这里我改成了同步调用 异步调用请使用connectAsynchronously()
            setConnectStatus(ConnectStatus.CONNECTING);
        } catch (IOException e) {
            e.printStackTrace();
            reconnect();
        }
    }

    // 客户端像服务器发送消息
    public void sendMessage(byte[] array) {
        if(mWebSocket != null) {
            mWebSocket.sendBinary(array);
        }
    }

    private void setConnectStatus(ConnectStatus connectStatus) {
        if(mWebSocket != null) {
            mConnectStatus = connectStatus;
        }
    }

    public ConnectStatus getConnectStatus() {
        return mConnectStatus;
    }

    public void disconnect() {
        if (mWebSocket != null) {
            mWebSocket.disconnect();
        }
        setConnectStatus(null);
    }

    public void reconnect() {
        if (mWebSocket != null && !mWebSocket.isOpen() && getConnectStatus() != ConnectStatus.CONNECTING) {
            mReconnectTimerTask = new TimerTask() {
                @Override
                public void run() {
                    connect();
                }
            };
            mReconnectTimer.schedule(mReconnectTimerTask, DEFAULT_SOCKET_RECONNECT_INTERVAL);
        }
    }


    /**
     *  Adapter的回调中主要做三件事
     *  1、设置连接状态
     *  2、回调 WebSocketListener
     *  3、连接失败重连
     */
    class NVWebSocketListener extends WebSocketAdapter {

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            System.out.println("OS. WebSocket onConnected");
            setConnectStatus(WebSocketManager.ConnectStatus.CONNECT_SUCCESS);
            if (mWebSocketListener != null) {
                mWebSocketListener.onConnected(headers);
            }
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            Log.e(Constants.TAG, "OS. WebSocket onConnectError");
            setConnectStatus(WebSocketManager.ConnectStatus.CONNECT_FAIL);
            exception.printStackTrace(System.err);
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                   WebSocketFrame clientCloseFrame, boolean closedByServer)
                throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            System.out.println("OS. WebSocket onDisconnected");
            setConnectStatus(WebSocketManager.ConnectStatus.CONNECT_DISCONNECT);
            reconnect();
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            if (mWebSocketListener != null) {
                mWebSocketListener.onTextMessage(text);
            }
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            super.onBinaryMessage(websocket, binary);
            if (mWebSocketListener != null) {
                mWebSocketListener.onByteMessage(binary);
            }
        }
    }
}