layout(location = 0) out vec3 fragNormal; // RGB8 output (Normal map encoded 0..1)
layout(location = 1) out float fragHeight; // R8 output (R channel contains height)

uniform sampler2D uUdfTexture;          // GL_R16F raw distance field
uniform sampler2D uSourceTexture;       // GL_SRGB8_ALPHA8 source texture
uniform int uSourceChannels;
uniform float uMaxDistance;             // Cutoff distance in pixels
uniform float uVolume;                  //
uniform float uVolumeExp;               // UDF volume profile exponent
uniform float uDetail;                  // Global micro-relief strength
uniform float uDetailExp;               // Detail contrast multiplier (default 1.0)
uniform float uNormalScalar;


// Calculates gamma-adjusted luminance
float getLuminance(ivec2 coord) {
    float value;
    if(uSourceChannels == 1) {
        value = texelFetch(uSourceTexture, coord, 0).r;
    } else {
        vec3 color = texelFetch(uSourceTexture, coord, 0).rgb;
        value = dot(color, vec3(0.2126, 0.7152, 0.0722));
    } return pow(value, uDetailExp);
}

float getUdfHeight(ivec2 coord) {
    float dist = texelFetch(uUdfTexture, coord, 0).r;
    if (dist <= 0.0) return 0.0;
    float normalizedDist = clamp(dist / uMaxDistance, 0.0, 1.0);
    float dome = sin(normalizedDist * 1.57079632679);
    return pow(dome, uVolumeExp);
}

// Calculates Laplacian High-Pass Micro-Relief
float getDeltaLuminance(ivec2 coord, ivec2 size) {
    if (uDetail <= 0.0) return 0.0;
    float centerLum = getLuminance(coord);
    float neighborSum = 0.0;
    ivec2 adjacentKernel[8] = ivec2[8](
    ivec2(-1, -1), ivec2(0, -1), ivec2(1, -1),
    ivec2(-1,  0),               ivec2(1,  0),
    ivec2(-1,  1), ivec2(0,  1), ivec2(1,  1));
    for (int i = 0; i < 8; i++) {
        ivec2 c = clamp(coord + adjacentKernel[i], ivec2(0), size - ivec2(1));
        neighborSum += getLuminance(c);
    } float rawDelta = centerLum - (neighborSum * 0.125);
    // Soft compression via hyperbolic tangent to cap extreme contrast spikes
    return tanh(rawDelta);
}

float computeHeight(ivec2 coord, ivec2 size) {
    if (uVolume <= 0.0) return 0.0;
    float volume = getUdfHeight(coord);
    if (volume <= 0.0) return 0.0;
    float deltaLum = getDeltaLuminance(coord, size);
    float detail = deltaLum * sqrt(volume);

    float totalHeight = clamp(volume * uVolume,0.0,1.0 - 0.05 * uDetail) + (detail * uDetail);
    return clamp(totalHeight, 0.0, 1.0);
}


void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    ivec2 size = textureSize(uUdfTexture, 0);
    float hC = computeHeight(coord, size);
    float hL = (coord.x > 0)          ? computeHeight(coord + ivec2(-1,  0), size) : hC;
    float hR = (coord.x < size.x - 1) ? computeHeight(coord + ivec2( 1,  0), size) : hC;
    float hD = (coord.y > 0)          ? computeHeight(coord + ivec2( 0, -1), size) : hC;
    float hU = (coord.y < size.y - 1) ? computeHeight(coord + ivec2( 0,  1), size) : hC;
    float dX = (hR - hL) * uNormalScalar;
    float dY = (hU - hD) * uNormalScalar;
    vec3 normal = normalize(vec3(-dX, dY, 1.0));
    fragNormal = normal * 0.5 + 0.5;
    fragHeight = hC;
}