package cn.tim.xchat.common.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import cn.tim.xchat.common.R;

public class WebSocketService extends Service {
    public static final String TAG = "WebSocketService";

    private final static String CHANNEL_ID = "100500";
    private final static String CHANNEL_NAME = "XChat";
    private final static int GRAY_SERVICE_ID = 1001;
    public final static int NOTIFY_ID = 10080;
    private final WebSocketClientBinder mBinder = new WebSocketClientBinder();
    private final WebSocketHelper webSocketHelper;
    private Notification notification;

    public WebSocketService() {
        webSocketHelper = WebSocketHelper.getInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //灰色保活
    public static class GrayInnerService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
//            return Service.START_CONTINUATION_MASK;
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    // 用于Activity和service通讯
    public class WebSocketClientBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }

        public void cancel() {
            webSocketHelper.cancelWebSocket();
        }

        public void startForeground() {
            WebSocketService.this.startForeground(NOTIFY_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        //初始化WebSocket
        webSocketHelper.launchWebSocket();
        //mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
        //设置service为前台服务，提高优先级
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

        Class<?> chatActivityClazz = null;
        try {
            chatActivityClazz = Class.forName("cn.tim.xchat.chat.ChatActivity");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, chatActivityClazz), 0);
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("This is content title.")
                .setContentText("This is content text.")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_baseline_chat_24)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_chat_144))
                .setContentIntent(pi)
                .build();

        manager.createNotificationChannel(notificationChannel);
        manager.notify(NOTIFY_ID, notification);
        return START_STICKY;
    }
}