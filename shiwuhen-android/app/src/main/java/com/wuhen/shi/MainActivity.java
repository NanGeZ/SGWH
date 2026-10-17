package com.wuhen.shi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements Diag.Listener {

    private EditText editUrl;
    private TextView textStatus;
    private TextView textInfo;
    private TextView textLog;
    private ScrollView scrollLog;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private String lastDownloadPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editUrl = findViewById(R.id.editUrl);
        textStatus = findViewById(R.id.textStatus);
        textInfo = findViewById(R.id.textInfo);
        textLog = findViewById(R.id.textLog);
        scrollLog = findViewById(R.id.scrollLog);

        findViewById(R.id.btnParse).setOnClickListener(v -> doParse());
        findViewById(R.id.btnDownloadVideo).setOnClickListener(v -> doDownload(false));
        findViewById(R.id.btnDownloadAudio).setOnClickListener(v -> doDownload(true));
        findViewById(R.id.btnGif).setOnClickListener(v -> doGif());
        findViewById(R.id.btnGifToMp4).setOnClickListener(v -> doGifToMp4());
        findViewById(R.id.btnExtractFrame).setOnClickListener(v -> doExtractFrame());
        findViewById(R.id.btnShareLog).setOnClickListener(v -> shareLog());
        findViewById(R.id.btnClearLog).setOnClickListener(v -> Diag.clear());

        Diag.addListener(this);

        String shared = handleIntent(getIntent());
        if (!TextUtils.isEmpty(shared)) editUrl.setText(shared);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String shared = handleIntent(intent);
        if (!TextUtils.isEmpty(shared)) editUrl.setText(shared);
    }

    private String handleIntent(Intent intent) {
        if (intent == null) return null;
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            return intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri u = intent.getData();
            return u != null ? u.toString() : null;
        }
        return null;
    }

    private String inputUrl() {
        String u = editUrl.getText().toString().trim();
        if (TextUtils.isEmpty(u)) {
            textStatus.setText("请先粘贴链接");
            return null;
        }
        return u;
    }

    private void doParse() {
        String url = inputUrl();
        if (url == null) return;
        textStatus.setText(R.string.status_parsing);
        io.submit(() -> {
            try {
                YoutubeDLRequest req = YtDlpRunner.build(this, url, "probe.%(ext)s", false);
                List<String> cmd = req.buildCommand();
                cmd.add("--dump-single-json");
                YoutubeDLRequest probe = new YoutubeDLRequest(cmd);
                YoutubeDLResponse resp = YoutubeDL.getInstance().execute(probe, "probe", null);
                String out = resp.getOut();
                String title = extractJsonField(out, "title");
                String uploader = extractJsonField(out, "uploader");
                String duration = extractJsonField(out, "duration");
                runOnUiThread(() -> {
                    textStatus.setText(R.string.status_done);
                    textInfo.setText("标题: " + title + "\n作者: " + uploader + "\n时长: " + duration + "s");
                });
                Diag.log("probe ok: " + title);
            } catch (Exception e) {
                Diag.log("probe failed: " + e.getMessage());
                runOnUiThread(() -> {
                    textStatus.setText(R.string.status_error);
                    textInfo.setText(String.valueOf(e.getMessage()));
                });
            }
        });
    }

    private void doDownload(boolean audioOnly) {
        String url = inputUrl();
        if (url == null) return;
        startService(new Intent(this, DownloadService.class));
        textStatus.setText(R.string.status_downloading);
        String template = audioOnly ? "audio-%(title).80B.%(ext)s" : "video-%(title).80B.%(ext)s";
        io.submit(() -> {
            try {
                YoutubeDLRequest req = YtDlpRunner.build(this, url, template, audioOnly);
                YoutubeDLResponse resp = YtDlpRunner.exec(req, "dl-" + System.currentTimeMillis());
                File dir = new File(getExternalFilesDir(null), "downloads");
                File newest = newestFile(dir);
                lastDownloadPath = newest != null ? newest.getAbsolutePath() : null;
                runOnUiThread(() -> {
                    textStatus.setText(R.string.status_done);
                    textInfo.setText("已保存: " + lastDownloadPath);
                });
            } catch (Exception e) {
                Diag.log("download failed: " + e.getMessage());
                runOnUiThread(() -> {
                    textStatus.setText(R.string.status_error);
                    textInfo.setText(String.valueOf(e.getMessage()));
                });
            } finally {
                stopService(new Intent(this, DownloadService.class));
            }
        });
    }

    private void doGif() {
        File src = pickSourceFile();
        if (src == null) return;
        textStatus.setText(R.string.status_converting);
        io.submit(() -> {
            File out = new File(src.getParentFile(), baseName(src) + ".gif");
            int code = FFmpegRunner.gifFromVideo(this, src, out, "12", "480");
            runOnUiThread(() -> {
                textStatus.setText(code == 0 ? R.string.status_done : R.string.status_error);
                textInfo.setText(code == 0 ? "GIF: " + out.getAbsolutePath() : "GIF 转换失败 code=" + code);
            });
        });
    }

    private void doGifToMp4() {
        File src = pickSourceFile();
        if (src == null) return;
        textStatus.setText(R.string.status_converting);
        io.submit(() -> {
            File out = new File(src.getParentFile(), baseName(src) + ".mp4");
            int code = FFmpegRunner.gifToMp4(this, src, out);
            runOnUiThread(() -> {
                textStatus.setText(code == 0 ? R.string.status_done : R.string.status_error);
                textInfo.setText(code == 0 ? "MP4: " + out.getAbsolutePath() : "GIF 转 MP4 失败 code=" + code);
            });
        });
    }

    private void doExtractFrame() {
        File src = pickSourceFile();
        if (src == null) return;
        textStatus.setText(R.string.status_converting);
        io.submit(() -> {
            File out = new File(src.getParentFile(), baseName(src) + "-frame.png");
            int code = FFmpegRunner.extractFrame(this, src, out, "00:00:01");
            runOnUiThread(() -> {
                textStatus.setText(code == 0 ? R.string.status_done : R.string.status_error);
                textInfo.setText(code == 0 ? "关键帧: " + out.getAbsolutePath() : "抽帧失败 code=" + code);
            });
        });
    }

    private File pickSourceFile() {
        File dir = new File(getExternalFilesDir(null), "downloads");
        File f = newestFile(dir);
        if (f == null) {
            textStatus.setText(R.string.status_error);
            textInfo.setText("downloads 目录为空，请先下载");
            return null;
        }
        return f;
    }

    private static File newestFile(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] fs = dir.listFiles();
        if (fs == null || fs.length == 0) return null;
        File best = fs[0];
        for (File f : fs) {
            if (f.lastModified() > best.lastModified()) best = f;
        }
        return best;
    }

    private static String baseName(File f) {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(0, i) : n;
    }

    private void shareLog() {
        File dir = new File(getCacheDir(), "diag");
        File out = Diag.dumpToFile(dir);
        if (out == null) {
            textStatus.setText("日志写入失败");
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", out);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "分享诊断日志"));
    }

    private static String extractJsonField(String json, String key) {
        if (json == null) return "";
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        int start = i + needle.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start < json.length() && json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : "";
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    @Override
    public void onLog(String line) {
        runOnUiThread(() -> {
            if (line == null || line.isEmpty()) {
                textLog.setText("");
            } else {
                textLog.append(line + "\n");
            }
            scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onDestroy() {
        Diag.removeListener(this);
        super.onDestroy();
    }
}
