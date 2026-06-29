layout (location = 0) out vec3 f_diffuse;
layout (location = 1) out vec3 f_normals;
layout (location = 2) out vec3 f_heightSpecEmissive;
uniform usampler2D u_terrain_data;
uniform sampler2DArray u_material_diffuse;
uniform sampler2DArray u_material_normals;
uniform sampler2DArray u_noise_textures;
in VS_OUT {
    vec2 pos; // world pos
} fs_in;

#define COMMON_BUFFER_BINDING 12
#define PROPERTIES_BUFFER_BINDING 10  // Uniform buffer binding point
#define MAX_NUM_MATERIALS 128        // Fixed number of meterials
#define NUM_TERRAIN_TYPES 32        // Fixed number of terrain types
#define NUM_BLOCK_TYPES 32          // Fixed number of block types
#define NOISE_TYPE_NONE 0x0F        // No material noise (15)
#define HEGHT_BRIGHT_MIN 0.85
#define HEGHT_BRIGHT_MAX 1.15
#define ELEVATION_PRECISION 64.0
#define HEIGHT_NOISE_PRECITION 32.0
#define ELEVATION_TRANSITION_FUNC 4.0

const float HEIGHT_PRECISION = (ELEVATION_PRECISION * 3.0 + HEIGHT_NOISE_PRECITION);
const float ELEVATION_DELTA = ELEVATION_PRECISION / 256.0;
const float HEIGHT_MAX = HEIGHT_PRECISION / 256.0;
const float NOISE_HEIGHT_MAX = HEIGHT_NOISE_PRECITION / 256.0;

// COMMOM BUFFER *******************************************************************************

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

struct Terrain {
    uint type;      // terrain type (0 - 31)
    uint foliage;   // unused atm. minor terrain foilage
    float height;
};

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

Terrain getTerrainClamped(ivec2 tileCoord, ivec2 mapSize) {
    ivec2 coord = ivec2(clamp(tileCoord.x, 0, mapSize.x - 1), clamp(tileCoord.y, 0, mapSize.y - 1));
    uint data = texelFetch(u_terrain_data,coord, 0).r;
    Terrain terrain;
    terrain.type = data & 0x1F;
    terrain.foliage = (data >> 7) & 0x01;
    terrain.height = float((data >> 5) & 0x03) * ELEVATION_DELTA;
    return terrain;
}

float smoothen(float t) { return t * t * (3.0 - 2.0 * t); }
vec2 smoothen(vec2 t) { return t * t * (3.0 - 2.0 * t); }
vec3 packNormal(vec3 n)   { return n * 0.5 + 0.5; }
vec3 unpackNormal(vec3 rgb) { return rgb * 2.0 - 1.0; }
vec3 scaleNormalVec(vec3 n, float scale) { n.xy *= scale; return normalize(n); }
vec2 pixalAntiAliasUV(vec2 uv, vec2 texSize) {
    vec2 texel = uv * texSize;
    texel = floor(texel) + min(fract(texel) / fwidth(texel),1.0) - 0.5;
    return texel / texSize;
}
/**
 * Restrictions:
 * - n must be clamped to [0.0, 1.0] before calling (function assumes this).
 * - contrast must be in [-0.9999, 0.9999] (never exactly ±1.0) to avoid division by zero.
 * - Function is branchless for performance; extreme values (±1.0) are approximated very closely.
 */
float contrastNoise(float n, float contrast) {
    float factor = (1.0 + contrast) / (1.0 - contrast);
    return (n - 0.5) * factor + 0.5;
}

float calculateHeightNoise(in NoiseParamStd140 param, vec2 noiseUV) {
    if(param.noiseType == NOISE_TYPE_NONE) return clamp(param.offset, 0.0, 1.0) * NOISE_HEIGHT_MAX;
    float n = texture(u_noise_textures, vec3(noiseUV,float(param.noiseType))).r;
    n = bool(param.negate & 1u) ? 1.0 - n : n;		        // 0. pre-negate noise
    n = n * param.scalar;									// 1. scale raw noise amlitude
    n = param.absolute == 1u ? abs(n * 2.0 - 1.0) : n;		// 2. ridged noise
    n = contrastNoise(n, param.contrast);					// 3. sharpen / flatten distribution
    n = pow(n, param.power);								// 4. non-linear reshaping (gamma-like)
    n = param.smoothen == 1u ? smoothen(n) : n;			    // 5. gentle blending/smoothing
    n = bool(param.negate & 2u) ? 1.0 - n : n;		        // 6. post-negate noise
    n = clamp(n + param.offset, 0.0, 1.0);				    // 7. raise / lower the result and clamp
    return n * NOISE_HEIGHT_MAX;                            // 8. adjust for precision
}

// alternative way to get position
vec2 getFragCoordWorldPos(vec2 fboSize) {
    // gl_FragCoord.xy is pixel center: range [0.5 .. viewportSizePx - 0.5]
    vec2 screenQuadUV = gl_FragCoord.xy / fboSize; //
    return mix(camera.frustumMin,camera.frustumMax,screenQuadUV);
}


void main() {

    vec3 fragmentColor = vec3(0.0,0.0,0.0);
    vec3 fragmentNormalRGB = vec3(0.5,0.5,1.0);
    float fragmentHeight = 0.0;
    float fragmentSpecular = 0.0;
    float fragmentEmissive = 0.0;

    if(fs_in.pos.x >= 0.0 && fs_in.pos.x < mapSize
    && fs_in.pos.y >= 0.0 && fs_in.pos.y < mapSize) {

        const ivec2 iMapSize = ivec2(mapSize);
        const vec2 materialTexSize = vec2(textureSize(u_material_diffuse, 0).xy);
        const vec2 noiseTexSize = vec2(textureSize(u_noise_textures, 0).xy);

        vec2 terrainPos = fs_in.pos + vec2(-0.5,-0.5); // offset for bi-linear blend
        ivec2 iTerrainCoord = ivec2(floor(terrainPos));
        vec2 localPosPixelated = floor(fract(terrainPos) * tileSize) / tileSize; // 0 -> 31 / 32

        vec2 texSamplePos = fs_in.pos * tileSize;
        //vec2 materialUV = pixalAntiAliasUV(texSamplePos / materialTexSize, materialTexSize);
        vec2 materialUV = texSamplePos / materialTexSize;
        vec2 noiseUV = 2.0 * texSamplePos / noiseTexSize; // nearest filtering

        // TERRAIN TILE SAMPLES
        Terrain terrain_bl = getTerrainClamped(iTerrainCoord, iMapSize);
        Terrain terrain_br = getTerrainClamped(iTerrainCoord + ivec2(1,0), iMapSize);
        Terrain terrain_tl = getTerrainClamped(iTerrainCoord + ivec2(0,1), iMapSize);
        Terrain terrain_tr = getTerrainClamped(iTerrainCoord + ivec2(1,1), iMapSize);
        MaterialStd140 material_bl = materials[terrainTypeToMaterialMap[terrain_bl.type]];
        MaterialStd140 material_br = materials[terrainTypeToMaterialMap[terrain_br.type]];
        MaterialStd140 material_tl = materials[terrainTypeToMaterialMap[terrain_tl.type]];
        MaterialStd140 material_tr = materials[terrainTypeToMaterialMap[terrain_tr.type]];
        vec3 color_bl, color_br, color_tl, color_tr, normal_bl, normal_br, normal_tl, normal_tr;

        // COLOR & NORMAL SAMPLES
        if(material_bl.texIndex < 0.0) {
            color_bl = material_bl.color.rgb;
            normal_bl = vec3(0.0,0.0,1.0);
        } else { vec3 layerUV = vec3(materialUV,material_bl.texIndex);
            color_bl = texture(u_material_diffuse, layerUV).rgb * material_bl.color.rgb;
            normal_bl = scaleNormalVec(unpackNormal(texture(u_material_normals, layerUV).rgb), material_bl.detail);
        } if(material_br.texIndex < 0.0) {
            color_br = material_br.color.rgb;
            normal_br = vec3(0.0,0.0,1.0);
        } else { vec3 layerUV = vec3(materialUV,material_br.texIndex);
            color_br = texture(u_material_diffuse, layerUV).rgb * material_br.color.rgb;
            normal_br = scaleNormalVec(unpackNormal(texture(u_material_normals, layerUV).rgb), material_br.detail);
        } if(material_tl.texIndex < 0.0) {
            color_tl = material_tl.color.rgb;
            normal_tl = vec3(0.0,0.0,1.0);
        } else { vec3 layerUV = vec3(materialUV,material_tl.texIndex);
            color_tl = texture(u_material_diffuse, layerUV).rgb * material_tl.color.rgb;
            normal_tl = scaleNormalVec(unpackNormal(texture(u_material_normals, layerUV).rgb), material_tl.detail);
        } if(material_tr.texIndex < 0.0) {
            color_tr = material_tr.color.rgb;
            normal_tr = vec3(0.0,0.0,1.0);
        } else { vec3 layerUV = vec3(materialUV,material_tr.texIndex);
            color_tr = texture(u_material_diffuse, layerUV).rgb * material_tr.color.rgb;
            normal_tr = scaleNormalVec(unpackNormal(texture(u_material_normals, layerUV).rgb), material_tr.detail);
        }

        //vec2 fi = smoothen(localPosPixelated);
        vec2 fi = smoothen((fract(terrainPos) * tileSize) / tileSize);
        float transitionNoise = texture(u_noise_textures,vec3(noiseUV * 1.25,ELEVATION_TRANSITION_FUNC)).x;
        float strength_x = 3.0 * fi.x * (1.0 - fi.x); // 0 - 1
        float strength_y = 3.0 * fi.y * (1.0 - fi.y); // 0 - 1
        float wx = fi.x + (transitionNoise - 0.5) * strength_x;
        float wy = fi.y + (transitionNoise - 0.5) * strength_y;

        {
            // HEIGHT


            float height_bl = terrain_bl.height + calculateHeightNoise(material_bl.noiseParam,noiseUV); // 0 to (224 / 256)
            float height_br = terrain_br.height + calculateHeightNoise(material_br.noiseParam,noiseUV); // 0 to (224 / 256)
            float height_tl = terrain_tl.height + calculateHeightNoise(material_tl.noiseParam,noiseUV); // 0 to (224 / 256)
            float height_tr = terrain_tr.height + calculateHeightNoise(material_tr.noiseParam,noiseUV); // 0 to (224 / 256)
            fragmentHeight = mix(mix(height_bl, height_br, wx), mix(height_tl, height_tr, wx), wy);
        }
        {
            // SPECULAR
            float spec_bl = material_bl.shininess;
            float spec_br = material_br.shininess;
            float spec_tl = material_tl.shininess;
            float spec_tr = material_tr.shininess;
            fragmentSpecular = mix(mix(spec_bl, spec_br, wx), mix(spec_tl, spec_tr, wx), wy);
        }
        // DETAIL NORMALS
        fragmentNormalRGB = packNormal(normalize(mix(mix(normal_bl, normal_br, wx), mix(normal_tl, normal_tr, wx), wy)));
        // COLOR
        float brightMod = mix(HEGHT_BRIGHT_MIN, HEGHT_BRIGHT_MAX, fragmentHeight / HEIGHT_MAX);
        fragmentColor = mix(mix(color_bl, color_br, wx), mix(color_tl, color_tr, wx), wy) * brightMod;


    }
    f_diffuse = fragmentColor;
    f_normals = fragmentNormalRGB;
    f_heightSpecEmissive = vec3(fragmentHeight,fragmentSpecular,fragmentEmissive);
}