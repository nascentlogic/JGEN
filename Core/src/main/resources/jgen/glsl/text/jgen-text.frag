layout (location = 0) out vec4 fColor;

uniform sampler2D[5] uTextures; // samples are linear

in GSOUT {
    vec2 uv;
    flat vec4 color; // straight linear color
    flat float dfPixelRange; // 2 * font padding
    flat uint texSlot;
    flat bool cursor;
} fsIn;

// Safely query texture dimensions without illegal dynamic indexing
vec2 getTextureSize(uint slot) {
    switch(slot) {
        case 0:  return vec2(textureSize(uTextures[0], 0));
        case 1:  return vec2(textureSize(uTextures[1], 0));
        case 2:  return vec2(textureSize(uTextures[2], 0));
        case 3:  return vec2(textureSize(uTextures[3], 0));
        case 4:  return vec2(textureSize(uTextures[4], 0));
        default: return vec2(1.0);
    }
}

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

// Width of the baked distance band, in *screen* pixels (2D).
float screenPxRange(vec2 uv, vec2 atlasSize, float dfPixelRange) {
    // UV size of the full distance band
    vec2 bandInUV = vec2(dfPixelRange) / atlasSize;
    // screen pixels covered by one unit of UV
    vec2 screenPixelsPerUV = 1.0 / fwidth(uv);
    // average horizontal/vertical band width on screen
    return max(0.5 * dot(bandInUV, screenPixelsPerUV), 1.0);
}

void main() {

    vec4 color = fsIn.color;

    if(!fsIn.cursor) {
        vec2 atlasSize = getTextureSize(fsIn.texSlot);
        vec3 msdf = sampleTexture(fsIn.texSlot, fsIn.uv).rgb;
        float sd = median(msdf);  // ~0.5 at the edge
        float w  = screenPxRange(fsIn.uv, atlasSize, fsIn.dfPixelRange);
        float distInScreenPx = w * (sd - 0.5);
        float alpha = clamp(distInScreenPx + 0.5, 0.0, 1.0);
        color.a *= alpha;
    }

    // premultiply alpha
    // outputs linear space premultiplied color
    fColor = vec4(color.rgb * color.a,color.a);
}