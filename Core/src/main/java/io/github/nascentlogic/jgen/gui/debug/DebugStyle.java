package io.github.nascentlogic.jgen.gui.debug;

import io.github.nascentlogic.jgen.gfx.Color;

/**
 * F.Dahl, 10/10/2026
 */
public class DebugStyle {

    public static final int FONT_TEXT = 0;
    public static final int FONT_MONO = 1;
    public static final float CONTRAST = 0.1f;
    public static final float TRACK_WIDTH = 5.0f;
    public static final float HOVER_BRIGHT = 0.1f;
    public static final float HOVER_LERP_MUL = 8.0f;

    public static final Color BG_COLOR = new Color("1E1E22FF");
    public static final Color BG_COLOR_LIGHT = new Color(BG_COLOR).brighten(0.1f);
    public static final Color BG_COLOR_DARK = new Color(BG_COLOR).darken(0.1f);


    public static final float WINDOW_BORDER = 3.0f;
    public static final float WINDOW_MARGIN = 1.0f;
    public static final float WINDOW_PADDING = 2.0f;
    public static final float WINDOW_BORDER_TOTAL = WINDOW_BORDER + WINDOW_MARGIN + WINDOW_PADDING;
    public static final float WINDOW_ANIM_DURATION = 0.25f;
    public static final ColorPair WINDOW_BORDER_COLOR = new ColorPair("3F3F46FF",CONTRAST);
    public static final Color WINDOW_EDGE_COLOR = new Color(WINDOW_BORDER_COLOR.defaultColor).darken(CONTRAST * 2.5f);

    public static final float NAV_BORDER = 2.0f;
    public static final float NAV_HEIGHT = 24f;
    public static final int NAV_FONT = FONT_MONO;
    public static final ColorPair NAV_COLOR = new ColorPair("22385CFF",CONTRAST);
    public static final Color NAV_LABEL_COLOR = new Color("FFFFFFFF");

    public static final float GROUP_BORDER = 2.0f;
    public static final float GROUP_HOVER_TRANSFORM = 2.0f;
    public static final float[] GROUP_HEIGHT = new float[]{ 22f, 20f, 20f };
    public static final int GROUP_FONT = FONT_TEXT;
    public static final ColorPair[] GROUP_COLORS = new ColorPair[] {
            new ColorPair("22385CFF",CONTRAST),
            new ColorPair("1F4B41FF",CONTRAST),
            new ColorPair("22312CFF",CONTRAST)};
    public static final Color[] GROUP_LABEL_COLORS = new Color[] {
            new Color("F4F6F9FF"),
            new Color("E0EBE6FF"),
            new Color("CCD6D2FF")};


    public static final float FIELD_MARGIN = 3.0f;
    public static final float FIELD_BORDER = 1.0f;
    public static final float FIELD_PADDING = 2.0f;

    public static final float LIST_TRANSFORM = 3.0f;
    public static final float LIST_MARGIN = 3.0f;
    public static final float LIST_BORDER = 1.0f; // x3 for width
    public static final float LIST_PADDING = 2.0f;
    public static final float LIST_ITEM_HEIGHT = 20f;
    public static final int LIST_FONT = FONT_MONO;

    public static final Color LIST_BORDER_COLOR = new Color(WINDOW_BORDER_COLOR.defaultColor());
    public static final ColorPair LIST_LABEL_COLOR = new ColorPair("E0EBE6FF",CONTRAST);
    public static final ColorPair LIST_HANDLE_COLOR = new ColorPair("1F4B41FF",CONTRAST);





    public static final class ColorPair {

        private static final Color TMP = new Color();
        private final Color defaultColor;
        private final Color hoveredColor;

        public ColorPair() {
            defaultColor = new Color();
            hoveredColor = new Color();
        }

        public ColorPair(String defaultHex, float contrast) {
            defaultColor = new Color(defaultHex);
            hoveredColor = new Color(defaultColor).brighten(contrast);
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
