package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gui.JuiCore;

/**
 * F.Dahl, 10/1/2026
 */
public interface JuiWindow {


    void process(JuiCore jui, float dt);
    String name();
    void open();
    void close();
    boolean isOpen();
    default void onExit() { /* */ }

}
