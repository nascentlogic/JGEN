package io.github.nascentlogic.jgen;

import org.joml.Vector2i;
import org.joml.Vector4i;

/**
 * Configurations for window creation.
 * Can later be expanded to hold audio options and other "main-menu" options.
 * F.Dahl, 5/12/2026
 */
public class LaunchConfig {

    /**
     * If the {@code preferredMonitor} matches a connected monitor during window creation,
     * The window will attempt to launch on that monitor, else the primary monitor will be used.
     * <p> default = {@code null}.
     * @see Window.Monitor#persistentID
     */
    public String preferredMonitor = null;

    /**
     * The target game resolution at launch (Window creation).
     * Can be changed at runtime via. {@link Window#setGameResolution(Vector2i)}.
     * <p> default = {@code 0,0 }.
     * @see Window#viewport(Vector4i)
     * @see Window#gameResolution(Vector2i)
     */
    public Vector2i gameResolution = new Vector2i(0);

    /**
     * {@code targetFps} is only relevant when {@code V-Sync} is disabled.
     * Can be changed at runtime via. {@link Window#setTargetFps(int)}.
     * Clamped to {@link Window#TARGET_FPS_MIN} and {@link Window#TARGET_FPS_MAX}
     * <p> default = {@code 120}.
     */
    public int targetFps = 120;

    /**
     * {@code backgroundFps} kicks in when the window is minimized,
     * regardless of whether {@code V-Sync} is enabled / disabled.
     * Can be changed at runtime via. {@link Window#setBackgroundFps(int)}.
     * Clamped to {@link Window#TARGET_FPS_MIN} and {@link Window#TARGET_FPS_MAX_BACKGROUND}
     * <p> default = {@code 30}.
     */
    public int backgroundFps = 30;

    /**
     * If {@code vsyncEnabled} is {@code true}, vertical synchronization is enabled at lauch.
     * Can be changed at runtime via. {@link Window#toggleVsync(boolean)}
     * <p> default = {@code true}.
     */
    public boolean vsyncEnabled = true;

    /**
     * If {@code resizableWindow} is {@code true}, the window will be resizable at launch.
     * Only affect windowed mode windows. Can be changed at runtime via. {@link Window#toggleResizable(boolean)}
     * <p> default = {@code true}.
     */
    public boolean resizableWindow = true;

    /**
     * If {@code windowedMode} is {@code true}, the game will launch in windowed mode, else fullscreen.
     * <p> default = {@code true}.
     */
    public boolean windowedMode = true;


}
