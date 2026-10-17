package com.wuhen.shi;

import android.app.Application;
import android.util.Log;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ShiwuhenApp extends Application {

    private static ShiwuhenApp instance;

    public static ShiwuhenApp get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        new Thread(() -> {
            try {
                YoutubeDL.getInstance().init(this);
                FFmpeg.getInstance().init(this);
                ensureYtdlpBinary();
                Diag.log("runtime init ok");
            } catch (Throwable t) {
                Log.e("Shiwuhen", "init failed", t);
                Diag.log("runtime init FAILED: " + t.getMessage());
            }
        }, "shi-runtime-init").start();
    }

    private void ensureYtdlpBinary() {
        File dir = new File(getFilesDir(), "youtubedl");
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, "yt-dlp");
        if (dest.exists() && dest.length() > 1000) return;
        try (InputStream in = getResources().openRawResource(R.raw.yt_dlp);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            dest.setExecutable(true);
            Diag.log("yt-dlp binary extracted: " + dest.length() + " bytes");
        } catch (IOException e) {
            Diag.log("yt-dlp extract failed: " + e.getMessage());
        }
    }
}
