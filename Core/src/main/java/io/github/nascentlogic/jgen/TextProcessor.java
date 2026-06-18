package io.github.nascentlogic.jgen;

/**
 * F.Dahl, 6/18/2026
 */
public interface TextProcessor {

    void onCharType(byte c);
    void onKeyEvent(int key, int mods, int action);

}
