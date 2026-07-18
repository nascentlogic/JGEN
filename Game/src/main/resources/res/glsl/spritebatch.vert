layout (location=0) in vec2 aPos;   // position in world coordinats
layout (location=1) in vec2 aUV;    // UV coordinates
layout (location=2) in vec4 aColor; // linear RGBA color (samples are linear)
layout (location=3) in float aData; // texture slot ++

uniform mat4 uCombined; // projection view matrix

out VSOUT {
    vec4 color;
    vec2 pos;
    vec2 uv;
    flat uint texSlot;
    flat bool antiAlias;
} vsOut;


void main() {
    uint data = floatBitsToUint(aData);
    vsOut.texSlot = data & 0x0F;
    vsOut.antiAlias = ((data >> 4) & 0x01) == 1u;
    vsOut.pos = aPos;
    vsOut.color = aColor;
    vsOut.color.a *= (255.0/254.0);
    gl_Position = uCombined * vec4(aPos,0.0,1.0);
}