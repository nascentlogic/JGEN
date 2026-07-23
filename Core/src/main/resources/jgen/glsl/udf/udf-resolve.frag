layout(location = 0) out float fragDist; // GL_R16F

uniform usampler2D uSource; // GL_RG16UI
uniform int uDistFunc;     // 0 = Euclidean, 1 = Manhattan, 2 = Chebyshev

#define INVALID 0xFFFFu

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    uvec2 seed = texelFetch(uSource, coord, 0).rg;

    if (seed.x == INVALID) {
        fragDist = 0.0;
        return;
    }
    vec2 d = abs(vec2(coord) - vec2(seed));

    if (uDistFunc == 1) {
        // Manhattan
        fragDist = d.x + d.y;
    } else if (uDistFunc == 2) {
        // Chebyshev
        fragDist = max(d.x, d.y);
    } else {
        // Euclidean (Default)
        fragDist = length(d);
    }
    // 0.5 offset guarantees even isolated 1x1 pixels resolve to 0.5
    fragDist += 0.5; // make sure += is alloved (it should be)
}