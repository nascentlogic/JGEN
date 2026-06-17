package io.github.nascentlogic.jgen;

/**
 * F.Dahl, 5/10/2026
 */
public interface Game {
    void configure(LaunchConfig config, String[] args);
    void start() throws Exception;
    void update(double dt);
    void render();
    void exit();
}
