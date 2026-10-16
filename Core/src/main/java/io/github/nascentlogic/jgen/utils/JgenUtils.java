package io.github.nascentlogic.jgen.utils;

import io.github.nascentlogic.jgen.gfx.Color;
import org.joml.primitives.Rectanglef;

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

    private static int rectStackDepth = 0;
    private static int colorStackDepth = 0;
    private static final Rectanglef[] rectStack = new Rectanglef[48];
    private static final Color[] colorStack = new Color[32];

    static {
        for (int i = 0; i < rectStack.length; i++) rectStack[i] = new Rectanglef();
        for (int i = 0; i < colorStack.length; i++) colorStack[i] = new Color();
    }


    public static Rectanglef pushRect() {
        if (rectStackDepth == rectStack.length) throw new IllegalStateException("util rect stack overflow!");
        return rectStack[rectStackDepth++];
    }
    public static Rectanglef pushRect(Rectanglef rect) { return pushRect(rect.minX,rect.minY,rect.maxX,rect.maxY); }
    public static Rectanglef pushRect(float width, float height) { return pushRect(0,0,width,height); }
    public static Rectanglef pushRect(float minX, float minY, float maxX, float maxY) {
        if (rectStackDepth == rectStack.length)
            throw new IllegalStateException("util rect stack overflow!");
        return rectStack[rectStackDepth++].setMin(minX,minY).setMax(maxX,maxY);
    }
    public static void popRect() { popRect(1); }
    public static void popRect(int count) {
        if (rectStackDepth - count < 0)
            throw new IllegalStateException("util rect stack underFlow!");
        rectStackDepth -= count;
    }

    public static Color pushColor() {
        if (colorStackDepth == colorStack.length) throw new IllegalStateException("util color stack overflow!");
        return colorStack[colorStackDepth++];
    }

    public static Color pushColor(Color color) {
        if (colorStackDepth == colorStack.length) throw new IllegalStateException("util color stack overflow!");
        return colorStack[colorStackDepth++].set(color);
    }

    public static Color pushColor(int r, int g, int b) { return pushColor(r,g,b,255); }
    public static Color pushColor(int r, int g, int b, int a) {
        if (colorStackDepth == colorStack.length) throw new IllegalStateException("util color stack overflow!");
        return colorStack[colorStackDepth++];
    }

    public static void popColor() { popColor(1); }
    public static void popColor(int count) {
        if (colorStackDepth - count < 0)
            throw new IllegalStateException("util color stack underFlow!");
        colorStackDepth -= count;
    }





}
