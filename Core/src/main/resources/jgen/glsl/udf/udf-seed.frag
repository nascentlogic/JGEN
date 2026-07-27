layout(location = 0) out uvec2 fragSeed; // GL_RG16UI
uniform sampler2D uSourceTexture;
uniform int uSourceChannels;
uniform float uThreshold; // 1 channel: red, 2,3,4 channels: alpha,

bool isSolid(ivec2 p, ivec2 bounds) {
    if (p.x < 0 || p.x >= bounds.x || p.y < 0 || p.y >= bounds.y) {
        return false;
    } vec4 texel = texelFetch(uSourceTexture, p, 0);
    if(uSourceChannels == 1) {
        return texel.r >= uThreshold;
    } else return texel.a >= uThreshold;
}

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    ivec2 bounds = textureSize(uSourceTexture, 0);
    bool c = isSolid(coord,  bounds);
    bool t = isSolid(coord + ivec2( 0,  1), bounds);
    bool b = isSolid(coord + ivec2( 0, -1), bounds);
    bool l = isSolid(coord + ivec2(-1,  0), bounds);
    bool r = isSolid(coord + ivec2( 1,  0), bounds);
    bool isEdge = c && (!l || !r || !t || !b);
    if (isEdge) { fragSeed = uvec2(coord);
    } else { fragSeed = uvec2(0xFFFFu); }
}
