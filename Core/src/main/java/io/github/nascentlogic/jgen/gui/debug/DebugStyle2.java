package io.github.nascentlogic.jgen.gui.debug;

import io.github.nascentlogic.jgen.gfx.Color;

/**
 * F.Dahl, 10/12/2026
 */
public class DebugStyle2 {


    public static final int FONT_TEXT = 0;
    public static final int FONT_MONO = 1;
    public static final float CONTRAST = 0.1f;
    public static final float HOVER_LERP_MUL = 8.0f;



    public static final class ColorPair {

        private static final Color TMP = new Color();
        private final Color defaultColor;
        private final Color hoveredColor;

        public ColorPair() {
            defaultColor = new Color();
            hoveredColor = new Color();
        }

        public ColorPair(String defaultHex) {
            defaultColor = new Color(defaultHex);
            hoveredColor = new Color(defaultColor).brighten(CONTRAST);
        }

        public ColorPair(String defaultHex, String hoveredHex) {
            defaultColor = new Color(defaultHex);
            hoveredColor = new Color(hoveredHex);
        }
        public Color lerp(float t) {
            if (t <= 0) return defaultColor;
            if (t >= 1) return hoveredColor;
            return Color.lerpOklab(defaultColor, hoveredColor,t, TMP);
        }
        public Color hoverLerp(boolean hovered, float duration) {
            if (hovered) {
                return lerp(duration * HOVER_LERP_MUL);
            } return defaultColor;
        }

        public Color defaultColor() { return defaultColor; }
        public Color hoveredColor() { return hoveredColor; }
        public Color defaultDark(float amount) { return TMP.set(defaultColor).darken(amount); }
        public Color hoveredDark(float amount) { return TMP.set(hoveredColor).darken(amount); }
        public Color defaultBright(float amount) { return TMP.set(defaultColor).brighten(amount); }
        public Color hoveredBright(float amount) { return TMP.set(hoveredColor).brighten(amount); }

    }
}
