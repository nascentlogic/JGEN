#define ALPHA_THRESHOLD 0.1

layout(location = 0) out float fDestination; // Single-channel float output (e.g., GL_R16F or GL_R8)

uniform sampler2D uJFATexture;  // RG16F output from final JFA step pass
uniform sampler2D uSourceAtlas; // Original sprite atlas for alpha testing
uniform float uMaxDistance;     // Max distance in pixels for 0.0-1.0 mapping saturation
uniform float uHeightExponent;  // UDF only: < 1.0 for rounded dome, 1.0 for cone, > 1.0 for steep ridge
uniform float uEdgeOffset;      // UDF only: Base thickness offset for edge pixels (e.g., 0.5px)
uniform bool uModeSDF;          // true = Full Signed Distance Field, false = UDF Heightmap

bool isSolid(ivec2 p) {
    return texelFetch(uSourceAtlas, p, 0).a > ALPHA_THRESHOLD;
}

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    vec2 nearestSeed = texelFetch(uJFATexture, p, 0).rg;
    bool solid = isSolid(p);

    if (nearestSeed.x < 0.0) {
        if (uModeSDF) {
            fDestination = solid ? 1.0 : 0.0;
        } else {
            fDestination = 0.0;
        } return;
    }

    float rawDist = distance(vec2(p), nearestSeed);

    if (uModeSDF) {
        // --- MODE 1: SIGNED DISTANCE FIELD (SDF) ---
        // Linear normalization [-uMaxDistance, +uMaxDistance] -> [-1.0, +1.0]
        float normalDist = clamp(rawDist / max(uMaxDistance, 0.001), 0.0, 1.0);
        float signedDist = solid ? normalDist : -normalDist;
        // Map [-1.0, +1.0] -> [0.0, 1.0] where 0.5 is the exact silhouette boundary
        fDestination = signedDist * 0.5 + 0.5;
    }
    else {
        // --- MODE 2: INSIDE-ONLY UDF VOLUME HEIGHTMAP ---
        if (!solid) {
            fDestination = 0.0; // Outside space stays strictly at floor level (0.0)
            return;
        }
        // Add edge offset (e.g. +0.5px) so 1x1 or outer edge pixels get immediate base elevation
        float distWithOffset = rawDist + uEdgeOffset;
        float normalizedHeight = clamp(distWithOffset / max(uMaxDistance, 0.001), 0.0, 1.0);
        if (normalizedHeight <= 0.0) {
            fDestination = 0.0;
        } else {
            fDestination = pow(normalizedHeight, max(uHeightExponent, 0.01));
        }
    }
}