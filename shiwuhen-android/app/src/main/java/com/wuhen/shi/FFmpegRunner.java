package com.wuhen.shi;

import android.content.Context;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;

public final class FFmpegRunner {

    private FFmpegRunner() {}

    public static int gifFromVideo(Context ctx, File video, File outGif, String fps, String width) {
        YoutubeDLRequest req = new YoutubeDLRequest(java.util.Arrays.asList(
                "ffmpeg", "-y",
                "-i", video.getAbsolutePath(),
                "-vf", "fps=" + fps + ",scale=" + width + ":-1:flags=lanczos",
                "-loop", "0",
                outGif.getAbsolutePath()));
        return run(req, "gif");
    }

    public static int gifToMp4(Context ctx, File gif, File outMp4) {
        YoutubeDLRequest req = new YoutubeDLRequest(java.util.Arrays.asList(
                "ffmpeg", "-y",
                "-i", gif.getAbsolutePath(),
                "-movflags", "faststart",
                "-pix_fmt", "yuv420p",
                outMp4.getAbsolutePath()));
        return run(req, "gif2mp4");
    }

    public static int extractFrame(Context ctx, File video, File outPng, String timestamp) {
        YoutubeDLRequest req = new YoutubeDLRequest(java.util.Arrays.asList(
                "ffmpeg", "-y",
                "-ss", timestamp,
                "-i", video.getAbsolutePath(),
                "-frames:v", "1",
                outPng.getAbsolutePath()));
        return run(req, "frame");
    }

    private static int run(YoutubeDLRequest req, String taskId) {
        try {
            Diag.log("ffmpeg: " + req.buildCommand());
            return FFmpeg.getInstance().execute(req, taskId, null).getExitCode();
        } catch (Exception e) {
            Diag.log("ffmpeg error: " + e.getMessage());
            return -1;
        }
    }
}
