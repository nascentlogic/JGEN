#define ALPHA_THRESHOLD 0.1

layout(location = 0) out vec2 fDestination; // RG16F seed texture

uniform sampler2D uSource; // Input sprite atlas
// Java CPU side
// int minX = sprite.x;
// int minY = sprite.y;
// int maxX = sprite.x + sprite.width - 1;  // Inclusive last pixel
// int maxY = sprite.y + sprite.height - 1; // Inclusive last pixel
uniform ivec4 uRegion;     // Active sprite rect in atlas: ivec4(minX, minY, maxX, maxY)
uniform bool uModeSDF;     // true = Full SDF (2-px seed), false = UDF Heightmap (1-px seed)

bool isSolid(ivec2 p) {
    if (p.x < uRegion.x || p.x > uRegion.z || p.y < uRegion.y || p.y > uRegion.w) {
        return false;
    } return texelFetch(uSource, p, 0).a > ALPHA_THRESHOLD;
}

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    bool current = isSolid(p);
    bool left    = isSolid(p + ivec2(-1,  0));
    bool right   = isSolid(p + ivec2( 1,  0));
    bool top     = isSolid(p + ivec2( 0,  1));
    bool bottom  = isSolid(p + ivec2( 0, -1));
    bool isBoundary = false;
    if (uModeSDF) {
        // Mode 1: Full SDF (Seeds inside and outside edges)
        isBoundary = (current ^^ left)  ||
        (current ^^ right) ||
        (current ^^ top)   ||
        (current ^^ bottom);
    } else {
        // Mode 2: UDF Heightmap (Seeds inside edges only)
        isBoundary = current && (!left || !right || !top || !bottom);
    }
    if (isBoundary) {
        fDestination = vec2(p);
    } else {
        fDestination = vec2(-1.0);
    }
}