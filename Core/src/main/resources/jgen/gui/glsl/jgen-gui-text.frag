layout (location = 0) out vec4 fColor;

uniform sampler2D[5] uTextures; // samples are linear

in GSOUT {
    vec2 uv;
    flat vec4 color; // linear rgba
    flat float screenPxRange;
    flat float dfPixelRange; // 2 * MSDF_RANGE
    flat float glow;
    flat uint texSlot;
    flat bool cursor;
    flat bool ouulined;
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


const float OUTLINE_PX = 1.75;                         // Desired outline width in screen pixels



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

        if (fsIn.ouulined) {
            float outlineDistPx = bodyDistPx + OUTLINE_PX;
            float totalAlpha = clamp(outlineDistPx + 0.5, 0.0, 1.0);
            float outlineVisibleAlpha = clamp(totalAlpha - fillAlpha, 0.0, 1.0);
            // Color is already premultiplied right here:
            fColor.rgb = color.rgb * (color.a * fillAlpha);
            fColor.a   = (color.a * fillAlpha) + outlineVisibleAlpha;
            return; // or wrap the trailing fColor line in an else
        }
        color.a *= fillAlpha;
    }


    // Output linear premultiplied alpha color
    fColor = vec4((color.rgb + color.rgb * 3.0 * fsIn.glow) * color.a, color.a);
}


