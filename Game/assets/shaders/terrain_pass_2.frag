layout (location = 0) out vec3 f_diffuse;
layout (location = 1) out vec3 f_normals;
layout (location = 2) out vec3 f_heightSpecEmissive;
uniform sampler2DArray u_material_diffuse;
uniform sampler2DArray u_material_normals;
uniform sampler2DArray u_block_atlas;   // 48 block sprites (normals and height)
uniform usampler2D u_block_data;        // block data uploaded from the cpu
uniform usampler2D u_block_mask;        // 8 bit masks to identify neighboring blocks in 8 ordinal directions
uniform sampler2D u_diffuse;            // RGB8
uniform sampler2D u_normals;            // RGB8 (terrain texture normals)
uniform sampler2D u_heightSpecEmissive; // RGB8

// I made a monstrosity. Good luck.

in VS_OUT {
    vec2 uv;  // 0 - 1
    vec2 pos; // world pos
} fs_in;

#define PROPERTIES_BUFFER_BINDING 10
#define COMMON_BUFFER_BINDING 12

#define NO_BLOCK 0x1F           // block type: 0001 1111 = no block
#define NUM_MATERIALS 128        // Fixed number of meterials
#define NUM_TERRAIN_TYPES 32    // Fixed number of terrain types
#define NUM_BLOCK_TYPES 32      // Fixed number of block types

#define BLOCK_WALL_SHADOW_PRECISION 48.0 // higher value produse "broader" shadows where terrain meet wall
#define BLOCK_WALL_BASE 0.0     // f_height is R8 (256 bit precision) walls start at (0 / 256)
#define BLOCK_WALL_TOP 128.0    // f_height is R8 (256 bit precision) "block base" starts where walls end
#define BLOCK_TOP 192.0         // f_height is R8 (256 bit precision) "block top" highest point of block
const float BLOCK_WALL_PRECISION = BLOCK_WALL_TOP - BLOCK_WALL_BASE;

#define BLOCK_TOP_HEIGHT_BRIGHT_MIN 0.8     // Blocks are darkened the lower it's height
#define BLOCK_WALL_HEIGHT_BRIGHT_MIN 0.6    // Walls are extensions of blocks downward. The base is the darkest.

#define HIDE_BLOCKS false
#define HIDE_BLOCK_WALLS true
#define REORIENTED_NORMAL_MAPPING true

// Precalculated normals for the lower parts of a block ("wall").
const vec3 BLOCK_WALL_NORMAL_1 = vec3(0.0, -0.8944, 0.4472); // this is the one that should be used for billboards etc.
const vec3 BLOCK_WALL_NORMAL_2 = vec3(-0.004, -0.858, 0.513);
const vec3 BLOCK_WALL_NORMAL_RIGHT = vec3(0.535, -0.542, 0.648);
const vec3 BLOCK_WALL_NORMAL_LEFT = vec3(-0.541, -0.541, 0.644);
// A precalculated array that maps adjacent tile mask to block sprite atlas index / layer
const uint[256] BLOCK_INDEX = uint[256](
47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
13, 13, 14, 14, 13, 13, 14, 14, 15, 15, 16, 17, 15, 15, 16, 17,
18, 18, 19, 19, 18, 18, 20, 20, 21, 21, 22, 23, 21, 21, 24, 25,
13, 13, 14, 14, 13, 13, 14, 14, 26, 26, 27, 28, 26, 26, 27, 28,
18, 18, 19, 19, 18, 18, 20, 20, 29, 29, 30, 31, 29, 29, 32, 33,
47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
47, 47, 1 , 1 , 47, 47, 1 , 1 , 2 , 2 , 3 , 4 , 2 , 2 , 3 , 4 ,
5 , 5 , 6 , 6 , 5 , 5 , 7 , 7 , 8 , 8 , 9 , 10, 8 , 8 , 11, 12,
13, 13, 14, 14, 13, 13, 14, 14, 15, 15, 16, 17, 15, 15, 16, 17,
34, 34, 35, 35, 34, 34, 36, 36, 37, 37, 38, 39, 37, 37, 40, 41,
13, 13, 14, 14, 13, 13, 14, 14, 26, 26, 27, 28, 26, 26, 27, 28,
34, 34, 35, 35, 34, 34, 36, 36, 42, 42, 43, 44, 42, 42, 45, 46);

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

// PROPERTIES **********************************************************************************

struct Block {
    uint type;      // block type (0 - 31) 31 == NO_BLOCK
    uint damage;    // unused atm. damage sustained (0 - 3)
    uint res;  // unused atm. (0 == no resource)
    uint sprite;    // atlas sprite layer (0 - 47)
    uint mask;      // 8 neighbor mask (block or no block)
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
    MaterialStd140[NUM_MATERIALS] materials;
    uint[NUM_TERRAIN_TYPES] terrainTypeToMaterialMap;
    uint[NUM_BLOCK_TYPES] blockTypeToMaterialMap;
    WaterStd140 water;
    GlobalIlluminationStd140 illumination;
};

// *********************************************************************************************

float smoothen(float t) { return t * t * (3.0 - 2.0 * t); }
vec2 smoothen(vec2 t) { return t * t * (3.0 - 2.0 * t); }
vec3 packNormal(vec3 n)   { return n * 0.5 + 0.5; }
vec3 unpackNormal(vec3 n) { return n * 2.0 - 1.0; }
vec3 scaleNormal(vec3 n, float scale) { n.xy *= scale; return normalize(n); }
// Unpack Derive Normalize
vec3 normalBlendUDN(vec3 surface, vec3 detail) {
    return normalize(vec3(surface.xy + detail.xy,surface.z)); }
// Reoriented Normal Mapping
vec3 normalBlendRNM(vec3 surface, vec3 detail) {
    vec3 t = surface + vec3(0.0, 0.0, 1.0);
    vec3 u = detail * vec3(-1.0, -1.0, 1.0);
    return normalize(t * dot(t, u) / t.z - u);
} float fetchHeight(ivec2 texel, ivec2 texture_size) {
    if(texel.x >= texture_size.x) texel.x = texture_size.x - 1;
    if(texel.y >= texture_size.y) texel.y = texture_size.y - 1;
    if(texel.x < 0) texel.x = 0;
    if(texel.y < 0) texel.y = 0;
    return texelFetch(u_heightSpecEmissive,texel,0).r;
} vec2 pixalAntiAliasUV(vec2 uv, vec2 texSize) {
    vec2 texel = uv * texSize;
    texel = floor(texel) + min(fract(texel) / fwidth(texel),1.0) - 0.5;
    return texel / texSize;
}
// *********************************************************************************************

MaterialStd140 getBlockMaterial(uint blockType) {
    return materials[blockTypeToMaterialMap[blockType]];
} Block getBlock(ivec2 tileCoord) {
    Block block;
    uint data = texelFetch(u_block_data, tileCoord, 0).r;
    uint mask = texelFetch(u_block_mask, tileCoord, 0).r;
    block.mask = mask;
    block.type = data & 0x1F;
    block.damage = (data >> 5) & 0x03;
    block.res = (data >> 7) & 0x01;
    block.sprite = BLOCK_INDEX[mask];
    return block;
}

vec2 getFragCoordWorldPos(vec2 fboSize) {
    // gl_FragCoord.xy is pixel center: range [0.5 .. viewportSizePx - 0.5]
    vec2 screenQuadUV = gl_FragCoord.xy / fboSize; //
    return mix(camera.frustumMin,camera.frustumMax,screenQuadUV);
}




void main() {

    // out of bounds (default)
    f_diffuse = vec3(0.0,0.0,0.0);
    f_normals = vec3(0.5,0.5,1.0);
    f_heightSpecEmissive = vec3(0.0,0.0,0.0);

    if(fs_in.pos.x >= 0.0 && fs_in.pos.x < mapSize
    && fs_in.pos.y >= 0.0 && fs_in.pos.y < mapSize) {

        const ivec2 iTexSize = textureSize(u_heightSpecEmissive, 0);
        const vec2 texSize = vec2(iTexSize);

        vec2 worldPos = fs_in.pos;
        vec2 worldPixelPos = worldPos * tileSize;
        ivec2 iTileCoord = ivec2(worldPos);
        Block block = getBlock(iTileCoord);


        if(block.type == NO_BLOCK || HIDE_BLOCKS) {

            ivec2 iFragCoord = ivec2(gl_FragCoord.xy);
            vec3 terrainDiffuse = texelFetch(u_diffuse, iFragCoord, 0).rgb;
            vec3 terrainNormal;
            vec3 terrainHeightSpecEmissive = texelFetch(u_heightSpecEmissive, iFragCoord, 0).rgb;

            /*
                Height sample precision:
                The incoming height texture sample precision is x2:
                height sample = 64 * elevation + 32 * height noise = (0 to 224) / 256
                actual height = sample * 0.5 = (0 to 112) / 256
            */

            float terrainHeight = terrainHeightSpecEmissive.r * 0.5; // (0 to 224) / 256 -> (0 to 112) / 256
            float terrainSpecular = terrainHeightSpecEmissive.g;
            float terrainEmissive = terrainHeightSpecEmissive.b;

            {

                // Calculate Terrain Normals

                const bool zoomedOut = true; // bool(camera.zoom >= 1.0);
                const float depthBufferHeightTiles = camera.depthMapHeight * 0.5; // 2.0


                vec3 terrainDetailNormal = unpackNormal(texelFetch(u_normals, iFragCoord, 0).rgb);
                vec3 terrainSurfaceNormal;

                if(zoomedOut) {
                    const float upperLim = tileSize * mapSize - camera.zoom;
                    const float lowerLim = camera.zoom;
                    const float heightToWorld = depthBufferHeightTiles * tileSize * 0.5 / camera.zoom; // height downscaling applied here (0.5)
                    float pixelR = worldPixelPos.x + 1.0;
                    float pixelL = worldPixelPos.x - 1.0;
                    float pixelU = worldPixelPos.y + 1.0;
                    float pixelD = worldPixelPos.y - 1.0;
                    float hc = terrainHeightSpecEmissive.r; // terrain height sample raw
                    float hr = (pixelR < upperLim) ? fetchHeight(iFragCoord + ivec2( 1, 0), iTexSize) : hc;
                    float hl = (pixelL > lowerLim) ? fetchHeight(iFragCoord + ivec2(-1, 0), iTexSize) : hc;
                    float hu = (pixelU < upperLim) ? fetchHeight(iFragCoord + ivec2( 0, 1), iTexSize) : hc;
                    float hd = (pixelD > lowerLim) ? fetchHeight(iFragCoord + ivec2( 0,-1), iTexSize) : hc;
                    terrainSurfaceNormal = normalize(vec3((hl - hr) * heightToWorld,(hd - hu) * heightToWorld,2.0)); // todo: was 2.0
                } else{
                    const float heightToWorld = depthBufferHeightTiles * tileSize * 0.5; // height downscaling applied here (0.5)
                    const float gamePixelStepWorld = 1.0 / tileSize;
                    const float eps = 0.5 / tileSize;
                    const vec2 mapMin = vec2(eps);
                    const vec2 mapMax = vec2(mapSize - eps);
                    vec2 centerWorld = (floor(worldPixelPos) + 0.5) / tileSize;
                    vec2 worldR = centerWorld + vec2(gamePixelStepWorld, 0.0);
                    vec2 worldL = centerWorld - vec2(gamePixelStepWorld, 0.0);
                    vec2 worldU = centerWorld + vec2(0.0, gamePixelStepWorld);
                    vec2 worldD = centerWorld - vec2(0.0, gamePixelStepWorld);
                    vec2 deltaR = clamp(worldR, mapMin, mapMax) - worldPos;
                    vec2 deltaL = clamp(worldL, mapMin, mapMax) - worldPos;
                    vec2 deltaU = clamp(worldU, mapMin, mapMax) - worldPos;
                    vec2 deltaD = clamp(worldD, mapMin, mapMax) - worldPos;
                    const vec2 mul = tileSize / texSize / camera.zoom;
                    float hr = texture(u_heightSpecEmissive, fs_in.uv + deltaR * mul).r;
                    float hl = texture(u_heightSpecEmissive, fs_in.uv + deltaL * mul).r;
                    float hu = texture(u_heightSpecEmissive, fs_in.uv + deltaU * mul).r;
                    float hd = texture(u_heightSpecEmissive, fs_in.uv + deltaD * mul).r;
                    terrainSurfaceNormal = normalize(vec3((hl - hr) * heightToWorld,(hd - hu) * heightToWorld,3.0)); // todo: was 2.0
                }

                if(REORIENTED_NORMAL_MAPPING) { terrainNormal = normalBlendRNM(terrainSurfaceNormal, terrainDetailNormal); }
                else { terrainNormal = normalBlendUDN(terrainSurfaceNormal, terrainDetailNormal); }


                f_diffuse = terrainDiffuse;
                f_normals = packNormal(terrainNormal);
                f_heightSpecEmissive = vec3(terrainHeight,terrainSpecular,terrainEmissive);

                // Early exit condition
                if(HIDE_BLOCKS || HIDE_BLOCK_WALLS || iTileCoord.y >= int(mapSize - 1.0)) { return; }

                Block blockUp = getBlock(iTileCoord + ivec2(0,1));
                if(blockUp.type != NO_BLOCK) {

                    // ATP. The tile above is a Block amd fragment is possibly part of the wall

                    // calculation comments assume tileSize == 32. (example comments for understanding)
                    vec2 localPos = fract(worldPos); // 0 to 0.999...
                    vec2 localPixelCoord = vec2(floor(localPos * tileSize)); // 0 to 31
                    const float wallRangePixels = float(int((tileSize / 8.0) * 5.0)); // 20 (max height in pixels)
                    const float wallStartPixels = tileSize - wallRangePixels;  // 12
                    float wallPixelY = localPixelCoord.y - wallStartPixels; // -12 to 19 (0 = wall start)

                    // BLOCK_WALL_BASE      0.0     Wall start height (base)
                    // BLOCK_WALL_TOP       128.0   Wall stops here and block begins
                    // BLOCK_TOP            192.0   Top of block
                    // BLOCK_WALL_PRECISION 128.0   Wall max height (range)

                    float yFactor = wallPixelY / wallRangePixels; // -0.6 to 0.95
                    float wallHeight256 = BLOCK_WALL_BASE + BLOCK_WALL_PRECISION * yFactor; // -76.8 to 121.6
                    float terrainHeight256 = floor(terrainHeight * 256.0); // 0 to (96 + 16 = 112)
                    float heightDiff = wallHeight256 - terrainHeight256;
                    // distance mod grows the further the wall is from the terrain. 0 to 12 -> 0.0 to 1.0
                    float shadowDistMod = smoothen(min(abs(heightDiff), BLOCK_WALL_SHADOW_PRECISION) / BLOCK_WALL_SHADOW_PRECISION);

                    if(wallPixelY >= 0.0 && wallHeight256 >= terrainHeight256) {

                        // ATP. The current fragment is inside a pixel that makes up a block wall.
                        // E.g. The fragment is above the terrain and a wall is present

                        MaterialStd140 material = getBlockMaterial(blockUp.type);
                        const vec2 materialTexSize = vec2(textureSize(u_material_diffuse, 0).xy);
                        vec2 materialUV = pixalAntiAliasUV(worldPixelPos / materialTexSize, materialTexSize); // linear

                        vec3 materialColor = material.texIndex < 0.0 ? material.color.rgb :
                        texture(u_material_diffuse,vec3(materialUV, material.texIndex)).rgb * material.color.rgb;
                        vec3 materialNormal;
                        if(material.texIndex < 0.0) {
                            materialNormal = vec3(0.0,0.0,1.0);
                        } else {
                            materialNormal = texture(u_material_normals,vec3(materialUV,material.texIndex)).rgb;
                            materialNormal = scaleNormal(unpackNormal(materialNormal),material.detail);
                        }

                        vec3 wallColor = materialColor;
                        vec3 wallNormal = BLOCK_WALL_NORMAL_1;

                        if(localPixelCoord.x == 0) {
                            if((0x09 & block.mask) == 0x00) {
                                wallNormal = BLOCK_WALL_NORMAL_LEFT;
                            }
                        } else if(localPixelCoord.x == int(tileSize - 1.0)) {
                            if((0x14 & block.mask) == 0x00) {
                                wallNormal = BLOCK_WALL_NORMAL_RIGHT;
                            }
                        }

                        // Darkening wall based on distance from top (colorMod). ATP. yFactor >= 0.0
                        const float brightStart = BLOCK_WALL_HEIGHT_BRIGHT_MIN;
                        const float brightPotential = BLOCK_TOP_HEIGHT_BRIGHT_MIN - brightStart;
                        float heightColorMod = brightStart + brightPotential * yFactor * yFactor;
                        shadowDistMod = mix(brightStart, 1.0, shadowDistMod);
                        float colorMod = min(heightColorMod,shadowDistMod);


                        if(heightDiff <= 4.0) {
                            // if height diff from terrain to wall is very small,
                            // we blend the pixel for a somewhat smoother transition
                            // ATP. wallColor is the wall material color
                            wallColor = mix(terrainDiffuse,wallColor ,0.6667);
                            wallNormal = normalize(mix(terrainNormal,wallNormal,0.6667));
                        }

                        f_diffuse = wallColor * colorMod;
                        if(REORIENTED_NORMAL_MAPPING) {
                            f_normals = packNormal(normalBlendRNM(wallNormal,materialNormal));
                        } else { f_normals = packNormal(normalBlendUDN(wallNormal,materialNormal)); }
                        f_heightSpecEmissive.r = wallHeight256 / 256.0; // height
                        f_heightSpecEmissive.g = material.shininess;    // specular
                        // f_heightSpecEmissive.b = 0.0;                // emissive (implement later)

                    } else {

                        // ATP. The current fragment is either bellow the terrain or no wall is present
                        // f_diffuse is terrain color
                        f_diffuse *= mix(BLOCK_WALL_HEIGHT_BRIGHT_MIN, 1.0, shadowDistMod);
                    }

                }

            }

        } else {

            // ATP. block exists and is visible
            // BLOCK *****************************************
            MaterialStd140 material = getBlockMaterial(block.type);
            const vec2 blockAtlasTexSize = vec2(textureSize(u_block_atlas, 0).rg);
            const vec2 materialTexSize = vec2(textureSize(u_material_diffuse, 0).xy);
            vec2 materialUV = pixalAntiAliasUV(worldPixelPos / materialTexSize, materialTexSize); // linear
            //vec2 atlasUV = mod(worldPixelPos, blockAtlasTexSize) / blockAtlasTexSize;
            vec4 atlasSample = texture(u_block_atlas,vec3(worldPos,float(block.sprite))); // nearest

            // Height / Specular / Emissive
            f_heightSpecEmissive.r = mix(BLOCK_WALL_TOP, BLOCK_TOP /*BLOCK_TOP - 1*/ , atlasSample.w) / 256.0;
            f_heightSpecEmissive.g = material.shininess;
            f_heightSpecEmissive.b = 0.0;

            // Normals
            vec3 blockNormal = unpackNormal(atlasSample.rgb);
            vec3 materialNormal;
            if(material.texIndex < 0.0) {
                materialNormal = vec3(0.0,0.0,1.0);
            } else {
                materialNormal = texture(u_material_normals,vec3(materialUV,material.texIndex)).rgb;
                materialNormal = scaleNormal(unpackNormal(materialNormal),material.detail);
            }

            if(REORIENTED_NORMAL_MAPPING) {
                f_normals = packNormal(normalBlendRNM(blockNormal,materialNormal));
            } else { f_normals = packNormal(normalBlendUDN(blockNormal,materialNormal)); }

            // Color
            const float brightStart = BLOCK_TOP_HEIGHT_BRIGHT_MIN;
            const float brightPotential = 1.0 - BLOCK_TOP_HEIGHT_BRIGHT_MIN;
            vec3 materialColor = material.texIndex < 0.0 ? material.color.rgb :
            texture(u_material_diffuse,vec3(materialUV, material.texIndex)).rgb * material.color.rgb;
            float heightColorMod = brightStart + atlasSample.w * brightPotential;
            f_diffuse = materialColor * heightColorMod;
        }

    }
}