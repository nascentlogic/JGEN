layout(location = 0) out vec4 fCombined;
uniform sampler2D uTextures[4]; // Array of up to 4 input textures
uniform int uChannels[4]; // Number of channels each texture contributes (e.g. [1, 3, 0, 0])

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    float resultChannels[4] = float[4](0.0, 0.0, 0.0, 1.0);
    int currentOutChannel = 0;
    for (int i = 0; i < 4; i++) {
        int count = uChannels[i];
        if (count <= 0) continue;
        vec4 sampledColor = texelFetch(uTextures[i], coord, 0);
        for (int c = 0; c < count; c++) {
            if (currentOutChannel < 4) {
                resultChannels[currentOutChannel] = sampledColor[c];
                currentOutChannel++;
            }
        }
    }
    fCombined = vec4(
    resultChannels[0],
    resultChannels[1],
    resultChannels[2],
    resultChannels[3]
    );
}