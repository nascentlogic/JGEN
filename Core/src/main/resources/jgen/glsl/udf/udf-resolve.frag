layout(location = 0) out float fragDist; // GL_R16F
uniform usampler2D uSeedTexture; // GL_RG16UI
uniform sampler2D uSourceTexture;
uniform int uSourceChannels;
uniform float uThreshold; // 1 channel: red, 2,3,4 channels: alpha,
uniform int uDistFunc;    // 0 = Euclidean, 1 = Manhattan, 2 = Chebyshev

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    uvec2 seed = texelFetch(uSeedTexture, coord, 0).rg;
    vec4 texel = texelFetch(uSourceTexture, coord, 0);
    float soruceValue = uSourceChannels == 1 ? texel.r : texel.a;
    if (seed.x == 0xFFFFu || soruceValue < uThreshold) {
        fragDist = 0.0;
        return;
    }
    vec2 d = abs(vec2(coord) - vec2(seed));
    if (uDistFunc == 1) {
        fragDist = d.x + d.y;
    } else if (uDistFunc == 2) {
        fragDist = max(d.x, d.y);
    } else { fragDist = length(d); }
    // 0.5 offset guarantees even isolated 1x1 pixels resolve to 0.5
    fragDist += 0.5;
}

