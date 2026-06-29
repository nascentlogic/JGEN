package io.github.nascentlogic.jgen.utils;

/**
 * F.Dahl, 5/8/2026
 */
public class JgenUtils {


    private static final String[] BYTE_UNITS = {"B", "KB", "MB", "GB", "TB"};
    /** Formats a byte value into a human-readable String */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        int unitIndex = 0;
        long value = bytes;
        while (value >= 1024 && unitIndex < BYTE_UNITS.length - 1) {
            value >>= 10;
            unitIndex++;
        } double displayValue = (double) bytes / (1L << (10 * unitIndex));
        return switch (unitIndex) {
            case 0 -> bytes + " " + BYTE_UNITS[0];
            case 1 -> Math.round(displayValue) + " " + BYTE_UNITS[1];
            case 2 -> String.format("%.1f %s", displayValue, BYTE_UNITS[2]);
            default -> String.format("%.2f %s", displayValue, BYTE_UNITS[unitIndex]);
        };
    }

    /** Formats a nanosecond time duration value into a human-readable String */
    public static String formatNanos(long nanos) {
        if (nanos <= 0) return "0 ns";
        double us = nanos / 1000.0;
        if (us < 1.0) return nanos + " ns";
        double ms = us / 1000.0;
        if (ms < 1.0) return Math.round(us) + " μs";
        double s = ms / 1000.0;
        if (s < 1.0) return String.format("%.1f ms", ms);
        double m = s / 60.0;
        if (m < 1.0) return String.format("%.2f s", s);
        double h = m / 60.0;
        if (h < 1.0) return String.format("%.2f m", m);
        return String.format("%.2f h", h);
    }





}
