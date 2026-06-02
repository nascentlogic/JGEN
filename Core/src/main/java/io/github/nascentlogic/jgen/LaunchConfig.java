package io.github.nascentlogic.jgen;

/**
 * F.Dahl, 5/12/2026
 */
public class LaunchConfig {

    public String preferedMonitor = null;

    public transient int gameResolutionWidth = 1280;

    public transient int gameResolutionHeight = 720;

    public int targetFps = 120;

    public boolean vsyncEnabled = true;

    /**
     * If {@code resizableWindow} is enabled, the window will be resizable when windowed.
     * <p>
     * default = true.
     */
    public boolean resizableWindow = true;

    /**
     * If {@code windowedMode} is enabled, the Game will launch windowed,
     * else fullscreen. Useful for debugging as the program might freeze in fullscreen.
     * <p>
     * default = false.
     */
    public boolean windowedMode = true;

    /**
     * If {@code fitViewport} is enabled, window viewport will auto-rescale to fit game aspect ratio,
     * else it will use the window framebuffer resolution (stretch to fit).
     * <p>
     * default = true.
     */
    public boolean fitViewport = true;

}
