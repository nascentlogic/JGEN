layout(location = 0) out vec4 fColor;

uniform sampler2D uTextureBG; // linear RGB
uniform sampler2D uTextureFG; // linear PMA RGBA

in vec2 vTexCoord;

vec3 linearToSRGB(vec3 c) {
    vec3 cutoff = vec3(lessThan(c, vec3(0.0031308)));
    vec3 higher = 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055;
    vec3 lower = 12.92 * c;
    return mix(higher, lower, cutoff);
}

void main() {
    vec4 bg = texture(uTextureBG, vTexCoord);
    vec4 fg = texture(uTextureFG, vTexCoord);
    vec3 compositeRGB = bg.rgb * (1.0 - fg.a) + fg.rgb;
    fColor = vec4(linearToSRGB(compositeRGB), 1.0);
}