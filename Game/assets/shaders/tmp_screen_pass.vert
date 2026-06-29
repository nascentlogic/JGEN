layout (location = 0) in vec2 a_pos; // (-1,1)

void main() {
    gl_Position = vec4(a_pos,-1.0,1.0);
}