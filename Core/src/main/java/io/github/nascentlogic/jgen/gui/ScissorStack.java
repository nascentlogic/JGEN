package io.github.nascentlogic.jgen.gui;

import org.joml.primitives.Rectanglef;

import static io.github.nascentlogic.jgen.utils.JgenUtils.popRect;
import static io.github.nascentlogic.jgen.utils.JgenUtils.pushRect;
import static org.lwjgl.opengl.GL11.*;

/**
 * F.Dahl, 10/14/2026
 */
class ScissorStack {

    private int depth;
    private final Rectanglef[] stack;
    ScissorStack(int depth) {
        stack = new Rectanglef[depth];
        for (int i = 0; i < depth; i++) stack[i] = new Rectanglef();
    }

    boolean interects(JuiCore jui, Rectanglef rect) {
        Rectanglef scissor;
        if (depth == 0) {
            int framebufferW = jui.framebufferWidth();
            int framebufferH = jui.framebufferHeight();
            scissor = pushRect(framebufferW,framebufferH);
        } else scissor = stack[depth - 1];
        Rectanglef bounds = pushRect(rect);
        bounds.minX *= jui.scale();
        bounds.minY *= jui.scale();
        bounds.maxX *= jui.scale();
        bounds.maxY *= jui.scale();
        popRect(depth == 0 ? 2 : 1);
        return bounds.intersectsRectangle(scissor);
    }

    void push(JuiCore jui, Rectanglef rect, boolean intersection) { push(jui,rect.minX,rect.minY,rect.maxX,rect.maxY,intersection); }
    void push(JuiCore jui, float x1, float y1, float x2, float y2, boolean intersection) {
        if (depth >= stack.length) throw new IllegalStateException("scissor stack overflow!");
        final int framebufferW = jui.framebufferWidth();
        final int framebufferH = jui.framebufferHeight();
        Rectanglef previous;
        if (depth == 0) previous = pushRect(framebufferW,framebufferH);
        else previous = intersection ? pushRect(stack[depth - 1]) : pushRect(framebufferW,framebufferH);
        Rectanglef scissor = stack[depth];
        float minX = jui.scale() * Math.min(x1, x2);
        float minY = jui.scale() * Math.min(y1, y2);
        float maxX = jui.scale() * Math.max(x1, x2);
        float maxY = jui.scale() * Math.max(y1, y2);
        scissor.minX = Math.max(minX,0);
        scissor.minY = Math.max(minY,0);
        scissor.maxX = Math.min(maxX,framebufferW);
        scissor.maxY = Math.min(maxY,framebufferH);
        // Even if "intersection" is false we still intersect with the framebuffer.
        scissor.intersection(previous);
        popRect();
        depth++;
        useCurrent();
    }

    void useCurrent() {
        if (depth < 0) throw new IllegalStateException("scissor stack underflow!");
        if (depth == 0) glDisable(GL_SCISSOR_TEST);
        else { Rectanglef scissor = stack[depth - 1];
            if (scissor.isValid()) {
                int x = (int) Math.floor(scissor.minX);
                int y = (int) Math.floor(scissor.minY);
                int w = (int) Math.ceil(scissor.maxX) - x;
                int h = (int) Math.ceil(scissor.maxY) - y;
                glScissor(x,y,w,h);
            } else glScissor(0,0,0,0);
            glEnable(GL_SCISSOR_TEST);
        }
    }

    void pop() {
        if (depth <= 0) throw new IllegalStateException("scissor stack underflow!");
        depth--;
        useCurrent();
    }

    Rectanglef peek() { return depth == 0 ? null : stack[depth - 1]; }
    int depth() { return depth; }



}
