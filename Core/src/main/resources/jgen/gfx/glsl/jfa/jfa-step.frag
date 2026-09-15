layout(location = 0) out vec2 fDestination; // Stores pixel coords (e.g. 384.0, 512.0)

uniform sampler2D uSource; // RG16F output from previous pass
uniform ivec4 uRegion;     // Active sprite rect in atlas: ivec4(minX, minY, maxX, maxY)
uniform int uStepSize;     // Stride in pixels: 32, 16, 8, 4, 2, 1...

const ivec2 OFFSETS[9] = ivec2[9](
ivec2(-1, -1), ivec2( 0, -1), ivec2( 1, -1),
ivec2(-1,  0), ivec2( 0,  0), ivec2( 1,  0),
ivec2(-1,  1), ivec2( 0,  1), ivec2( 1,  1));

bool withinRegion(ivec2 p) {
    return p.x >= uRegion.x && p.x <= uRegion.z &&
    p.y >= uRegion.y && p.y <= uRegion.w;
}

void main() {
    ivec2 currentPos = ivec2(gl_FragCoord.xy);
    float bestDistSq = 99999999.0;
    vec2 bestCoord = vec2(-1.0);
    for (int i = 0; i < 9; i++) {
        ivec2 neighborPos = currentPos + (OFFSETS[i] * uStepSize);
        if (withinRegion(neighborPos)) {
            vec2 seedCoord = texelFetch(uSource, neighborPos, 0).rg;
            if (seedCoord.x >= 0.0) {
                vec2 diff = vec2(currentPos) - seedCoord;
                float distSq = dot(diff, diff);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestCoord = seedCoord;
                }
            }
        }
    }
    fDestination = bestCoord;
}