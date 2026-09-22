package io.github.nascentlogic.jgen.gui.api;

/**
 * F.Dahl, 9/19/2026
 */
public interface JuiWindows {


    // save the id stack, to trace back to winow. (See if an id belongs to window)

    void windowRegister(JuiWindow window);

    void windowOpen(String name);

    void windowClose(String name);



}
