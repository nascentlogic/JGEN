
// CHAR VERTEX
// float x          - pen x position
// float y          - pen y position
// float data       - packed float bits
// float color      - character linear color

// DATA (vertex data filed above)
// uint glyph       - 8 bit (32 - 126) outside range = cursor
// uint fSize       - 8 bit font size
// uint font        - 8 bit (0 - 7)
// uint unused      - 8 bit (later)

// outline color will be a uniform.
// changing outline color flushes the batch

struct VertexData {
    uint char;
    uint fSize;
    uint font;
    uint unused;
};

VertexData unpackVertexData(float floatBits) {
    uint intBits = floatBitsToUint(floatBits);
    VertexData data;
    data.char   = (intBits      ) & 0xFF;
    data.fSize  = (intBits >> 8 ) & 0xFF;
    data.font   = (intBits >> 16) & 0xFF;
    data.unused = (intBits >> 24) & 0xFF;
    return data;
}

#define TEXT_BINDING_POINT 8
#define NUM_FONTS 8
#define NUM_PRINTABLE_CHARS 95

// 32 byte
struct Glyph {
    vec4 uvCoords; // u, v, u2, v2
    vec2 size;     // width, height
    vec2 offset;   // pen -> bottom left
};

// 3088 byte
struct Font {
    Glyph[NUM_PRINTABLE_CHARS + 1] glyphs; // characters + cursor
    uint texSlot;   // font texture 0 -> 7
    float size;     // font size (generated size in pixels)
    float padding;  // glyph padding (used for sdf band width)
    float unused;   // unused
};

// 24704 Byte
layout (std140, binding = TEXT_BINDING_POINT) uniform TextBlock {
    Font[NUM_FONTS] fonts;
} textBlock;