package io.github.nascentlogic.jgen.gfx;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.joml.Vector4f;

import java.io.IOException;

import static io.github.nascentlogic.jgen.utils.JgenMath.clamp;

/**
 * Linear Space RGBA Color.
 * <p><strong>Note:</strong> GSON serialized to sRGBA format hex string.
 * Because of this the Color will lose precision. But the values will stabilize.</p>
 * F.Dahl, 7/5/2026
 */
public class Color {

    public static final Color CLEAR       = new Color(0f, 0f, 0f, 0f);
    public static final Color BLACK       = new Color(0f, 0f, 0f, 1f);
    public static final Color WHITE       = new Color(1f, 1f, 1f, 1f);
    public static final Color RED         = new Color(1f, 0f, 0f, 1f);
    public static final Color GREEN       = new Color(0f, 1f, 0f, 1f);
    public static final Color BLUE        = new Color(0f, 0f, 1f, 1f);
    public static final Color YELLOW      = new Color(1f, 1f, 0f, 1f);
    public static final Color CYAN        = new Color(0f, 1f, 1f, 1f);
    public static final Color MAGENTA     = new Color(1f, 0f, 1f, 1f);

    private float r, g, b, a;  // linear
    transient private float p; // packed
    transient private boolean d = true;

    public Color() { set(1,1,1,1); }
    public Color(Color color) { set(color); }
    /** sRGB hex value --> linear Color */
    public Color(String hex) { setHex(hex); }
    public Color(int intBits) { setIntBits(intBits); }
    public Color(float r, float g, float b, float a) { set(r, g, b, a); }

    public Color set(Color color) { return set(color.r,color.g,color.b,color.a); }
    public Color setR(float r) { this.r = clamp(r); d = true; return this; }
    public Color setG(float g) { this.g = clamp(g); d = true; return this; }
    public Color setB(float b) { this.b = clamp(b); d = true; return this; }
    public Color setA(float a) { this.a = clamp(a); d = true; return this; }
    public Color set(float r, float g, float b, float a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
        this.d = true;
        return this;
    }

    public Color setIntBits(int intBits) {
        this.r = ((intBits)         & 0xFF) / 255f;
        this.g = ((intBits >> 8)    & 0xFF) / 255f;
        this.b = ((intBits >> 16)   & 0xFF) / 255f;
        this.a = ((intBits >> 24)   & 0xFF) / 255f;
        this.d = true;
        return this;
    }

    public Color setPackedFormat(float packed) {
        int i = Float.floatToRawIntBits(packed);
        i |= (int)((i >>> 24) * (255f / 254f)) << 24;
        return setIntBits(i);
    }

    /**
     * Sets this color from a sRGB Hex code string (e.g., "#FF0000")
     */
    public Color setHex(String hex) {
        return fromHex(hex,this);
    }

    /**
     * Sets this color from a raw Linear Hex code string without gamma alteration.
     */
    public Color setHexLinear(String hex) {
        return fromHexLinear(hex, this);
    }

    /**
     * Sets this color's channels using values from a JOML Vector4f (assumed Linear).
     */
    public Color setVec4(Vector4f vec) {
        return set(vec.x, vec.y, vec.z, vec.w);
    }

    /**
     * Sets this color from an RGBA float array at the specified offset.
     */
    public Color setArray(float[] src, int offset) {
        return set(src[offset], src[offset + 1], src[offset + 2], src[offset + 3]);
    }

    public Color add(Color other) { return add(this, other, this); }
    public Color sub(Color other) { return sub(this, other, this); }
    public Color mul(Color other) { return mul(this, other, this); }
    public Color div(Color other) { return div(this, other, this); }

    public Color add(float r, float g, float b, float a) { return add(this, r, g, b, a, this); }
    public Color sub(float r, float g, float b, float a) { return sub(this, r, g, b, a, this); }
    public Color mul(float r, float g, float b, float a) { return mul(this, r, g, b, a, this); }
    public Color div(float r, float g, float b, float a) { return div(this, r, g, b, a, this); }

    public Color scale(float scalar) { return scale(this,scalar,this); }
    public Color scaleRgb(float scalar) { return scaleRgb(this,scalar,this); }

    public Color blend(Color other) {
        return blend(this,other,this);
    }

    /**
     * Linearly interpolates this color instance toward a target state.
     */
    public Color lerp(Color target, float t) {
        return lerpLinear(this, target, t, this);
    }

    /**
     * Inverts all components
     */
    public Color invert() {
        return set(1f - r, 1f - g, 1f - b, 1f - a);
    }

    /**
     * Inverts the RGB components while preserving the alpha channel.
     */
    public Color invertRgb() {
        return set(1f - r, 1f - g, 1f - b, a);
    }

    /**
     * Premultiplies the color components by the alpha channel. (Irreversible)
     * Note: This keeps the alpha channel exactly as it is for the GPU blending track.
     */
    public Color premultiplyAlpha() {
        return set(r * a, g * a, b * a, a);
    }


    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

    public float sR() { return linearToSrgb(r); }
    public float sG() { return linearToSrgb(g); }
    public float sB() { return linearToSrgb(b); }
    public float sA() { return a; }

    public float packedFormat() {
        if (d) { int i = intBits();
            p = Float.intBitsToFloat(i & 0xfeffffff);
            d = false;
        } return p;
    }

    public int intBits() {
        int rBits = (int) (r * 255f) & 0xFF;
        int gBits = (int) (g * 255f) & 0xFF;
        int bBits = (int) (b * 255f) & 0xFF;
        int aBits = (int) (a * 255f) & 0xFF;
        return rBits | (gBits << 8) | (bBits << 16) | (aBits << 24);
    }

    /** Exposes the internal linear values directly into a provided destination vector. */
    public Vector4f toVec4Rgba(Vector4f dst) {
        return dst.set(r, g, b, a);
    }

    public Vector4f toVec4Srgba(Vector4f dst) {
        dst.x = linearToSrgb(r);
        dst.y = linearToSrgb(g);
        dst.z = linearToSrgb(b);
        dst.w = a;
        return dst;
    }

    /** Writes this Color into a float array at the specified offset in RGBA order. */
    public void toArrayRgba(float[] dst, int offset) {
        dst[offset]     = r;
        dst[offset + 1] = g;
        dst[offset + 2] = b;
        dst[offset + 3] = a;
    }

    /**
     * Converts this Linear Color into sRGB-space HSV (Hue, Saturation, Value) + Alpha.
     * Writes the results into the provided destination Vector4f to avoid allocation.
     * @param dst The Vector4f to store the results:
     * <ul>
     * <li><strong>x</strong> : Hue [0.0 to 360.0]</li>
     * <li><strong>y</strong> : Saturation [0.0 to 1.0]</li>
     * <li><strong>z</strong> : Value/Brightness [0.0 to 1.0]</li>
     * <li><strong>w</strong> : Alpha [0.0 to 1.0]</li>
     * </ul>
     * @return The dst vector populated with HSV+A data.
     */
    public Vector4f toVec4HSV(Vector4f dst) {
        float sR = linearToSrgb(r);
        float sG = linearToSrgb(g);
        float sB = linearToSrgb(b);
        float max = Math.max(sR, Math.max(sG, sB)); // v
        float min = Math.min(sR, Math.min(sG, sB));
        float delta = max - min;
        float h = 0f;
        float s = 0f;
        if (max != 0f) s = delta / max;
        if (delta != 0f) {
            if (max == sR) h = (sG - sB) / delta + (sG < sB ? 6f : 0f);
            else if (max == sG) h = (sB - sR) / delta + 2f;
            else h = (sR - sG) / delta + 4f;
            h *= 60f;
        } return dst.set(h,s,max,a);
    }

    /**
     * Calculates the perceptual luminance (brightness) of this linear color.
     * Useful for checking contrast ratios or applying grayscale effects.
     * @return A value between 0.0 (perceptually black) and 1.0 (perceptually bright white).
     */
    public float luminance() {
        // Exact linear coefficients from ITU-R BT.709
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    @Override
    public String toString() {
        int bits = intBits();
        return String.format("[%3d,%3d,%3d,%3d]",
                (bits & 0xFF),
                ((bits >> 8) & 0xFF),
                ((bits >> 16) & 0xFF),
                ((bits >> 24) & 0xFF)
        );
    }

    /**
     * Converts the color into an sRGB Hex string representation format.
     */
    public String toHexString() {
        int rBits = (int) (sR() * 255f) & 0xFF;
        int gBits = (int) (sG() * 255f) & 0xFF;
        int bBits = (int) (sB() * 255f) & 0xFF;
        int aBits = (int) (sA() * 255f) & 0xFF;
        return String.format("#%02X%02X%02X%02X", rBits, gBits, bBits, aBits);
    }

    /**
     * Converts this color into a raw, un-de-gamma'd Linear Hex string format (#RRGGBBAA).
     * Useful for direct debugging or logging exact component memory states.
     */
    public String toHexStringLinear() {
        int rBits = (int) (r * 255f) & 0xFF;
        int gBits = (int) (g * 255f) & 0xFF;
        int bBits = (int) (b * 255f) & 0xFF;
        int aBits = (int) (a * 255f) & 0xFF;
        return String.format("#%02X%02X%02X%02X", rBits, gBits, bBits, aBits);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Color other)) return false;
        return this.intBits() == other.intBits();
    }

    @Override
    public int hashCode() {
        return this.intBits();
    }

    public static float srgbToLinear(float val) {
        float c = Math.clamp(val, 0.0f, 1.0f);
        return (c <= 0.04045f) ? (c / 12.92f) : (float) Math.pow((c + 0.055) / 1.055, 2.4);
    }

    public static float linearToSrgb(float val) {
        float c = Math.clamp(val, 0.0f, 1.0f);
        return (c <= 0.0031308f) ? (c * 12.92f) : (float) (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    public static Vector4f srgbToLinear(Vector4f srgb, Vector4f dst) {
        dst.x = srgbToLinear(srgb.x);
        dst.y = srgbToLinear(srgb.y);
        dst.z = srgbToLinear(srgb.z);
        dst.w = clamp(srgb.w);
        return dst;
    }

    public static Vector4f linearToSrgb(Vector4f linear, Vector4f dst) {
        dst.x = linearToSrgb(linear.x);
        dst.y = linearToSrgb(linear.y);
        dst.z = linearToSrgb(linear.z);
        dst.w = clamp(linear.w);
        return dst;
    }

    public static Color add(Color src, Color other, Color dst) {
        return dst.set(src.r + other.r, src.g + other.g, src.b + other.b, src.a + other.a);
    }

    public static Color sub(Color src, Color other, Color dst) {
        return dst.set(src.r - other.r, src.g - other.g, src.b - other.b, src.a - other.a);
    }

    public static Color mul(Color src, Color other, Color dst) {
        return dst.set(src.r * other.r, src.g * other.g, src.b * other.b, src.a * other.a);
    }

    public static Color div(Color src, Color other, Color dst) {
        float r = other.r == 0f ? 0f : src.r / other.r;
        float g = other.g == 0f ? 0f : src.g / other.g;
        float b = other.b == 0f ? 0f : src.b / other.b;
        float a = other.a == 0f ? 0f : src.a / other.a;
        return dst.set(r, g, b, a);
    }

    public static Color add(Color src, float r, float g, float b, float a, Color dst) {
        return dst.set(src.r + r, src.g + g, src.b + b, src.a + a);
    }

    public static Color sub(Color src, float r, float g, float b, float a, Color dst) {
        return dst.set(src.r - r, src.g - g, src.b - b, src.a - a);
    }

    public static Color mul(Color src, float r, float g, float b, float a, Color dst) {
        return dst.set(src.r * r, src.g * g, src.b * b, src.a * a);
    }

    public static Color div(Color src, float r, float g, float b, float a, Color dst) {
        float outR = r == 0f ? 0f : src.r / r;
        float outG = g == 0f ? 0f : src.g / g;
        float outB = b == 0f ? 0f : src.b / b;
        float outA = a == 0f ? 0f : src.a / a;
        return dst.set(outR, outG, outB, outA);
    }

    public static Color scaleRgb(Color src, float scalar, Color dst) {
        return dst.set(src.r * scalar, src.g * scalar, src.b * scalar, src.a);
    }

    public static Color scale(Color src, float scalar, Color dst) {
        return dst.set(src.r * scalar, src.g * scalar, src.b * scalar, src.a * scalar);
    }

    public static Color lerpLinear(Color start, Color target, float t, Color dst) {
        if (t <= 0) return dst.set(start);
        if (t >= 1) return dst.set(target);
        float r = start.r + (target.r - start.r) * t;
        float g = start.g + (target.g - start.g) * t;
        float b = start.b + (target.b - start.b) * t;
        float a = start.a + (target.a - start.a) * t;
        return dst.set(r, g, b, a);
    }

    /**
     * Interpolates between two colors inside the perceptually uniform Oklab space.
     * Prevents muddy midpoints, delivering beautiful, natural color transitions.
     */
    public static Color lerpOklab(Color start, Color target, float t, Color dst) {
        if (t <= 0f) return dst.set(start);
        if (t >= 1f) return dst.set(target);

        // 1. Convert Start Color from Linear RGB to Oklab
        float l1 = 0.4122214708f * start.r + 0.5363325363f * start.g + 0.0514459929f * start.b;
        float m1 = 0.2119034982f * start.r + 0.6806995451f * start.g + 0.1073969566f * start.b;
        float s1 = 0.0883024619f * start.r + 0.2817188376f * start.g + 0.6299787005f * start.b;

        l1 = (float) Math.cbrt(l1);
        m1 = (float) Math.cbrt(m1);
        s1 = (float) Math.cbrt(s1);

        float L1 = 0.2104542553f * l1 + 0.7936177850f * m1 - 0.0040720468f * s1;
        float A1 = 1.9779984951f * l1 - 2.4285922050f * m1 + 0.4505937099f * s1;
        float B1 = 0.0259040371f * l1 + 0.7827717662f * m1 - 0.8086757660f * s1;

        // 2. Convert Target Color from Linear RGB to Oklab
        float l2 = 0.4122214708f * target.r + 0.5363325363f * target.g + 0.0514459929f * target.b;
        float m2 = 0.2119034982f * target.r + 0.6806995451f * target.g + 0.1073969566f * target.b;
        float s2 = 0.0883024619f * target.r + 0.2817188376f * target.g + 0.6299787005f * target.b;

        l2 = (float) Math.cbrt(l2);
        m2 = (float) Math.cbrt(m2);
        s2 = (float) Math.cbrt(s2);

        float L2 = 0.2104542553f * l2 + 0.7936177850f * m2 - 0.0040720468f * s2;
        float A2 = 1.9779984951f * l2 - 2.4285922050f * m2 + 0.4505937099f * s2;
        float B2 = 0.0259040371f * l2 + 0.7827717662f * m2 - 0.8086757660f * s2;

        // 3. Linearly Interpolate the components within Oklab Space
        float L = L1 + (L2 - L1) * t;
        float A = A1 + (A2 - A1) * t;
        float B = B1 + (B2 - B1) * t;
        float alpha = start.a + (target.a - start.a) * t;

        // 4. Invert Oklab back to Linear LMS Space
        float l = L + 0.3963377774f * A + 0.2158037573f * B;
        float m = L - 0.1055613458f * A - 0.0638541728f * B;
        float s = L - 0.0894841775f * A - 1.2914855480f * B;

        l = l * l * l;
        m = m * m * m;
        s = s * s * s;

        // 5. Invert LMS back to Linear RGB Workspace
        float r = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;

        return dst.set(r, g, b, alpha);
    }

    /**
     * Parses a standard sRGB Hex code string (e.g., "#FF0000") into a Linear Color object.
     * Safely catches format errors and falls back to White if the string is corrupted.
     */
    public static Color fromHex(String hex, Color dst) {
        if (hex == null || hex.isEmpty()) return dst.set(1f, 1f, 1f, 1f);
        int start = hex.charAt(0) == '#' ? 1 : 0;
        int length = hex.length() - start;
        if (length != 6 && length != 8) return dst.set(1f, 1f, 1f, 1f);
        try {
            int rBits = Integer.parseInt(hex.substring(start, start + 2), 16);
            int gBits = Integer.parseInt(hex.substring(start + 2, start + 4), 16);
            int bBits = Integer.parseInt(hex.substring(start + 4, start + 6), 16);
            int aBits = (length == 8) ? Integer.parseInt(hex.substring(start + 6, start + 8), 16) : 255;
            return dst.set(
                    srgbToLinear(rBits / 255f),
                    srgbToLinear(gBits / 255f),
                    srgbToLinear(bBits / 255f),
                    aBits / 255f);
        } catch (NumberFormatException e) {
            return dst.set(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Parses a raw Linear Hex code string directly into a Color object without gamma alteration.
     * Safely catches format errors and falls back to White if the string is corrupted.
     */
    public static Color fromHexLinear(String hex, Color dst) {
        if (hex == null || hex.isEmpty()) return dst.set(1f, 1f, 1f, 1f);
        int start = hex.charAt(0) == '#' ? 1 : 0;
        int length = hex.length() - start;
        if (length != 6 && length != 8) return dst.set(1f, 1f, 1f, 1f);
        try {
            int rBits = Integer.parseInt(hex.substring(start, start + 2), 16);
            int gBits = Integer.parseInt(hex.substring(start + 2, start + 4), 16);
            int bBits = Integer.parseInt(hex.substring(start + 4, start + 6), 16);
            int aBits = (length == 8) ? Integer.parseInt(hex.substring(start + 6, start + 8), 16) : 255;
            return dst.set(rBits / 255f, gBits / 255f, bBits / 255f, aBits / 255f);
        } catch (NumberFormatException e) {
            return dst.set(1f, 1f, 1f, 1f);
        }
    }

    // =============================================================================
    // Serialization Adapter
    // =============================================================================

    public static class Adapter extends TypeAdapter<Color> {

        private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
        private static final ThreadLocal<char[]> CHAR_BUFFER = ThreadLocal.withInitial(() -> new char[9]);

        @Override
        public void write(JsonWriter out, Color color) throws IOException {
            if (color != null) {
                out.beginArray();
                out.value(linearToSrgb(color.r));
                out.value(linearToSrgb(color.g));
                out.value(linearToSrgb(color.b));
                out.value(color.a); // Alpha does not use gamma correction
                out.endArray();
            } else out.nullValue();
            //if (color != null) {
            //    int r = (int) (linearToSrgb(color.r) * 255f) & 0xFF;
            //    int g = (int) (linearToSrgb(color.g) * 255f) & 0xFF;
            //    int b = (int) (linearToSrgb(color.b) * 255f) & 0xFF;
            //    int a = (int) (color.a * 255f) & 0xFF;
            //    char[] buffer = CHAR_BUFFER.get();
            //    buffer[0] = '#';
            //    buffer[1] = HEX_CHARS[(r >>> 4) & 0xF];  buffer[2] = HEX_CHARS[r & 0xF];
            //    buffer[3] = HEX_CHARS[(g >>> 4) & 0xF];  buffer[4] = HEX_CHARS[g & 0xF];
            //    buffer[5] = HEX_CHARS[(b >>> 4) & 0xF];  buffer[6] = HEX_CHARS[b & 0xF];
            //    buffer[7] = HEX_CHARS[(a >>> 4) & 0xF];  buffer[8] = HEX_CHARS[a & 0xF];
            //    out.value(new String(buffer));
            //} else out.nullValue();
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            in.beginArray();
            float r = srgbToLinear((float) in.nextDouble());
            float g = srgbToLinear((float) in.nextDouble());
            float b = srgbToLinear((float) in.nextDouble());
            float a = (float) in.nextDouble();
            in.endArray();
            return new Color(r, g, b, a);
            //if (in.peek() == JsonToken.NULL) {
            //    in.nextNull();
            //    return null;
            //} String value = in.nextString();
            //if (value == null || value.isEmpty()) return new Color();
            //int start = value.charAt(0) == '#' ? 1 : 0;
            //int length = value.length() - start;
            //if (length != 6 && length != 8) return new Color();
            //try {
            //    int rBits = parseHexPair(value, start);
            //    int gBits = parseHexPair(value, start + 2);
            //    int bBits = parseHexPair(value, start + 4);
            //    int aBits = (length == 8) ? parseHexPair(value, start + 6) : 255;
            //    float r = srgbToLinear(rBits / 255f);
            //    float g = srgbToLinear(gBits / 255f);
            //    float b = srgbToLinear(bBits / 255f);
            //    float a = aBits / 255f;
            //    return new Color(r,g,b,a);
            //} catch (Exception e) { return new Color(); }
        }

        private int parseHexPair(String str, int index) {
            int h1 = Character.digit(str.charAt(index), 16);
            int h2 = Character.digit(str.charAt(index + 1), 16);
            if (h1 == -1 || h2 == -1) throw new IllegalArgumentException();
            return (h1 << 4) | h2;
        }
    }

    // =============================================================================
    // CPU OpenGL color blending
    // Not optimized for performance
    // =============================================================================

    /**
     * Blends a source color over a destination color using standard alpha compositing.
     * This handles translucent-over-translucent blending perfectly without color distortion.
     *
     * @param src The foreground color being painted.
     * @param dst The background color underneath.
     * @param out The target Color object to store the result.
     * @return The 'out' color reference populated with the correctly blended state.
     */
    public static Color blend(Color src, Color dst, Color out) {
        // 1. Calculate the mathematically correct final alpha channel first
        float outA = src.a() + dst.a() * (1f - src.a());
        // Edge case: If both colors are completely invisible, return a clear color to avoid divide-by-zero
        if (outA <= 0f) return out.set(0f, 0f, 0f, 0f);
        // 2. Mix the color channels accounting for the destination's alpha weight,
        // and divide by the final alpha to bring the result back into Straight Space.
        float r = (src.r() * src.a() + dst.r() * dst.a() * (1f - src.a())) / outA;
        float g = (src.g() * src.a() + dst.g() * dst.a() * (1f - src.a())) / outA;
        float b = (src.b() * src.a() + dst.b() * dst.a() * (1f - src.a())) / outA;
        return out.set(r, g, b, outA);
    }

    /**
     * High-level method to blend two full Color objects using separate RGB and Alpha configurations.
     * This perfectly simulates glBlendFuncSeparate and glBlendEquationSeparate.
     *
     * @param src              The incoming source color.
     * @param srcRGBFactor     The blend factor used to scale the source RGB components.
     * @param srcAlphaFactor   The blend factor used to scale the source Alpha component.
     * @param dst              The current background destination color.
     * @param dstRGBFactor     The blend factor used to scale the destination RGB components.
     * @param dstAlphaFactor   The blend factor used to scale the destination Alpha component.
     * @param rgbEquation      The blend equation used to combine the weighted RGB channels.
     * @param alphaEquation    The blend equation used to combine the weighted Alpha channels.
     * @param out              The target Color object that will store the clamped results.
     * @return The 'out' color reference populated with the final blended state.
     */
    public static Color blendSeparate(Color src, BlendFactor srcRGBFactor, BlendFactor srcAlphaFactor,
                                      Color dst, BlendFactor dstRGBFactor, BlendFactor dstAlphaFactor,
                                      BlendEquation rgbEquation, BlendEquation alphaEquation, Color out) {
        // 1. Calculate color channels via the primitive method
        float r = blendChannel(src.r, src.a, srcRGBFactor, dst.r, dst.a, dstRGBFactor, rgbEquation);
        float g = blendChannel(src.g, src.a, srcRGBFactor, dst.g, dst.a, dstRGBFactor, rgbEquation);
        float b = blendChannel(src.b, src.a, srcRGBFactor, dst.b, dst.a, dstRGBFactor, rgbEquation);
        // 2. Calculate the alpha track via the separate primitive method
        float a = blendAlpha(src.a(), srcAlphaFactor, dst.a(), dstAlphaFactor, alphaEquation);
        // 3. Delegate to your container (which manages its own bounds clamping)
        return out.set(r, g, b, a);
    }

    /**
     * High-level method to blend two full Color objects using uniform blend states across all channels.
     * This perfectly simulates glBlendFunc and glBlendEquation.
     * @param src       The incoming source color.
     * @param srcFactor The uniform blend factor applied to both source RGB and Alpha tracks.
     * @param dst       The current background destination color.
     * @param dstFactor The uniform blend factor applied to both destination RGB and Alpha tracks.
     * @param equation  The uniform blend equation used to combine all components.
     * @param out       The target Color object that will store the clamped results.
     * @return The 'out' color reference populated with the final blended state.
     */
    public static Color blend(Color src, BlendFactor srcFactor,
                              Color dst, BlendFactor dstFactor,
                              BlendEquation equation, Color out) {
        // Forward directly to the separate track handler by mirroring the uniform parameters
        return blendSeparate(src, srcFactor, srcFactor, dst, dstFactor, dstFactor, equation, equation, out);
    }

    /**
     * Blends a single color channel component (Red, Green, or Blue) using raw primitive values.
     * This simulates a single color pipeline track before final fragment buffer clamping.
     *
     * @param srcVal    The raw source color channel intensity (e.g., source Red).
     * @param srcAlpha  The alpha channel intensity of the source color.
     * @param srcFactor The blend factor setting used to scale the source component.
     * @param dstVal    The raw destination background color channel intensity (e.g., destination Red).
     * @param dstAlpha  The alpha channel intensity of the destination background color.
     * @param dstFactor The blend factor setting used to scale the destination component.
     * @param equation  The mathematical blending equation used to combine the two weighted parts.
     * @return The blended, unclamped linear result for this channel.
     */
    public static float blendChannel(float srcVal, float srcAlpha, BlendFactor srcFactor,
                                     float dstVal, float dstAlpha, BlendFactor dstFactor,
                                     BlendEquation equation) {
        // Use our new enum functional methods directly to determine the weights
        float srcWeight = srcFactor.colorBlend(srcVal, srcAlpha, dstVal, dstAlpha);
        float dstWeight = dstFactor.colorBlend(srcVal, srcAlpha, dstVal, dstAlpha);
        // Compute the final scaled parts and combine them using the equation lambda
        return equation.combine(srcVal * srcWeight, dstVal * dstWeight);
    }

    /**
     * Blends the Alpha channel component using raw primitive values.
     * This simulates the isolated alpha blending track before final fragment buffer clamping.
     *
     * @param srcAlpha  The alpha channel intensity of the source color.
     * @param srcFactor The blend factor setting used to scale the source alpha.
     * @param dstAlpha  The alpha channel intensity of the destination background color.
     * @param dstFactor The blend factor setting used to scale the destination alpha.
     * @param equation  The mathematical blending equation used to combine the two weighted parts.
     * @return The blended, unclamped linear result for the alpha channel.
     */
    public static float blendAlpha(float srcAlpha, BlendFactor srcFactor,
                                   float dstAlpha, BlendFactor dstFactor,
                                   BlendEquation equation) {
        // Evaluate the alpha track weights using the separate alpha formulas
        float srcWeight = srcFactor.alphaBlend(srcAlpha, dstAlpha);
        float dstWeight = dstFactor.alphaBlend(srcAlpha, dstAlpha);
        // Compute the final scaled alpha components and combine them
        return equation.combine(srcAlpha * srcWeight, dstAlpha * dstWeight);
    }

    public enum BlendFactor {
        ZERO(
                (src, sA, dst, dA) -> 0f,
                (sA, dA) -> 0f
        ), ONE(
                (src, sA, dst, dA) -> 1f,
                (sA, dA) -> 1f
        ), SRC_COLOR(
                (src, sA, dst, dA) -> src,
                (sA, dA) -> sA
        ), ONE_MINUS_SRC_COLOR(
                (src, sA, dst, dA) -> 1f - src,
                (sA, dA) -> 1f - sA
        ), DST_COLOR(
                (src, sA, dst, dA) -> dst,
                (sA, dA) -> dA
        ), ONE_MINUS_DST_COLOR(
                (src, sA, dst, dA) -> 1f - dst,
                (sA, dA) -> 1f - dA
        ), SRC_ALPHA(
                (src, sA, dst, dA) -> sA,
                (sA, dA) -> sA
        ), ONE_MINUS_SRC_ALPHA(
                (src, sA, dst, dA) -> 1f - sA,
                (sA, dA) -> 1f - sA
        ), DST_ALPHA(
                (src, sA, dst, dA) -> dA,
                (sA, dA) -> dA
        ), ONE_MINUS_DST_ALPHA(
                (src, sA, dst, dA) -> 1f - dA,
                (sA, dA) -> 1f - dA
        );

        private final ColorFormula colorFormula;
        private final AlphaFormula alphaFormula;

        BlendFactor(ColorFormula colorFormula, AlphaFormula alphaFormula) {
            this.colorFormula = colorFormula;
            this.alphaFormula = alphaFormula;
        }

        /** Computes the weight modifier for a color channel (Red, Green, or Blue). */
        public float colorBlend(float src, float sA, float dst, float dA) {
            return colorFormula.compute(src, sA, dst, dA);
        }

        /** Computes the weight modifier for the Alpha channel. */
        public float alphaBlend(float sA, float dA) {
            return alphaFormula.compute(sA, dA);
        }

        @FunctionalInterface
        public interface ColorFormula {
            float compute(float src, float sA, float dst, float dA);
        }

        @FunctionalInterface
        public interface AlphaFormula {
            float compute(float sA, float dA);
        }
    }

    public enum BlendEquation {
        ADD(Float::sum),
        SUBTRACT((srcPart, dstPart) -> srcPart - dstPart),
        REVERSE_SUBTRACT((srcPart, dstPart) -> dstPart - srcPart),
        MIN(Math::min),
        MAX(Math::max);

        private final EquationFormula formula;

        BlendEquation(EquationFormula formula) { this.formula = formula; }

        /** Combines the weighted source and destination channel components. */
        public float combine(float srcPart, float dstPart) {
            return formula.compute(srcPart, dstPart);
        }
        @FunctionalInterface
        public interface EquationFormula {
            float compute(float srcPart, float dstPart);
        }
    }




}
