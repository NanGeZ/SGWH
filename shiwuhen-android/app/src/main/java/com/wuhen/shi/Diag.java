package com.wuhen.shi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Diag {

    public interface Listener {
        void onLog(String line);
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final StringBuilder BUFFER = new StringBuilder();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private Diag() {}

    public static void addListener(Listener l) {
        if (l != null) LISTENERS.add(l);
    }

    public static void removeListener(Listener l) {
        LISTENERS.remove(l);
    }

    public static String snapshot() {
        synchronized (BUFFER) {
            return BUFFER.toString();
        }
    }

    public static void clear() {
        synchronized (BUFFER) {
            BUFFER.setLength(0);
        }
        for (Listener l : LISTENERS) l.onLog("");
    }

    public static void log(String msg) {
        String line = "[" + SDF.format(new Date()) + "] " + msg;
        synchronized (BUFFER) {
            BUFFER.append(line).append('\n');
            if (BUFFER.length() > 120_000) {
                BUFFER.delete(0, BUFFER.length() - 80_000);
            }
        }
        for (Listener l : LISTENERS) l.onLog(line);
    }

    public static File dumpToFile(File dir) {
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "shi-diag-" + System.currentTimeMillis() + ".log");
        try (FileWriter w = new FileWriter(out)) {
            synchronized (BUFFER) {
                w.write(scrub(BUFFER.toString()));
            }
            return out;
        } catch (IOException e) {
            return null;
        }
    }

    static String scrub(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)(cookie|authorization|token|api[_-]?key|password)[=:][^\\s,;&]+", "$1=***")
                .replaceAll("\\b[0-9]{15,20}\\b", "***");
    }
}
