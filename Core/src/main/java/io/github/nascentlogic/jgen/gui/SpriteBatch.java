package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.gfx.Shader;
import io.github.nascentlogic.jgen.gfx.ShaderProgram;
import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.io.Disk;

import java.nio.FloatBuffer;

/**
 * F.Dahl, 9/19/2026
 */
public class SpriteBatch {
    /* ----------------------------------------
     * vertex: | pos | uv | color | id | data |
     * ----------------------------------------
     * size:   | 2   | 2  | 1     | 1  | 1    |
     * ----------------------------------------*/
    public static final String SPRITE_PROGRAM_NAME = "jgen-gui-sprite";
    public static final String SPRITE_PROGRAM_DIR = "jgen/gui/glsl";
    public static final int VERTEX_SIZE_FLOAT = 7;
    public static final int SPRITE_SIZE_FLOAT = VERTEX_SIZE_FLOAT * 4;
    public static final int TEXTURE_SLOTS = 8; // same as in the shader

    private final Texture[] textureSlots = new Texture[TEXTURE_SLOTS];
    private int nextSlot;
    private int prevSlot;

    private ShaderProgram program;
    private FloatBuffer vertices;
    private int vao;
    private int vbo;
    private int ebo; // 0,1,2,2,3,0 ...
    private int count;
    private int limit;
    private int countAccum;
    private int drawCalls;

    SpriteBatch(int width, int height) throws Exception {
        ShaderProgram program = ShaderProgram.getProgramByName(SPRITE_PROGRAM_NAME); // just in case
        if (program == null) {
            Shader shader = Disk.resourceShader(SPRITE_PROGRAM_NAME, SPRITE_PROGRAM_DIR);
            program = new ShaderProgram(shader);
        } this.program = program;
        program.use();
        ShaderProgram.setUniformF("uResolution", width, height);
        int[] samplers = new int[TEXTURE_SLOTS];
        for (int i = 0; i < samplers.length; i++) {
            samplers[i] = i + FontLibrary.MAX_FONT_SLOTS; // 5, 6, 7, ... 12
        } ShaderProgram.setUniformI("uTextures",samplers);
        ShaderProgram.useNone();



    }


}
