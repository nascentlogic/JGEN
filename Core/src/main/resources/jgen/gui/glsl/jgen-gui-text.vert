layout (location = 0) in vec3 aVertex; // pen x,y | data
layout (location = 1) in vec4 aColor; // color

#define TEXT_BINDING_POINT 8
#define NUM_FONTS 5u
#define NUM_PRINTABLE_CHARS 95u
#define FIRST_CHAR 32u
#define LAST_CHAR 126u

uniform vec2 uResolution;

// tl------tr
// |        |
// |        |
// bl------br
// triangle-strip: bl | br | tl | br | tl | tr
struct TextureRegion {
    vec2[4] pos; // bl | br | tl | tr
    vec2[4] uvs; // u1,v2 | u2,v2 | u1,v1 | u2,v1
};

struct CharData {
    TextureRegion region; // NDC
    vec4 color;
    uint font;
    float screenPxRange; // Changed from dfPixelRange to actual screen space range
    float dfPixelRange;
    bool cursor;
};

out VSOUT {
    CharData character;
} vsOut;

struct Glyph {
    vec4 uvCoords; // u, v, u2, v2
    vec2 size;     // width, height
    vec2 offset;   // pen -> bottom left
};

struct Font {
    Glyph[NUM_PRINTABLE_CHARS + 1] glyphs; // characters + cursor
    float size;     // font size (generated size in pixels)
    float msdfRange;// (sdf band width)
    float unused0;  // unused
    float unused1;  // unused
};

layout (std140, binding = TEXT_BINDING_POINT) uniform TextBlock {
    Font[NUM_FONTS] fonts;
    uint[NUM_FONTS] indexMap;
} textBlock;


struct VertexData {
    uint ch;
    uint gSize;
    uint font;
    uint unused;
};

VertexData unpackVertexData(float floatBits) {
    uint intBits = floatBitsToUint(floatBits);
    VertexData data;
    data.ch     = (intBits      ) & 0xFF;
    data.gSize  = (intBits >> 8 ) & 0xFF;
    data.font   = (intBits >> 16) & 0xFF;
    data.unused = (intBits >> 24) & 0xFF;
    data.font = textBlock.indexMap[data.font];
    return data;
}

TextureRegion generateRegion(Glyph glyph, vec2 penPos, float scale) {
    TextureRegion region;
    float u1 = glyph.uvCoords.x; // left
    float v1 = glyph.uvCoords.y; // top
    float u2 = glyph.uvCoords.z; // right
    float v2 = glyph.uvCoords.w; // bottom
    vec2 size   = glyph.size * scale;
    vec2 offset = glyph.offset * scale; // pen → bottom-left
    vec2 bl = penPos + offset;
    vec2 br = bl + vec2(size.x, 0.0);
    vec2 tl = bl + vec2(0.0, size.y);
    vec2 tr = bl + size;
    region.pos[0] = (bl / uResolution) * 2.0 - 1.0;
    region.pos[1] = (br / uResolution) * 2.0 - 1.0;
    region.pos[2] = (tl / uResolution) * 2.0 - 1.0;
    region.pos[3] = (tr / uResolution) * 2.0 - 1.0;
    region.uvs[0] = vec2(u1, v2); // bl
    region.uvs[1] = vec2(u2, v2); // br
    region.uvs[2] = vec2(u1, v1); // tl
    region.uvs[3] = vec2(u2, v1); // tr
    return region;
}

void main() {

    const float ALPHA_SCALE = 255.0 / 254.0;
    vec4 color = aColor;
    color.a *= ALPHA_SCALE;

    vec2 penScreenPos = aVertex.xy;
    float glyphDataPacked = aVertex.z;
    VertexData vertexData = unpackVertexData(glyphDataPacked);
    Font font = textBlock.fonts[vertexData.font];

    Glyph glyph;
    bool isCursor;

    if (vertexData.ch < FIRST_CHAR || vertexData.ch > LAST_CHAR) {
        glyph = font.glyphs[NUM_PRINTABLE_CHARS]; // cursor glyph
        isCursor = true;
    } else {
        glyph = font.glyphs[vertexData.ch - FIRST_CHAR];
        isCursor = false;
    }

    float scale = float(vertexData.gSize) / font.size;

    CharData charData;
    charData.region = generateRegion(glyph, penScreenPos, scale);
    charData.color = color;
    charData.font = vertexData.font;

    charData.screenPxRange = (font.msdfRange * 2.0) * scale;
    charData.dfPixelRange = font.msdfRange * 2.0;
    charData.cursor = isCursor;
    vsOut.character = charData;

}












