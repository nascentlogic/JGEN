layout (location = 0) out float f_ambient;   // R8

in VS_OUT {
    vec2 pos;
} fs_in;

uniform sampler2D u_height_map; // scene height buffer (height / spec / emissive)
uniform sampler2D u_normal_map; // scene normal buffer
uniform sampler2DArray u_noise_textures;

#define PI2 6.28318531
#define NORMAL_MOD_INFLUENCE 0.25
#define COMMON_BUFFER_BINDING 12
#define HEIGHT_BUFFER_HEIGHT_TILES 4.0

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

/*
    Instead of generating the kernel (of samples) on the CPU, I have
    precalculated some values based on:https://en.wikipedia.org/wiki/Halton_sequence
    and hardcoded them in. They work very well. Having them be const instead of uniform
    should help performance. (could try with 16 instead)
*/
const vec3 occlusion_normals[32] = vec3[](
vec3(-0.353553, 0.612372, 0.707107),
vec3(-0.250000, -0.433013, 0.866025),
vec3(0.663414, 0.556670, 0.500000),
vec3(-0.332232, 0.120922, 0.935414),
vec3(0.137281, -0.778559, 0.612372),
vec3(0.106337, 0.603069, 0.790569),
vec3(-0.879002, -0.319931, 0.353553),
vec3(0.191511, -0.160697, 0.968246),
vec3(0.729784, 0.172962, 0.661438),
vec3(-0.383621, 0.406614, 0.829156),
vec3(-0.258521, -0.863520, 0.433013),
vec3(0.258577, 0.347330, 0.901388),
vec3(-0.823550, 0.096259, 0.559017),
vec3(0.261982, -0.607343, 0.750000),
vec3(-0.056298, 0.966608, 0.250000),
vec3(-0.147695, -0.097140, 0.984251),
vec3(0.651341, -0.327116, 0.684653),
vec3(0.473920, 0.238012, 0.847791),
vec3(-0.738474, 0.485702, 0.467707),
vec3(-0.022984, -0.394616, 0.918559),
vec3(0.320861, 0.743840, 0.586302),
vec3(-0.633068, -0.073995, 0.770552),
vec3(0.568478, -0.763599, 0.306186),
vec3(-0.087815, 0.293323, 0.951972),
vec3(-0.528785, -0.560479, 0.637377),
vec3(0.570498, -0.135211, 0.810093),
vec3(0.915796, 0.071181, 0.395285),
vec3(-0.264538, 0.385706, 0.883883),
vec3(-0.365725, -0.764850, 0.530330),
vec3(0.488794, 0.479406, 0.728869),
vec3(-0.948199, 0.263949, 0.176777),
vec3(0.031180, -0.121049, 0.992157));

const float occlusion_lengths[32] = float[](
0.014286, 0.028839, 0.044463, 0.061959,
0.082132, 0.105783, 0.002729, 0.023819,
0.048960, 0.078957, 0.114611, 0.156725,
0.206101, 0.010542, 0.052082, 0.101459,
0.159475, 0.226932, 0.304634, 0.393383,
0.029058, 0.104695, 0.191954, 0.291635,
0.404542, 0.531479, 0.673246, 0.063896,
0.187277, 0.326063, 0.481057, 0.653061);

float smoothen(float t) { return t * t * (3.0 - 2.0 * t); }
vec3 rotateNormalByAngle(vec3 normal, float sinR, float cosR) {
    vec2 xy = vec2(
    normal.x * cosR - normal.y * sinR,
    normal.x * sinR - normal.y * cosR);
    return vec3(xy,normal.z);
}

void main() {

    if(fs_in.pos.x >= 0.0 && fs_in.pos.x < mapSize
    && fs_in.pos.y >= 0.0 && fs_in.pos.y < mapSize) {

        float occlusion = 0.0;

        vec2 pixelPosCentered = floor(fs_in.pos * tileSize) + 0.5;
        vec2 fragWorldPos2D = pixelPosCentered / tileSize;
        // vec2 fboUV = (fragWorldPos2D - camera.frustumMin) / camera.frustumSize;

        vec2 noiseUV = pixelPosCentered / vec2(textureSize(u_noise_textures, 0).xy);
        float rotAngle = PI2 * texture(u_noise_textures,vec3(noiseUV,0.0)).r;
        float sinR = sin(rotAngle);
        float cosR = cos(rotAngle);

        ivec2 fragCoord = ivec2(gl_FragCoord.xy);
        vec3 fragNormal = texelFetch(u_normal_map,fragCoord,0).rgb * 2.0 - 1.0;
        float fragHeight = texelFetch(u_height_map,fragCoord,0).r * camera.depthMapHeight;

        float normalDot = dot(vec3(0.0,0.0,1.0), fragNormal);
        float normalMod = (1.0 - normalDot) * NORMAL_MOD_INFLUENCE;

        vec3 fragWorldPos3D = vec3(fragWorldPos2D,fragHeight);

        for(int i = 0; i < 32; i++) {
            vec3 hemisphereNormal = rotateNormalByAngle(occlusion_normals[i].xyz, sinR, cosR); // lenght == 1.0
            vec3 sampleDirection = normalize(fragNormal + hemisphereNormal);
            float lengthOffset = occlusion_lengths[i] * 3.0; // 3.5 is the max length of kernel vec in tiles
            vec3 sampleOffsetVec = sampleDirection * lengthOffset;
            vec3 samplePosWorld3D = fragWorldPos3D + sampleOffsetVec;
            vec2 sampleUV = (samplePosWorld3D.xy - camera.frustumMin) / camera.frustumSize;
            float sampleHeightTiles = texture(u_height_map, sampleUV).r * camera.depthMapHeight;
            float sampleVecHeightTiles = samplePosWorld3D.z;
            // zDist is the signed distance from the "terrain" at samplePosWorld3D at xy and
            // the sudo random height (z-component) of the vector at the same position.
            // If the distance is negative you would add to the output occlusion.
            // If the distance is positive you are "in the clear" and would not add to occlusion.
            float zDist = sampleVecHeightTiles - sampleHeightTiles;
            // Now. Since this is a 2D SSAO shader there are certain hacks I use to make the result
            // look good for the purposes of this Engine. That's why I use customized hard coded
            // variables. For a regular 3D SSAO shader example see. https://learnopengl.com/Advanced-Lighting/SSAO
            // Point is, do not be confused.
            float zDistModified = zDist - 1.0;
            if(zDistModified <= 0.0) {
                occlusion -= zDistModified / 4.5; // 3.5 + 1.0
            } else {
                occlusion -= zDistModified / 2.0; // 3.5 - 1.0
            }
        }
        occlusion = 1.0 - occlusion / 32.0;
        occlusion = 1.0 - occlusion * occlusion;
        f_ambient = max(occlusion, normalMod);
    } else {
        f_ambient = 0.0;
    }

}