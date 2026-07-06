package io.github.nascentlogic.jgen.gfx;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.joml.Vector4f;

import java.io.IOException;

/**
 * Linear Space RGBA Color.
 * <p><strong>Note:</strong> GSON serialized to sRGBA format hex string.
 * Because of this the Color will lose precision. But the values will stabilize.</p>
 * F.Dahl, 7/5/2026
 */
public class Color {

    public static float GAMMA = 2.2f;
    public static float GAMMA_INV = 1f / GAMMA;

    private float r, g, b, a;       // linear
    transient private float p;      // packed
    transient private boolean d = true;

    public Color() { set(1,1,1,1); }
    public Color(int intBits) { setIntBits(intBits); }
    public Color(Color color) { set(color); }
    public Color(float r, float g, float b, float a) { set(r, g, b, a); }

    public Color set(Color color) { return set(color.r,color.g,color.b,color.a); }
    public Color setR(float r) { this.r = Math.clamp(r,0f,1f); d = true; return this; }
    public Color setG(float g) { this.g = Math.clamp(g,0f,1f); d = true; return this; }
    public Color setB(float b) { this.b = Math.clamp(b,0f,1f); d = true; return this; }
    public Color setA(float a) { this.a = Math.clamp(a,0f,1f); d = true; return this; }

    public Color set(float r, float g, float b, float a) {
        this.r = Math.clamp(r,0f,1f);
        this.g = Math.clamp(g,0f,1f);
        this.b = Math.clamp(b,0f,1f);
        this.a = Math.clamp(a,0f,1f);
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

    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

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

    public Color srgbToLinear() {
        this.r = (float) Math.pow(r, GAMMA);
        this.g = (float) Math.pow(g, GAMMA);
        this.b = (float) Math.pow(b, GAMMA);
        this.d = true;
        return this;
    }

    public Color linearToSrgb() {
        this.r = (float) Math.pow(r, GAMMA_INV);
        this.g = (float) Math.pow(g, GAMMA_INV);
        this.b = (float) Math.pow(b, GAMMA_INV);
        this.d = true;
        return this;
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
    public Vector4f toHSVA(Vector4f dst) {
        float sR = (float) Math.pow(r, Color.GAMMA_INV);
        float sG = (float) Math.pow(g, Color.GAMMA_INV);
        float sB = (float) Math.pow(b, Color.GAMMA_INV);
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

    @Override
    public String toString() {
        int bits = intBits();
        return "[" + (bits & 0xFF) + "," + ((bits >> 8) & 0xFF) + "," + ((bits >> 16) & 0xFF) + "," + ((bits >> 24) & 0xFF) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Color other)) return false;
        return this.intBits() == other.intBits();
    }

    public boolean equalsPrecise(Color other) {
        if (other == null) return false;
        if (this == other) return true;
        return  this.r == other.r &&
                this.g == other.g &&
                this.b == other.b &&
                this.a == other.a;
    }

    @Override
    public int hashCode() {
        return this.intBits();
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

    // =============================================================================
    // Serialization Adapter
    // =============================================================================

    public static class Adapter extends TypeAdapter<Color> {

        private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
        private static final ThreadLocal<char[]> CHAR_BUFFER = ThreadLocal.withInitial(() -> new char[9]);

        @Override
        public void write(JsonWriter out, Color color) throws IOException {
            if (color != null) {
                int r = (int) (Math.pow(color.r,GAMMA_INV) * 255f) & 0xFF;
                int g = (int) (Math.pow(color.g,GAMMA_INV) * 255f) & 0xFF;
                int b = (int) (Math.pow(color.b,GAMMA_INV) * 255f) & 0xFF;
                int a = (int) (color.a * 255f) & 0xFF;
                char[] buffer = CHAR_BUFFER.get();
                buffer[0] = '#';
                buffer[1] = HEX_CHARS[(r >>> 4) & 0xF];  buffer[2] = HEX_CHARS[r & 0xF];
                buffer[3] = HEX_CHARS[(g >>> 4) & 0xF];  buffer[4] = HEX_CHARS[g & 0xF];
                buffer[5] = HEX_CHARS[(b >>> 4) & 0xF];  buffer[6] = HEX_CHARS[b & 0xF];
                buffer[7] = HEX_CHARS[(a >>> 4) & 0xF];  buffer[8] = HEX_CHARS[a & 0xF];
                out.value(new String(buffer));
            } else out.nullValue();
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } String value = in.nextString();
            if (value == null || value.isEmpty()) return new Color();
            int start = value.charAt(0) == '#' ? 1 : 0;
            int length = value.length() - start;
            if (length != 6 && length != 8) return new Color();
            try {
                int rBits = parseHexPair(value, start);
                int gBits = parseHexPair(value, start + 2);
                int bBits = parseHexPair(value, start + 4);
                int aBits = (length == 8) ? parseHexPair(value, start + 6) : 255;
                float r = rBits / 255f;
                float g = gBits / 255f;
                float b = bBits / 255f;
                float a = aBits / 255f;
                return new Color(r,g,b,a).srgbToLinear();
            } catch (Exception e) { return new Color(); }
        }

        private int parseHexPair(String str, int index) {
            int h1 = Character.digit(str.charAt(index), 16);
            int h2 = Character.digit(str.charAt(index + 1), 16);
            if (h1 == -1 || h2 == -1) throw new IllegalArgumentException();
            return (h1 << 4) | h2;
        }
    }
}
