package org.sikrip.vboeditor.helper;


import java.util.concurrent.TimeUnit;

public final class TimeHelper {

    private TimeHelper() {
    }

    public static String getTimeString(long timeMillis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        long millis = timeMillis - TimeUnit.SECONDS.toMillis(seconds) - TimeUnit.MINUTES.toMillis(minutes);
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
