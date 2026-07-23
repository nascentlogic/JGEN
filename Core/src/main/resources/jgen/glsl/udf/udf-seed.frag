layout(location = 0) out uvec2 fragSeed; // GL_RG16UI

uniform sampler2D uSource;

#define ALPHA_THRESHOLD 0.01
#define INVALID 0xFFFFu

bool isSolid(ivec2 p, ivec2 bounds) {
    if (p.x < 0 || p.x >= bounds.x || p.y < 0 || p.y >= bounds.y) {
        return false;
    } return texelFetch(uSource, p, 0).a >= ALPHA_THRESHOLD;
}

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    ivec2 bounds = textureSize(uSource, 0);
    bool c = isSolid(coord,  bounds);
    bool t = isSolid(coord + ivec2( 0,  1), bounds);
    bool b = isSolid(coord + ivec2( 0, -1), bounds);
    bool l = isSolid(coord + ivec2(-1,  0), bounds);
    bool r = isSolid(coord + ivec2( 1,  0), bounds);
    bool isEdge = c && (!l || !r || !t || !b);
    if (isEdge) {
        fragSeed = uvec2(coord);
    } else {
        fragSeed = uvec2(INVALID);
    }
}