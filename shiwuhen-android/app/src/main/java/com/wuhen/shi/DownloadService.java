package com.wuhen.shi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class DownloadService extends Service {

    private static final String CHANNEL_ID = "shi_downloads";
    private static final int NOTIF_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "拾光无痕下载", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("拾光无痕")
                .setContentText("正在处理任务…")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, n);
        Diag.log("DownloadService started (foreground)");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Diag.log("DownloadService stopped");
        super.onDestroy();
    }
}
