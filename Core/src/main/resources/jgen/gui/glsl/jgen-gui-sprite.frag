layout (location = 0) out vec4 fColor;
layout (location = 1) out uint fId;

uniform sampler2D[8] uTextures; // samples are linear

in VSOUT {
    flat vec4 color; // straight linear color
    vec2 pos;
    vec2 uv;
    flat uint pixelID;
    flat uint texSlot;
    flat float glow; // unused atm
    flat bool antiAlias;
    flat bool transparentID;
} fsIn;

// not used
vec3 linearToSRGB(vec3 c) {
    vec3 cutoff = vec3(lessThan(c, vec3(0.0031308)));
    vec3 higher = 1.055 * pow(c, vec3(1.0/2.4)) - 0.055;
    vec3 lower = 12.92 * c;
    return mix(higher, lower, cutoff);
}

// Safely query texture dimensions without illegal dynamic indexing
vec2 getTextureSize(uint slot) {
    switch(slot) {
        case 0:  return vec2(textureSize(uTextures[0], 0));
        case 1:  return vec2(textureSize(uTextures[1], 0));
        case 2:  return vec2(textureSize(uTextures[2], 0));
        case 3:  return vec2(textureSize(uTextures[3], 0));
        case 4:  return vec2(textureSize(uTextures[4], 0));
        case 5:  return vec2(textureSize(uTextures[5], 0));
        case 6:  return vec2(textureSize(uTextures[6], 0));
        case 7:  return vec2(textureSize(uTextures[7], 0));
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
        case 5:  return texture(uTextures[5], uv);
        case 6:  return texture(uTextures[6], uv);
        case 7:  return texture(uTextures[7], uv);
        default: return vec4(1.0);
    }
}

void main() {
    vec4 color = fsIn.color; // color <-- tint

    if (fsIn.texSlot != 8u) {
        vec2 uv;
        if (fsIn.antiAlias) {
            vec2 size = getTextureSize(fsIn.texSlot);
            vec2 texel = fsIn.uv * size;
            texel = floor(texel) + min(fract(texel) / fwidth(texel), 1.0) - 0.5;
            uv = texel / size;
        } else {
            uv = fsIn.uv;
        }

        // Hardware SRGBA8 converts to 32-bit Linear Float
        // Multiply colors in linear space
        color *= sampleTexture(fsIn.texSlot, uv);
    }

    if(!fsIn.transparentID && (color.a <= 0.0)) {
        discard;
    }

    // premultiply alpha
    // outputs linear space premultiplied color
    fColor = vec4(color.rgb * color.a,color.a);
    fId = fsIn.pixelID; // immediate mode gui pixel id
}