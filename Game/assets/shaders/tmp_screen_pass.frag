layout (location = 0) out vec4 f_dst;

#define OUTPUT_COLOR 0
#define OUTPUT_NORMALS 1
#define OUTPUT_HEIGHT 2
#define OUTPUT_SPECULAR 3
#define OUTPUT_EMISSIVE 4
#define OUTPUT_AMBIENT_OCCLUSION 5

uniform sampler2D u_src;
uniform float u_gammaCorrection; // 1.0 / gamma
uniform int u_outputMode;


void main() {
    if(u_outputMode == OUTPUT_COLOR) {
        vec3 color = texelFetch(u_src,ivec2(gl_FragCoord.xy),0).rgb;
        f_dst = vec4(pow(color,vec3(u_gammaCorrection)),1.0);
        return;
    }
    if(u_outputMode == OUTPUT_NORMALS) {
        vec3 color = texelFetch(u_src,ivec2(gl_FragCoord.xy),0).rgb;
        f_dst = vec4(color,1.0);
        return;
    }
    if(u_outputMode == OUTPUT_HEIGHT) {
        vec3 color = texelFetch(u_src,ivec2(gl_FragCoord.xy),0).rrr;
        f_dst = vec4(color,1.0);
        return;
    }
    if(u_outputMode == OUTPUT_SPECULAR) {
        vec3 color = texelFetch(u_src,ivec2(gl_FragCoord.xy),0).ggg;
        f_dst = vec4(color,1.0);
        return;
    }
    if(u_outputMode == OUTPUT_EMISSIVE) {
        vec3 color = texelFetch(u_src,ivec2(gl_FragCoord.xy),0).bbb;
        f_dst = vec4(color,1.0);
        return;
    }
    if(u_outputMode == OUTPUT_AMBIENT_OCCLUSION) {
        float ao = 1.0 - texelFetch(u_src,ivec2(gl_FragCoord.xy),0).r;
        f_dst = vec4(ao,ao,ao,1.0);
        return;
    }
    f_dst = vec4(1.0,0.0,0.0,1.0);
}