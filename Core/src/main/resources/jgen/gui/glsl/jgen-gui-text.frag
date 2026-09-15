layout (location = 0) out vec4 fColor;

uniform sampler2D[5] uTextures; // samples are linear

in GSOUT {
    vec2 uv;
    flat vec4 color; // straight linear color
    flat float screenPxRange;
    flat float dfPixelRange; // 2 * font padding
    flat uint texSlot;
    flat bool cursor;
} fsIn;


// Safely sample sampler2D array
vec4 sampleTexture(uint slot, vec2 uv) {
    switch(slot) {
        case 0:  return texture(uTextures[0], uv);
        case 1:  return texture(uTextures[1], uv);
        case 2:  return texture(uTextures[2], uv);
        case 3:  return texture(uTextures[3], uv);
        case 4:  return texture(uTextures[4], uv);
        default: return vec4(1.0);
    }
}

float median(vec3 v) {
    return max(min(v.r, v.g), min(max(v.r, v.g), v.b));
}

#define OUTLINED true       // Eventually passed in via vertex flags

// Constants
const vec4 OUTLINE_COLOR = vec4(0.0, 0.0, 0.0, 1.0); // Premultiplied linear outline color
const float OUTLINE_PX = 1.75;                         // Desired outline width in screen pixels
const float OUTLINE_BLEND = 0.2; // 0.0 = Pure Black, 0.5 = 50% Body / 50% Black, 1.0 = Pure Body Color

void main0() {
    vec4 color = fsIn.color;

    if (!fsIn.cursor) {
        vec3 msdf = sampleTexture(fsIn.texSlot, fsIn.uv).rgb;
        float sd = median(msdf);

        float safeScreenPxRange = max(fsIn.screenPxRange, 1.0);

        // Signed distance to glyph body edge in physical screen pixels
        float bodyDistPx = safeScreenPxRange * (sd - 0.5);

        // Exact 1-pixel sub-pixel anti-aliased body fill alpha
        float fillAlpha = clamp(bodyDistPx + 0.5, 0.0, 1.0);

        if (OUTLINED) {
            // 1. MSDF Corner Sharpness Correction (keeps acute corners sharp)
            float channelMax = max(msdf.r, max(msdf.g, msdf.b));
            float channelMin = min(msdf.r, min(msdf.g, msdf.b));
            float cornerFactor = clamp((channelMax - channelMin) * 0.5, 0.0, 0.35);
            float correctedOutlinePx = OUTLINE_PX * (1.0 + cornerFactor);

            // 2. Compute Distances & Alphas
            float outlineDistPx = bodyDistPx + correctedOutlinePx;
            float totalAlpha    = clamp(outlineDistPx + 0.5, 0.0, 1.0);

            // Isolate visible outline shell (prevents color bleed under transparent text)
            float outlineVisibleAlpha = clamp(totalAlpha - fillAlpha, 0.0, 1.0);

            // 3. Compute Effective Outline RGB (Blended with Black)
            // OUTLINE_BLEND = 0.0 -> vec3(0.0)
            // OUTLINE_BLEND = 1.0 -> color.rgb
            vec3 outlineRgb = color.rgb * OUTLINE_BLEND;

            // 4. Premultiplied Alpha Composite
            // Body contribution + Blended Outline contribution
            float bodyPremulAlpha    = color.a * fillAlpha;
            float outlinePremulAlpha = color.a * outlineVisibleAlpha;

            vec3 finalRgb = (color.rgb * bodyPremulAlpha) + (outlineRgb * outlinePremulAlpha);
            float finalA  = bodyPremulAlpha + outlinePremulAlpha;

            color.a *= fillAlpha;

            fColor = vec4(finalRgb, finalA);
            return;
        }

        color.a *= fillAlpha;
    }

    // Output linear premultiplied alpha color for non-outlined / cursor paths
    fColor = vec4(color.rgb * color.a, color.a);
}

void main7() {
    vec4 color = fsIn.color;

    if (!fsIn.cursor) {
        vec3 msdf = sampleTexture(fsIn.texSlot, fsIn.uv).rgb;
        float sd = median(msdf);

        float safeScreenPxRange = max(fsIn.screenPxRange, 1.0);

        // Signed distance to glyph body edge in physical screen pixels
        float bodyDistPx = safeScreenPxRange * (sd - 0.5);

        // Exact 1-pixel sub-pixel anti-aliased body fill alpha
        float fillAlpha = clamp(bodyDistPx + 0.5, 0.0, 1.0);

        if (OUTLINED) {
            // 1. MSDF Corner Sharpness Correction (keeps acute corners sharp)
            float channelMax = max(msdf.r, max(msdf.g, msdf.b));
            float channelMin = min(msdf.r, min(msdf.g, msdf.b));
            float cornerFactor = clamp((channelMax - channelMin) * 0.5, 0.0, 0.35);
            float correctedOutlinePx = OUTLINE_PX * (1.0 + cornerFactor);

            // 2. Compute Distances
            float outlineDistPx = bodyDistPx + correctedOutlinePx;

            // 3. Exact 1-pixel anti-aliased alpha for combined body + outline region
            float totalAlpha = clamp(outlineDistPx + 0.5, 0.0, 1.0);

            // 4. Premultiplied Alpha Output (Black outline adds 0.0 to RGB)
            float bodyPremulAlpha = color.a * fillAlpha;

            fColor.rgb = color.rgb * bodyPremulAlpha;
            fColor.a   = totalAlpha - fillAlpha * (1.0 - color.a);
            return;
        }

        color.a *= fillAlpha;
    }

    // Output linear premultiplied alpha color for non-outlined / cursor paths
    fColor = vec4(color.rgb * color.a, color.a);
}


void main() {
    vec4 color = fsIn.color;

    if (!fsIn.cursor) {
        vec3 msdf = sampleTexture(fsIn.texSlot, fsIn.uv).rgb;
        float sd = median(msdf);

        float safeScreenPxRange = max(fsIn.screenPxRange, 1.0);
        // Signed distance to glyph body edge in physical screen pixels
        // float bodyDistPx = fsIn.screenPxRange * (sd - 0.5);
        float bodyDistPx = safeScreenPxRange * (sd - 0.5);
        // Anti-aliased body fill alpha
        float fillAlpha = clamp(bodyDistPx + 0.5, 0.0, 1.0);

        // if (OUTLINED) {
        //     // 1. MSDF Corner Sharpness Correction (keeps acute corners sharp)
        //     float channelMax = max(msdf.r, max(msdf.g, msdf.b));
        //     float channelMin = min(msdf.r, min(msdf.g, msdf.b));
        //     float cornerFactor = clamp((channelMax - channelMin) * 0.5, 0.0, 0.35);
        //     float correctedOutlinePx = OUTLINE_PX * (1.0 + cornerFactor);
        //     // 2. Compute Distances
        //     float outlineDistPx = bodyDistPx + correctedOutlinePx;
        //     // 3. Hermite S-Curve Smoothstep (replaces linear clamp for perceptual anti-aliasing)
        //     float tFill  = clamp(bodyDistPx + 0.5, 0.0, 1.0);
        //     float tTotal = clamp(outlineDistPx + 0.5, 0.0, 1.0);
        //     float fillAlpha  = tFill * tFill * (3.0 - 2.0 * tFill);
        //     float totalAlpha = tTotal * tTotal * (3.0 - 2.0 * tTotal);
        //     // 4. Optimized Premultiplied Output
        //     float bodyPremulAlpha = color.a * fillAlpha;
        //     fColor.rgb = color.rgb * bodyPremulAlpha;
        //     // Fast path: if text is opaque (color.a == 1.0), totalAlpha IS the final alpha.
        //     fColor.a   = totalAlpha - fillAlpha * (1.0 - color.a);
        //     return;
        // }

        if (OUTLINED) {
            float outlineDistPx = bodyDistPx + OUTLINE_PX;
            float totalAlpha = clamp(outlineDistPx + 0.5, 0.0, 1.0);
            float outlineVisibleAlpha = clamp(totalAlpha - fillAlpha, 0.0, 1.0);
            // Color is already premultiplied right here:
            fColor.rgb = color.rgb * (color.a * fillAlpha);
            fColor.a   = (color.a * fillAlpha) + outlineVisibleAlpha;
            return; // or wrap the trailing fColor line in an else
        }

        color.a *= fillAlpha;

        // if (OUTLINED) {
        //     // Signed distance to outline outer edge in physical screen pixels
        //     float outlineDistPx = bodyDistPx + OUTLINE_PX;
        //     // Anti-aliased total shape alpha (body + outline)
        //     float totalAlpha = clamp(outlineDistPx + 0.5, 0.0, 1.0);
        //     // Calculate how much outline is visible outside the body fill
        //     // Subtracting fillAlpha prevents color bleeding under transparent text
        //     float outlineVisibleAlpha = clamp(totalAlpha - fillAlpha, 0.0, 1.0);
        //     // Composite: Combine body color and outline color cleanly
        //     vec3 finalRgb = (color.rgb * fillAlpha) + (OUTLINE_COLOR.rgb * outlineVisibleAlpha);
        //     float finalAlpha = (color.a * fillAlpha) + (OUTLINE_COLOR.a * outlineVisibleAlpha);
        //     color = vec4(finalRgb, finalAlpha);
        // } else {
        //     color.a *= fillAlpha;
        // }
    }

    // Output linear premultiplied alpha color
    fColor = vec4(color.rgb * color.a, color.a);
}


void main6() {
    vec4 color = fsIn.color;
    if(!fsIn.cursor) {
        vec3 msdf = sampleTexture(fsIn.texSlot, fsIn.uv).rgb;
        float sd = median(msdf);
        float distInScreenPx = fsIn.screenPxRange * (sd - 0.5);
        float alpha = clamp(distInScreenPx + 0.5, 0.0, 1.0);
        color.a *= smoothstep(0.0, 1.0, alpha);
    }
    // premultiply alpha
    // outputs linear space premultiplied color
    fColor = vec4(color.rgb * color.a,color.a);
}