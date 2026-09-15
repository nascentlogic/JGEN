package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.Mouse;
import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.utils.Disposable;

/**
 * F.Dahl, 9/5/2026
 */
public class JgenGui implements Disposable {

    private final JguiState coreState;
    private final JguiGfx graphics;
    private final JguiStore persistentStorage;
    private final JguiLayout layoutEngine;
    private boolean pocessingFrame;
    private long currentFrame = 0L;


    public JgenGui() throws Exception {
        coreState = new JguiState();
        graphics = new JguiGfx();
        persistentStorage = new JguiStore();
        layoutEngine = new JguiLayout();
    }

    public void beginFrame() {
        if (pocessingFrame) throw new IllegalStateException("Gui frame already started");
        pocessingFrame = true;
        Mouse mouse = Jgen.get().mouse();
        coreState.tick(graphics.readIdBuffer(mouse.position()));
        layoutEngine.resetLayoutStack();
        graphics.beginFrame();
    }


    public Texture endFrame() {
        if (!pocessingFrame) throw new IllegalStateException("Gui has not started processing frame");
        pocessingFrame = false;
        graphics.endFrame();
        currentFrame++;
        return graphics.colorAttachement();
    }

    public void reset() {
        if (pocessingFrame) throw new IllegalStateException("Gui processing frame");

        coreState.reset();
        persistentStorage.clear();
        currentFrame = 0L;
    }

    public JguiGfx graphics() {
        return graphics;
    }

    public JguiState state() {
        return coreState;
    }

    public JguiStore persistentStorage() {
        return persistentStorage;
    }

    public JguiLayout layoutEngine() {
        return layoutEngine;
    }

    public boolean isPocessingFrame() {
        return pocessingFrame;
    }

    public long currentFrame() {
        return currentFrame;
    }

    @Override
    public void free() {
        Disposable.free(graphics);
        persistentStorage.clear();
    }
}
