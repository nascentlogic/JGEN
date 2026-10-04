package org.example;

import io.github.nascentlogic.jgen.gui.JuiCore;
import io.github.nascentlogic.jgen.gui.api.JuiWindow;
import org.joml.primitives.Rectanglef;

/**
 * F.Dahl, 10/2/2026
 */
public abstract class DebugWindow implements JuiWindow {

    protected String name;
    protected Rectanglef currentArea;
    private Rectanglef restoredArea;
    private Rectanglef animOrigin;
    private Rectanglef animTarget;
    private boolean open;
    private boolean signaledToMaximize;
    private boolean signaledToRestore;
    private boolean animating;


    public void process(JuiCore jui, float dt) {

    }


    public String name() { return name; }
    public void open() { open = true; }
    public void close() { open = false; }
    public boolean isOpen() { return open; }
    protected void maximize() { signaledToMaximize = true; }
    protected void resore() { signaledToRestore = true; }


}
