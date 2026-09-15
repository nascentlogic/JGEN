layout (location=0) in vec2 aPos;   // gui screen position
layout (location=1) in vec2 aUV;    // UV coordinates
layout (location=2) in vec4 aColor; // linear RGBA color (samples are linear)
layout (location=3) in float aId;   // Pixel ID used for mouse picking
layout (location=4) in float aData; // texture slot ++

uniform vec2 uResolution;

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

void main() {
    vsOut.pixelID = floatBitsToUint(aId);
    VertexData data = unpackVertexData(aData);
    vsOut.texSlot = data.textureSlot;
    vsOut.glow = float(data.glowBits) / 65535.0;
    vsOut.antiAlias = (data.antiAlias > 0u);
    vsOut.transparentID = (data.transparentID > 0u);

    vsOut.color = aColor;
    vsOut.pos = aPos;
    vsOut.uv = aUV;
    vsOut.color.a *= (255.0/254.0);
    vec2 ndc = (aPos / uResolution) * 2.0 - 1.0;
    gl_Position = vec4(ndc,0.0,1.0);
}