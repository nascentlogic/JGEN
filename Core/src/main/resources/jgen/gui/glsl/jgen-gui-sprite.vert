layout (location=0) in vec2 aPos;   // gui screen position
layout (location=1) in vec2 aUV;    // UV coordinates
layout (location=2) in vec4 aColor; // sRGB format
layout (location=3) in float aId;   // Pixel ID used for mouse picking
layout (location=4) in float aData; // texture slot ++

uniform vec2 uResolution;
uniform float uScale;

struct VertexData {
    uint textureSlot;
    uint glowBits;
    uint antiAlias;
    uint transparentID;
};

VertexData unpackVertexData(float floatBits) {
    uint intBits = floatBitsToUint(floatBits);
    VertexData data;
    data.textureSlot =   (intBits      ) & 0x000F;
    data.glowBits =      (intBits >> 4 ) & 0xFFFF;
    data.antiAlias =     (intBits >> 20) & 0x0001;
    data.transparentID = (intBits >> 21) & 0x0001;
    return data;
}

out VSOUT {
    flat vec4 color;
    vec2 pos;
    vec2 uv;
    flat uint pixelID;
    flat uint texSlot;
    flat float glow;
    flat bool antiAlias;
    flat bool transparentID;
} vsOut;

vec3 srgbToLinear(vec3 c) {
    vec3 cutoff = vec3(lessThanEqual(c, vec3(0.04045)));
    vec3 higher = pow((c + 0.055) / 1.055, vec3(2.4));
    vec3 lower = c / 12.92;
    return mix(higher, lower, cutoff);
}

void main() {
    vsOut.pixelID = floatBitsToUint(aId);
    VertexData data = unpackVertexData(aData);
    vsOut.texSlot = data.textureSlot;
    vsOut.glow = float(data.glowBits) / 65535.0;
    vsOut.antiAlias = (data.antiAlias > 0u);
    vsOut.transparentID = (data.transparentID > 0u);

    vec3 rgb = srgbToLinear(aColor.rgb);
    float a = aColor.a * (255.0/254.0);
    vsOut.color = vec4(rgb,a);

    /*
        // aPos is in virtual space (0 to 960)
        // scale is 2.0
        // uResolution is physical (1920)
        vec2 physicalPos = aPos * scale; // 960 * 2.0 = 1920 (maps back to physical pixels)
        vec2 ndc = (physicalPos / uResolution) * 2.0 - 1.0;
    */

    vec2 pos = aPos * uScale;
    vsOut.pos = pos;
    vsOut.uv = aUV;
    vec2 ndc = (pos / uResolution) * 2.0 - 1.0;
    gl_Position = vec4(ndc,0.0,1.0);
}