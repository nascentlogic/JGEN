layout (location=0) in vec2 a_pos; // (-1,1)

#define COMMON_BUFFER_BINDING 12

out VS_OUT {
    vec2 uv;
    vec2 pos;
} vs_out;

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

void main() {
    gl_Position = vec4(a_pos,-1.0,1.0);
    vec4 pos = camera.combinedInv * vec4(a_pos,0.0,1.0);
    vs_out.pos = pos.xy;
    vs_out.uv = (a_pos + 1.0) * 0.5;
}