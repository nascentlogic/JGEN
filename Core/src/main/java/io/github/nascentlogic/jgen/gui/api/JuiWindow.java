package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gui.JuiCore;

/**
 * F.Dahl, 10/1/2026
 */
public interface JuiWindow {

    default void onRegister(JuiCore jui) { /* */ }
    void process(JuiCore jui, float dt); //                               layer
    String name();
    void open();
    void close();
    boolean isOpen();
    default void onExit(JuiCore jui) { /* */ }

}
