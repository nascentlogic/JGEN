layout (location = 0) out vec3 f_color;

uniform sampler2D u_diffuse;
uniform sampler2D u_normals;
uniform sampler2D u_height_spec_emissive;
uniform sampler2D u_ambient_occlusion;
uniform vec3 u_dir_light_dir;

in VS_OUT {
    vec2 pos; // world pos
} fs_in;

// COMMOM BUFFER *******************************************************************************

#define COMMON_BUFFER_BINDING 12

struct Camera {
    mat4 combined;          // combined view projecton
    mat4 combinedInv;       // combined inverse. (NDC to world-space)
    vec2 frustumMin;        // bottom left corner in world coords
    vec2 frustumMax;        // top right corner in world coords
    vec2 frustumSize;       // size of the camera frustum
    vec2 position;          // camera center position (x,y) in world space
    float virtualZ;         // Camera z position in tiles
    float zoom;             // "zoomed in" zoom < 1.0, "zoomed ou" zoom > 1.0
    float depthMapHeight;   // height in tiles of the R8 precision depth buffer
    float unused00;         // unused
};

layout(std140, binding = COMMON_BUFFER_BINDING) uniform CommonBuffer {
    Camera camera;      // current camera
    vec2 tmrFboRes;     // tilemap renderer fbo resolution
    vec2 unused00;      // unused
    float mapSize;      // size of world map in tiles
    float tileSize;     // size of tile in pixels
    float runtime;      // real time (Engine runtime)
    float gametime;     // accumulated sum(dt * gameSpeed)
};

// TERRAIN PROPERTIES **************************************************************************

#define PROPERTIES_BUFFER_BINDING 10  // Uniform buffer binding point
#define MAX_NUM_MATERIALS 128        // Fixed number of meterials
#define NUM_TERRAIN_TYPES 32        // Fixed number of terrain types
#define NUM_BLOCK_TYPES 32          // Fixed number of block types

struct NoiseParamStd140 {
    // values are clamped on the CPU
    float scalar;   //  0 to 1
    float power;    //  0 to 4
    float offset;   // -1 to 1
    float contrast; // -0.9999 to 0.9999
    uint noiseType; //  0 to 15
    uint absolute;  //  0 || 1
    uint smoothen;  //  0 || 1
    uint negate;    //  0 || 1 || 2 || 3 (mask)
};

struct MaterialStd140 {
    NoiseParamStd140 noiseParam; // material noise parameters (affects terrain only)
    vec4 color;         // material tint
    float texIndex;     // material texture array layer (-1 == NO_TEXTURE)
    float detail;       // prominence of material texture normals
    float shininess;    // used for specular lighting later
    float padding;      // unused atm
};

struct WaterStd140 {
    vec4 colorSurface_specularIntensity;
    vec4 colorDepths_normalProminence;
    vec2 velocity;
    float distortionMax;
    float frequency;
    float waterLevelHigh;
    float waterLevelOffset;
    float shorelineDepth;
    float shoreLineIntensity;
};

struct GlobalIlluminationStd140 {
    vec4 lightColorDay_wrap;    // light color "day" and diffuseLightWrap
    vec4 lightColorNight_ambi;  // light color "night" and ambientLightStrength
};

layout(std140, binding = PROPERTIES_BUFFER_BINDING) uniform TerrainProperties {
    MaterialStd140[MAX_NUM_MATERIALS] materials;
    uint[NUM_TERRAIN_TYPES] terrainTypeToMaterialMap;
    uint[NUM_BLOCK_TYPES] blockTypeToMaterialMap;
    WaterStd140 water;
    GlobalIlluminationStd140 illumination;
};


// SPECULAR
#define SPEC_ENERGY_COEFFICIENT_MIN 1.0
#define SPEC_ENERGY_COEFFICIENT_MAX 1256.0 // 19.13274123
#define PI8 25.13274123
//
const vec3 AMBIENCE_DIR = normalize(vec3(0.0,0.0,0.99));
#define AMBIENCE_WRAP 0.334
#define PI_INV 0.31830989

float smoothen(float t) { return t * t * (3.0 - 2.0 * t); }

void main() {

    vec3 globalColorDay = illumination.lightColorDay_wrap.rgb;
    vec3 globalColorNight = illumination.lightColorNight_ambi.rgb;
    float globalLightWrap = illumination.lightColorDay_wrap.a; // clamp on CPU
    float globalAmbientStrength = illumination.lightColorNight_ambi.a; // clamp on CPU

    ivec2 fragCoord = ivec2(gl_FragCoord.xy);
    vec3 fragDiffuse = texelFetch(u_diffuse, fragCoord, 0).rgb;
    vec3 fragNormals = texelFetch(u_normals, fragCoord, 0).rgb * 2.0 - 1.0;
    float fragAmbientOcclusion = 1.0 - texelFetch(u_ambient_occlusion, fragCoord, 0).r;
    float fragHeight, fragSpecular, fragEmissive;

    {
        vec3 sceneHeightSpecEmissive = texelFetch(u_height_spec_emissive, fragCoord, 0).rgb;
        fragHeight = sceneHeightSpecEmissive.r * camera.depthMapHeight; // 4 tiles is default
        fragSpecular = sceneHeightSpecEmissive.g;
        fragEmissive = sceneHeightSpecEmissive.b;
    }

    vec3 fragPos = vec3(fs_in.pos,fragHeight);
    vec3 cameraPos = vec3(camera.position,camera.virtualZ);
    vec3 globalToLightDir = u_dir_light_dir; // normalize on CPU

    // Diffuse
    float NdotL = dot(fragNormals, globalToLightDir); // can be negative
    float globalDiff = clamp(NdotL + globalLightWrap, 0.0, globalLightWrap + 1.0) / (globalLightWrap + 1.0);

    // Specular
    // SPECULAR LIGHT ENERGY CONSERVATION:
    // https://www.rorydriscoll.com/2009/01/25/energy-conservation-in-games/
    // https://www.farbrausch.de/~fg/stuff/phong.pdf
    vec3 globalToEyeDir = normalize(cameraPos - fragPos);
    vec3 globalHalfwayDir = normalize(globalToLightDir + globalToEyeDir);
    float gloss = pow(fragSpecular,2.0);
    float fragShininess = mix(SPEC_ENERGY_COEFFICIENT_MIN,SPEC_ENERGY_COEFFICIENT_MAX,gloss);
    float specEnergyConservation = (fragShininess + 8.0) / PI8;
    float globalSpec = specEnergyConservation * pow(max(0.0,dot(fragNormals, globalHalfwayDir)), fragShininess);

    // AMBIENCE + NIGHT / DAY
    //
    float globalAmbiDot = clamp(dot(AMBIENCE_DIR, fragNormals) + AMBIENCE_WRAP, 0.0, AMBIENCE_WRAP + 1.0) / (AMBIENCE_WRAP + 1.0);
    // day night ratio: zenith (1.0) == full day, horizon (0.0) == full night (might alter later)
    float dayNightRatio = pow(clamp(dot(globalToLightDir,vec3(0.0,0.0,1.0)),0.0,1.0),PI_INV); // 1.0 == day
    vec3 nightAmbienceColor = globalColorNight * globalAmbiDot;
    vec3 dayAmbienceColor = globalColorDay * globalAmbientStrength;
    vec3 globalAmbienceColor = dayAmbienceColor * dayNightRatio + nightAmbienceColor * (1.0 - dayNightRatio);
    /*
        vec3 secondary_ambient_dir = normalize(vec3(0.0,0.999,0.0));
        float wrap = 0.33;
        float dott = clamp(dot(secondary_ambient_dir,fragment_normal) + wrap, 0.0, wrap + 1.0) / (wrap + 1.0);
        float ratio = 1.0 - clamp(dot(to_light_direction,vec3(0.0,0.0,1.0)),0.0,1.0); // * influence
        float intensity = 1.0;
        vec3 secondary_ambient_color = SKY_COLOR * intensity * dott; // intensity
        vec3 light_a = (light.color * light.ambience * (1.0 - ratio) + secondary_ambient_color * ratio);
        diff = diff * shadow * (1.0 - ratio);

    */

    float fs = fragSpecular;

    vec3 f_diffuse  = fragDiffuse * PI_INV;
    vec3 f_specular = vec3(globalSpec * fs * (0.5 + (fragDiffuse.r + fragDiffuse.g + fragDiffuse.b / 3.0)) / 1.5);
    vec3 f_reflectance = (f_diffuse + f_specular) * globalColorDay * NdotL;

    f_color = f_reflectance + globalAmbienceColor * fragDiffuse * fragAmbientOcclusion;


    vec3 A = fragDiffuse * globalAmbienceColor * fragAmbientOcclusion;
    vec3 D = fragDiffuse * PI_INV * globalColorDay * globalDiff;
    vec3 S = globalColorDay * globalSpec * fs;
    vec3 E = fragDiffuse * fragEmissive;

    // vec3 A = fragDiffuse * globalAmbienceColor * fragAmbientOcclusion;
    // vec3 D = fragDiffuse * PI_INV * globalColorDay * globalDiff * (1.0 - fs);
    // vec3 S = fragDiffuse * globalColorDay * globalSpec * (fs);
    // vec3 E = fragDiffuse * fragEmissive;

    // f_color = max(A + D + S, E);
    // f_color = A + D + S + E;



}