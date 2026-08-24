layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

struct TextureRegion {
    vec2[4] pos; // bl | br | tl | tr
    vec2[4] uvs; // u1,v2 | u2,v2 | u1,v1 | u2,v1
};

struct CharData {
    TextureRegion region;
    vec4 color;
    uint font;
    float dfPixelRange;
    bool cursor;
};

// Input from Vertex Shader
in VSOUT {
    CharData character;
} gsIn[];

// Output to Fragment Shader
out GSOUT {
    vec2 uv;
    flat vec4 color;
    flat float dfPixelRange;
    flat uint texSlot;
    flat bool cursor;
} gsOut;

void main() {
    CharData ch = gsIn[0].character;
    // Pass flat attributes constant across all 4 vertices
    gsOut.color        = ch.color;
    gsOut.dfPixelRange = ch.dfPixelRange;
    gsOut.texSlot      = ch.font;
    gsOut.cursor       = ch.cursor;

    // Emit the 4 vertices in triangle_strip order (BL, BR, TL, TR)
    for (int i = 0; i < 4; i++) {
        gl_Position = vec4(ch.region.pos[i], 0.0, 1.0);
        gsOut.uv    = ch.region.uvs[i];
        EmitVertex();
    }

    EndPrimitive();
}