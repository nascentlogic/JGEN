layout(location = 0) out uvec2 fragSeed; // GL_RG16UI

uniform usampler2D uSource; // GL_RG16UI
uniform int uStepSize;

#define INVALID 0xFFFFu

bool inBounds(ivec2 p, ivec2 bounds) {
    return p.x >= 0 && p.x < bounds.x && p.y >= 0 && p.y < bounds.y;
}

float sqDist(vec2 p1, vec2 p2) {
    vec2 diff = p1 - p2;
    return dot(diff,diff);
}

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    ivec2 bounds = textureSize(uSource, 0);
    uvec2 bestSeed = texelFetch(uSource, coord, 0).rg;
    float bestDistSq = 1e9;
    if (bestSeed.x != INVALID) {
        bestDistSq = sqDist(vec2(coord),vec2(bestSeed));
    }
    for (int dy = -1; dy <= 1; ++dy) {
        for (int dx = -1; dx <= 1; ++dx) {
            ivec2 sampleCoord = coord + ivec2(dx, dy) * uStepSize;
            if (inBounds(sampleCoord, bounds)) {
                uvec2 nSeed = texelFetch(uSource, sampleCoord, 0).rg;
                if (nSeed.x != INVALID) {
                    float distSq = sqDist(vec2(coord),vec2(nSeed));
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        bestSeed = nSeed;
                    }
                }
            }
        }
    }
    fragSeed = bestSeed;
}