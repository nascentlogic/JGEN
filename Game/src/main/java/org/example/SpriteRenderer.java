package org.example;

import io.github.nascentlogic.jgen.gfx.*;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.io.Shader;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.JgenMath;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * F.Dahl, 7/10/2026
 */
public class SpriteRenderer implements Disposable {

    private static final int SAMPLER_ARRAY_SIZE = 15;
    private static final int VERTEX_SIZE_FLOAT = 6;
    private static final int SPRITE_SIZE_FLOAT = VERTEX_SIZE_FLOAT * 4;
    private static final int SPRITE_COUNT_LIMIT = (Short.MAX_VALUE + 1) / 4;
    private static final String PROGRAM_NAME = "spritebatch";
    private static final String PROGRAM_DIR = "res/glsl";

    /*
     * v0------v3
     * |        |
     * |        |
     * v1------v2
     * -----------------------------------
     * vertex: | pos | uv | color | data |
     * -----------------------------------
     * size:   | 2   | 2  | 1     | 1    |
     * -----------------------------------
     */ // 0,1,2,2,3,0 ...

    private final Texture[] texSlots = new Texture[SAMPLER_ARRAY_SIZE];
    private int nextTexSlot;
    private int prevTexSlot;

    private final FloatBuffer vertices;
    private final int vao;
    private final int vbo;
    private final int ebo;

    private final int limit;
    private int count;
    private int drawCalls;
    private boolean rendering;
    private boolean antiAlias;

    private final Matrix4f screenSpaceMatrix = new Matrix4f();
    private final ShaderProgram batchProgram;
    private Framebuffer batchBuffer;


    public SpriteRenderer(Vector2i resolution, int batchCapacity) throws Exception {
        batchBuffer = createFramebuffer(resolution.x,resolution.y);
        JgenMath.screenSpaceMatrix(resolution.x,resolution.y,screenSpaceMatrix);
        ShaderProgram program = ShaderProgram.getProgramByName(PROGRAM_NAME);
        if (program == null) {
            Shader shader = Disk.resourceShader(PROGRAM_NAME,PROGRAM_DIR);
            program = new ShaderProgram(shader);
        } this.batchProgram = program;
        program.use();
        int[] samplers = new int[SAMPLER_ARRAY_SIZE];
        for (int i = 0; i < SAMPLER_ARRAY_SIZE; i++) samplers[i] = i;
        ShaderProgram.setUniformI("uTextures",samplers);
        ShaderProgram.useNone();
        limit = Math.clamp(batchCapacity, 128, SPRITE_COUNT_LIMIT);
        int bufferSizeFloat = limit * SPRITE_SIZE_FLOAT;
        vertices = MemoryUtil.memAllocFloat(bufferSizeFloat);
        vao = Buffers.generateBindVAO();
        ebo = Buffers.generateQuadEBO(limit);
        vbo = Buffers.generateVBO(GL_DYNAMIC_DRAW, bufferSizeFloat * Float.BYTES);
        int pointer = 0;
        int vertexSize = VERTEX_SIZE_FLOAT * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT,         false, vertexSize, pointer); pointer += 2 * Float.BYTES;
        glVertexAttribPointer(1, 2, GL_FLOAT,         false, vertexSize, pointer); pointer += 2 * Float.BYTES;
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true,  vertexSize, pointer); pointer += Float.BYTES;
        glVertexAttribPointer(3, 1, GL_FLOAT,         false, vertexSize, pointer);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        Buffers.bindVBO(0);
        Buffers.bindVAO(0);
    }


    public void begin() { begin(screenSpaceMatrix); }
    public void begin(Matrix4f projView) {
        if (!rendering) {
            batchProgram.use();
            ShaderProgram.setUniformF("uCombined",projView);
            batchBuffer.bindDraw();
            batchBuffer.viewport();
            batchBuffer.clearColor(0,0,0,0,0);
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            drawCalls = 0;
            rendering = true;
        }
    }

    public void end() {
        if (rendering) {
            flush();
            ShaderProgram.useNone();
            batchBuffer.unbind();
            rendering = false;
        }
    }

    public void flush() {
        if (count > 0) {
            for (int i = 0; i < nextTexSlot; i++) {
                texSlots[i].bindToSlot(i);
                texSlots[i] = null;
            }
            nextTexSlot = 0;
            prevTexSlot = 0;
            Buffers.bindVBO(vbo);
            Buffers.uploadVBO(vertices.flip());
            Buffers.bindVAO(vao);
            glDrawElements(GL_TRIANGLES,6 * count,GL_UNSIGNED_SHORT,0);
            vertices.clear();
            drawCalls++;
            count = 0;
        }
    }


    public void draw(Texture texture, Rectanglef rect, Vector4f uv) {
        draw(texture, Color.WHITE,rect.minX,rect.minY,rect.maxX,rect.maxY,uv.x,uv.y,uv.z,uv.w,0.0f);
    }

    public void draw(Texture texture, Color color, Rectanglef rect, Vector4f uv) {
        draw(texture,color,rect.minX,rect.minY,rect.maxX,rect.maxY,uv.x,uv.y,uv.z,uv.w,0.0f);
    }

    public void draw(Texture texture, Color color, Rectanglef rect, Vector4f uv, float rot) {
        draw(texture,color,rect.minX,rect.minY,rect.maxX,rect.maxY,uv.x,uv.y,uv.z,uv.w,rot);
    }

    public void draw(Texture texture, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
        draw(texture,Color.WHITE,x1,y1,x2,y2,u1,v1,u2,v2,0.0f);
    }

    public void draw(Texture texture, Color color, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
        draw(texture,color,x1,y1,x2,y2,u1,v1,u2,v2,0.0f);
    }

    public void draw(Texture texture, Color color, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, float rot) {
        if (!rendering) throw new IllegalStateException("call begin() before draw()");
        if (count == limit) flush();
        int textureSlot;
        if (texture == null || texture.target() != GL_TEXTURE_2D || texture.isDisposed()) {
            textureSlot = SAMPLER_ARRAY_SIZE;
        } else { textureSlot = getTextureSlot(texture);
            if (textureSlot == SAMPLER_ARRAY_SIZE) { flush();
                textureSlot = getTextureSlot(texture);
            }
        }

        int iData = textureSlot;
        if (antiAlias) iData |= (1 << 4);
        float data = Float.intBitsToFloat(iData);
        float packedColor = color.packedFormat();
        float v0x, v0y, v1x, v1y, v2x, v2y, v3x, v3y;

        if (rot != 0.0f) {
            float wh = (x2 - x1) * 0.5f;
            float hh = (y2 - y1) * 0.5f;
            float cx = x1 + wh;
            float cy = y1 + hh;
            float sin = (float) Math.sin(rot);
            float cos = (float) Math.cos(rot);
            float sinWh = sin * wh;
            float sinHh = sin * hh;
            float cosWh = cos * wh;
            float cosHh = cos * hh;
            // v0: Top-Left (x1, y2)
            v0x = -cosWh - sinHh + cx;
            v0y = -sinWh + cosHh + cy;
            // v1: Bottom-Left (x1, y1)
            v1x = -cosWh + sinHh + cx;
            v1y = -sinWh - cosHh + cy;
            // v2: Bottom-Right (x2, y1)
            v2x =  cosWh + sinHh + cx;
            v2y =  sinWh - cosHh + cy;
            // v3: Top-Right (x2, y2)
            v3x =  cosWh - sinHh + cx;
            v3y =  sinWh + cosHh + cy;
        } else {
            v0x = x1; v0y = y2; // Top-Left
            v1x = x1; v1y = y1; // Bottom-Left
            v2x = x2; v2y = y1; // Bottom-Right
            v3x = x2; v3y = y2; // Top-Right
        }
        // v0------v3
        // |        |
        // |        |
        // v1------v2
        // Vertex Layout: | pos (2f) | uv (2f) | color (1f) | data (1f) |
        // Note: v1 is Top UV edge, v2 is Bottom UV edge
        vertices.put(v0x).put(v0y).put(u1).put(v1).put(packedColor).put(data); // v0: Top-Left (x1, y2) -> (u1, v1)
        vertices.put(v1x).put(v1y).put(u1).put(v2).put(packedColor).put(data); // v1: Bottom-Left (x1, y1) -> (u1, v2)
        vertices.put(v2x).put(v2y).put(u2).put(v2).put(packedColor).put(data); // v2: Bottom-Right (x2, y1) -> (u2, v2)
        vertices.put(v3x).put(v3y).put(u2).put(v1).put(packedColor).put(data); // v3: Top-Right (x2, y2) -> (u2, v1)
        count++;
    }

    /** Enable / disable pixel art anti aliasing. (No performance cost) */
    public void enablePixelArtAA(boolean enable) {
        antiAlias = enable;
    }

    public Texture texture() {
        return batchBuffer.attachment(0);
    }

    @Override
    public void free() {
        MemoryUtil.memFree(vertices);
        Buffers.deleteVAO(vao);
        Buffers.deleteBuffers(vbo,ebo);
        Disposable.free(batchBuffer);
    }

    private Framebuffer createFramebuffer(int width, int height) {
        Texture texture = Texture.generate2D(width,height);
        texture.allocate(TextureFormat.RGBA16F,false);
        texture.filterNearest();
        texture.clampToBorder();
        Framebuffer framebuffer = new Framebuffer(width, height);
        framebuffer.attachTexture(texture,0,true);
        framebuffer.drawbuffer(0);
        return framebuffer;
    }

    private int getTextureSlot(Texture texture) {
        // Check previously bound slot cache
        if (texSlots[prevTexSlot] == texture) {
            return prevTexSlot;
        } // Search active slots
        for (int i = 0; i < nextTexSlot; i++) {
            if (texSlots[i] == texture) {
                prevTexSlot = i;
                return i;
            }
        } // All slots occupied; signal caller to flush
        if (nextTexSlot == SAMPLER_ARRAY_SIZE) {
            return SAMPLER_ARRAY_SIZE;
        } // Register texture into next free slot
        texSlots[nextTexSlot] = texture;
        prevTexSlot = nextTexSlot;
        return nextTexSlot++;
    }


}
