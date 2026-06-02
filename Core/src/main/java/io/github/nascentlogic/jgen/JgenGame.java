package io.github.nascentlogic.jgen;

/**
 * F.Dahl, 5/10/2026
 */
public interface JgenGame {

    void gameConfigure(LaunchConfig config, String[] args);
    void gameStart() throws Exception;
    void gameUpdate();
    void gameRender();
    void gameExit();

}
