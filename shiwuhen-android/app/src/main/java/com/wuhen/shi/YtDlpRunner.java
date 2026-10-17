package com.wuhen.shi;

import android.content.Context;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class YtDlpRunner {

    private YtDlpRunner() {}

    public static YoutubeDLRequest build(Context ctx, String url, String outputTemplate, boolean audioOnly) {
        File dir = new File(ctx.getFilesDir(), "youtubedl");
        File ytdlp = new File(dir, "yt-dlp");
        File downloadDir = new File(ctx.getExternalFilesDir(null), "downloads");
        if (!downloadDir.exists()) downloadDir.mkdirs();

        List<String> cmd = new ArrayList<>();
        cmd.add(ytdlp.getAbsolutePath());
        cmd.add(url);
        cmd.add("-o");
        cmd.add(new File(downloadDir, outputTemplate).getAbsolutePath());
        cmd.add("--no-cache-dir");
        cmd.add("--no-warnings");
        cmd.add("--newline");
        if (audioOnly) {
            cmd.add("-x");
            cmd.add("--audio-format");
            cmd.add("mp3");
        }
        return new YoutubeDLRequest(cmd);
    }

    public static YoutubeDLResponse exec(YoutubeDLRequest request, String taskId) throws Exception {
        Diag.log("exec: " + request.buildCommand());
        YoutubeDLResponse resp = YoutubeDL.getInstance().execute(request, taskId, null);
        Diag.log("exec done, exit=" + resp.getExitCode());
        return resp;
    }
}
