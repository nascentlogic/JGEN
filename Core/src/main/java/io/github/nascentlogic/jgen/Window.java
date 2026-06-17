package io.github.nascentlogic.jgen;

import io.github.nascentlogic.jgen.io.GameModuleProperties;
import io.github.nascentlogic.jgen.io.Platform;
import org.joml.Math;
import org.joml.Vector2i;
import org.joml.Vector4i;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.joml.Math.clamp;
import static org.joml.Math.min;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMonitors;
import static org.lwjgl.opengl.GL11.GL_TRUE;

/**
 * F.Dahl, 6/10/2026
 */
public class Window {

    public static final int GAME_RES_MIN = 128;
    public static final int GAME_RES_MAX = 5000;
    public static final int TARGET_FPS_MIN = 10;
    public static final int TARGET_FPS_MAX = 1200;
    public static final int TARGET_FPS_MAX_BACKGROUND = 200;

    private final long windowHandle;
    private final Mouse mouse;
    private final Keyboard keyboard;
    private final Gamepads gamepads;
    private final Vector2i windowSize;
    private final Vector4i viewport;
    private final Vector2i framebufferRes;
    private final Vector2i gameResolution;
    private boolean vsyncEnabled;
    private int targetFps;
    private int backgroundFps;

    Window(LaunchConfig config) throws Exception {
        // =============================================================================
        // CREATE WINDOW
        // =============================================================================
        glfwSetErrorCallback(new GLFWErrorCallback() {
            public void invoke(int error, long description) {
                if (error != GLFW_NO_ERROR) {
                    Logger.error("GLFW ERROR[{}]: {}", error,
                            GLFWErrorCallback.getDescription(description));}}
        });
        if (!glfwInit()) { freeGLFWErrorCallback();
            throw new Exception("Unable to initialize GLFW");
        } Monitor targetMonitor;
        Monitor primaryMonitor = primaryMonitor();
        if (primaryMonitor == null) {
            glfwTerminate();
            freeGLFWErrorCallback();
            throw new Exception("Unable to create window. No monitors connected");
        } targetMonitor = locateMonitor(config.preferredMonitor);
        targetMonitor = targetMonitor == null ? primaryMonitor : targetMonitor;
        // =============================================================================
        // CREATE WINDOW
        // =============================================================================
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE,            GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED,          GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE,          config.resizableWindow ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_CENTER_CURSOR,      GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY,       GLFW_TRUE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW,      GLFW_TRUE);
        glfwWindowHint(GLFW_RED_BITS,           GLFW_DONT_CARE);
        glfwWindowHint(GLFW_GREEN_BITS,         GLFW_DONT_CARE);
        glfwWindowHint(GLFW_BLUE_BITS,          GLFW_DONT_CARE);
        glfwWindowHint(GLFW_DEPTH_BITS,         0);
        glfwWindowHint(GLFW_STENCIL_BITS,       0);
        glfwWindowHint(GLFW_REFRESH_RATE,       GLFW_DONT_CARE);
        glfwWindowHint(GLFW_DOUBLEBUFFER,       GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES,            0);
        glfwWindowHint(GLFW_SCALE_FRAMEBUFFER,  GLFW_FALSE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR,   GLFW_FALSE);
        glfwWindowHint(GLFW_CLIENT_API,         GLFW_OPENGL_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE,     GLFW_OPENGL_CORE_PROFILE);
        if (Platform.get() == Platform.MAC) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,      4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,      1);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,      GL_TRUE);
        } else { glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,      5);
        }

        vsyncEnabled = config.vsyncEnabled;
        targetFps = clamp(TARGET_FPS_MIN,TARGET_FPS_MAX,config.targetFps);
        backgroundFps = clamp(TARGET_FPS_MIN, TARGET_FPS_MAX_BACKGROUND,config.backgroundFps);
        if (config.gameResolution == null || (config.gameResolution.x == 0 && config.gameResolution.y == 0)) {
            gameResolution = new Vector2i(0);
        } else {
            gameResolution = new Vector2i(config.gameResolution);
            gameResolution.x = clamp(GAME_RES_MIN,GAME_RES_MAX,gameResolution.x);
            gameResolution.y = clamp(GAME_RES_MIN,GAME_RES_MAX,gameResolution.y);
            if (gameResolution.x != config.gameResolution.x || gameResolution.y != config.gameResolution.y) {
                Logger.warn("Clamped unreasonable game resolution: ({}x{}) --> ({}x{}).",
                        config.gameResolution.x,config.gameResolution.y,gameResolution.x,gameResolution.y);

            }
        }

        long monitorHandle;
        boolean windowedBorderless;
        String windowTitle = GameModuleProperties.get(GameModuleProperties.GAME_NAME);
        windowTitle = windowTitle == null ? "Untitled Application" : windowTitle;
        GLFWVidMode display = targetMonitor.videoMode;
        if (config.windowedMode) {
            monitorHandle = 0L;
            windowSize = determineWindowedDimensions(display,gameResolution);
            windowedBorderless = (windowSize.x == display.width() && windowSize.y == display.height());
            glfwWindowHint(GLFW_DECORATED, windowedBorderless ? GLFW_FALSE : GLFW_TRUE);
            glfwWindowHint(GLFW_REFRESH_RATE, GLFW_DONT_CARE);
        } else { monitorHandle = targetMonitor.handle;
            windowSize = new Vector2i(display.width(),display.height());
            windowedBorderless = false;
            glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
            glfwWindowHint(GLFW_REFRESH_RATE, display.refreshRate());
        } windowHandle = glfwCreateWindow(windowSize.x,windowSize.y,windowTitle,monitorHandle,0L);
        if (windowHandle == 0L) {
            glfwTerminate();
            freeGLFWErrorCallback();
            throw new Exception("Unable to create GLFW window");
        }
        // =============================================================================
        // WINDOW LAYOUT & POSITIONING
        // =============================================================================
        if (config.windowedMode) {
            if (windowedBorderless) {
                glfwSetWindowPos(windowHandle, targetMonitor.xPos, targetMonitor.yPos);
                Logger.info("Created borderless windowed mode window on monitor: \"{}\".", targetMonitor.name);
            } else try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer windowW = stack.mallocInt(1);
                IntBuffer windowH = stack.mallocInt(1);
                glfwGetWindowSize(windowHandle, windowW, windowH);
                int targetX = targetMonitor.xPos + (display.width()  - windowW.get(0)) / 2;
                int targetY = targetMonitor.yPos + (display.height() - windowH.get(0)) / 2;
                glfwSetWindowPos(windowHandle, targetX, targetY);
                Logger.info("Created decorated windowed mode window on monitor: \"{}\".", targetMonitor.name);}
        } else { if (glfwGetWindowMonitor(windowHandle) != 0L) Logger.info("Created exclusive fullscreen mode window on monitor: \"{}\".", targetMonitor.name);
            else { Logger.error("OS graphics driver failed to instantiate exclusive fullscreen context during window creation.");
                glfwTerminate();
                freeGLFWErrorCallback();
                throw new Exception("Unable to create GLFW window");
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(windowHandle,w,h);
            windowSize.set(w.get(0),h.get(0));
            glfwGetFramebufferSize(windowHandle,w,h);
            framebufferRes = new Vector2i(w.get(0),h.get(0));
            viewport = new Vector4i();
            rescaleViewport(framebufferRes,gameResolution,viewport);
            Logger.info("Window content size:\t\t\t({}x{})", windowSize.x, windowSize.y);
            Logger.info("Window framebuffer resolution:\t({}x{})", framebufferRes.x, framebufferRes.y);
            Logger.info("Window viewport resolution:\t\t({}x{})", viewport.z, viewport.w);
        }

        mouse = new Mouse(this);
        keyboard = new Keyboard();
        gamepads = new Gamepads();

        glfwSetFramebufferSizeCallback(windowHandle, new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                framebufferRes.set(width,height);
                rescaleViewport(framebufferRes,gameResolution,viewport);
            }
        });
        glfwSetWindowSizeCallback(windowHandle, new GLFWWindowSizeCallback() {
            public void invoke(long window, int width, int height) {
                windowSize.set(width,height);
            }
        });
        glfwSetMonitorCallback(new GLFWMonitorCallback() {
            /*
                If a monitor is disconnected, all windows that are full screen on it
                will be switched to windowed mode before the callback is called.
                Only glfwGetMonitorName and glfwGetMonitorUserPointer will return useful values
                for a disconnected monitor and only before the monitor callback returns.
                Only used for debugging.
             */
            public void invoke(long monitor, int event) {
                if (event == GLFW_DISCONNECTED) {
                    Logger.info("Monitor: {}, disconnected", glfwGetMonitorName(monitor));
                } else if (event == GLFW_CONNECTED) {
                    Logger.info("Monitor: {}, connected", glfwGetMonitorName(monitor));
                }
            }
        });


        glfwSetCursorPosCallback(windowHandle, new GLFWCursorPosCallback() {
            public void invoke(long window, double xpos, double ypos) {
                mouse.onCursorHover(xpos,ypos);
            }
        });
        glfwSetMouseButtonCallback(windowHandle, new GLFWMouseButtonCallback() {
            public void invoke(long window, int button, int action, int mods) {
                mouse.onButtonPress(button,action == GLFW_PRESS);
            }
        });
        glfwSetScrollCallback(windowHandle, new GLFWScrollCallback() {
            public void invoke(long window, double xoffset, double yoffset) {
                mouse.onWheelScroll(yoffset);
            }
        });
        glfwSetCursorEnterCallback(windowHandle, new GLFWCursorEnterCallback() {
            public void invoke(long window, boolean entered) {
                mouse.onCursorEnter(entered);
            }
        });
        glfwSetKeyCallback(windowHandle, new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                keyboard.onKeyEvent(key, mods, action);
            }
        });
        glfwSetCharCallback(windowHandle, new GLFWCharCallback() {
            public void invoke(long window, int codepoint) {
                keyboard.onCharPress(codepoint);
            }
        });


        glfwMakeContextCurrent(windowHandle);
        Logger.debug("OpenGL context current in thread: \"{}\"", Thread.currentThread().getName());
        glfwSwapInterval(vsyncEnabled ? 1 : 0);
        glfwShowWindow(windowHandle);
        glfwFocusWindow(windowHandle);
        GL.createCapabilities();
    }

    /**
     * Poll input events (triggering input / display callbacks),
     * then process the input for connected gamepads, keyboard and mouse.
     */
    void processInput() {
        glfwPollEvents();
        gamepads.processInput();
        keyboard.processInput();
        mouse.processInput();
    }
    /** GLFW windows are by default double buffered.
     * That means that you have two rendering buffers; a front buffer and a back buffer.
     * The front buffer is the one being displayed and the back buffer the one you render to.
     * When the entire frame has been rendered, it is time to swap the back and the front buffers
     * in order to display what has been rendered and begin rendering a new frame. */
    void swapBuffers() {
        glfwSwapBuffers(windowHandle);
    }

    /**
     * Signal that the window should close.
     * Triggering the exit from the main-loop the next iteration.
     */
    void signalToClose() {
        Logger.debug("Window signalled to close");
        glfwSetWindowShouldClose(windowHandle,true);
    }
    /**
     * @return Window has been signaled to close
     */
    boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    /**
     * Frees callbacks and terminates the window and GLFW.
     */
    @SuppressWarnings("all")
    void terminate() {
        Logger.debug("Clearing OpenGL capabilities");
        GL.setCapabilities(null); // this IS nullable
        Logger.debug("Freeing GLFW input callbacks");
        gamepads.free();
        freeInputCallbacks();
        Logger.debug("Freeing GLFW display callbacks");
        freeDisplayCallbacks();
        Logger.debug("Destroying the GLFW window");
        glfwDestroyWindow(windowHandle);
        Logger.debug("Terminating GLFW");
        glfwTerminate();
        Logger.debug("Freeing GLFW error callback");
        freeGLFWErrorCallback();
    }
    /**
     * @return Whether the window is windowed (not attached to a spesific monitor).
     */
    public boolean isWindowed() {
        return glfwGetWindowMonitor(windowHandle) == 0L;
    }
    /**
     * @return Whether the window is minimized (iconified).
     */
    public boolean isMinimized() {
        return glfwGetWindowAttrib(windowHandle,GLFW_ICONIFIED) == GLFW_TRUE;
    }
    /**
     * @return Whether the window is resizable.
     */
    public boolean isResizable() {
        return glfwGetWindowAttrib(windowHandle,GLFW_RESIZABLE) == GLFW_TRUE;
    }
    /**
     * @return Whether vsync is enabled.
     */
    public boolean isVsyncEnabled() {
        return vsyncEnabled;
    }
    /**
     * Enable / disable resizable windowed window
     */
    public void toggleResizable(boolean enable) { glfwSetWindowAttrib(windowHandle,GLFW_RESIZABLE, enable ? GLFW_TRUE : GLFW_FALSE); }
    /**
     * Enable / disable vsync
     */
    public void toggleVsync(boolean enable) { glfwSwapInterval((vsyncEnabled = enable) ? 1 : 0); }
    /**
     * Set target fps. Caps the fps to this, when vsync is disabled.
     * @see #targetFps()
     */
    public void setTargetFps(int fps) {
        targetFps = clamp(TARGET_FPS_MIN,TARGET_FPS_MAX,fps);
    }
    /**
     * @see #backGroundFps()
     */
    public void setBackgroundFps(int fps) { backgroundFps = clamp(TARGET_FPS_MIN, TARGET_FPS_MAX_BACKGROUND,fps); }
    /**
     * @return GLFW window handle / pointer
     */
    public long handle() {
        return windowHandle;
    }
    /**
     * {@code targetFps} is only relevant when {@code V-Sync} is disabled.
     * Can be changed at runtime via. {@link #setTargetFps(int)}.
     * Clamped to {@link #TARGET_FPS_MIN} and {@link #TARGET_FPS_MAX}
     */
    public int targetFps() {
        return targetFps;
    }
    /**
     * {@code backgroundFps} kicks in when the window is minimized,
     * regardless of whether {@code V-Sync} is enabled / disabled.
     * Can be changed at runtime via. {@link #setBackgroundFps(int)}.
     * Clamped to {@link #TARGET_FPS_MIN} and {@link #TARGET_FPS_MAX_BACKGROUND}
     */
    public int backGroundFps() {
        return backgroundFps;
    }

    public Mouse mouse() {
        return mouse;
    }
    public Keyboard keyboard() {
        return keyboard;
    }
    public Gamepads gamepads() {
        return gamepads;
    }
    public Vector2i windowSize(Vector2i dst) {
        return dst.set(windowSize);
    }
    public Vector2i framebufferResolution(Vector2i dst) {
        return dst.set(framebufferRes);
    }
    public Vector4i viewport(Vector4i dst) { return dst.set(viewport); }

    /**
     * Game Resolution (If set) is a fixed resolution (E.g. 1920x1080) that the Game uses for rendering (off-screen / internal framebuffer).
     * It tells the Window which resolution the Game uses internally (As opposed to the actual display resolution / default glfw framebuffer).
     * If the display resolution does not equal the game resolution, the viewport will adapt to best fit the target game resolution (aspect ratio).
     * If the game resolution is not set ({@code null} or {@code (0,0)}), the game resolution will always equal the display resolution (dynamic game resolution).
     * @param dst the vector to put game resolution.
     * @return fixed game resolution OR the actual window framefuffer resolution (if game resolution is not set).
     */
    public Vector2i gameResolution(Vector2i dst) {
        if (gameResolution == null || (gameResolution.x == 0 && gameResolution.y == 0))
            return framebufferResolution(dst);
        return dst.set(gameResolution);
    }

    /**
     * Set target Game resolution.
     * A {@code resolution} of {@code null} OR {@code (0,0)}, tells the window that
     * the game resolution should be whatever the window framebuffer resolution is (dynamic game resolution).
     * If {@code resolution} is provided, the viewport will adapt to the {@code resolution} (letterboxing).
     * The game only ever need to care about that virtual resolution and let the viewport adapt automatically.
     * Note: unreasoble values are clamped. The passedd in vector remains unmodified.
     * @see #gameResolution
     * @param resolution desired game resolution or {@code null}
     */
    public void setGameResolution(Vector2i resolution) {
        if (resolution == null || (resolution.x == 0 && resolution.y == 0))
            gameResolution.set(0,0);
        else { gameResolution.set(resolution);
            gameResolution.x = Math.clamp(GAME_RES_MIN,GAME_RES_MAX,gameResolution.x);
            gameResolution.y = Math.clamp(GAME_RES_MIN,GAME_RES_MAX,gameResolution.y);
            if (gameResolution.x != resolution.x || gameResolution.y != resolution.y)
                Logger.warn("Clamped unreasonable game resolution: ({}x{}) --> ({}x{}).",
                        resolution.x,resolution.y,gameResolution.x,gameResolution.y);
        } rescaleViewport(framebufferRes,gameResolution,viewport);
    }

    public void fullscreenMode(Monitor monitor) {
        if (monitor != null) {
            if (monitor.isConnected()) {
                GLFWVidMode display = monitor.videoMode;
                Logger.info("Attempting to set window to fullscreen mode on monitor: \"{}\"", monitor.name);
                Logger.info("Target monitor display resolution:     ({}x{})", display.width(), display.height());
                Logger.info("Target monitor refresh rate:           ({}Hz)", display.refreshRate());
                glfwShowWindow(windowHandle);
                glfwSetWindowMonitor(windowHandle, monitor.handle,0,0, display.width(), display.height(), display.refreshRate());
                glfwSwapInterval(vsyncEnabled ? 1 : 0);
                glfwFocusWindow(windowHandle);
                long monitorHandle = glfwGetWindowMonitor(windowHandle);
                if (monitorHandle != 0L) {
                    Logger.info("Entered exclusive fullscreen mode on monitor: \"{}\".", monitor.name);
                    try (MemoryStack stack = MemoryStack.stackPush()){
                        IntBuffer framebufferW = stack.mallocInt(1);
                        IntBuffer framebufferH = stack.mallocInt(1);
                        glfwGetFramebufferSize(windowHandle, framebufferW, framebufferH);
                        Logger.info("New framebuffer resolution:    ({}x{})", framebufferW, framebufferH);
                    } // This fallback should technically never happen
                } else Logger.error("CRITICAL: Operating System graphics driver failed to instantiate exclusive fullscreen context.");
            } else Logger.warn("Unable to use monitor: \"{}\" (Disconnected)", monitor.name);
        } else Logger.warn("Cannot switch to fullscreen mode: Monitor argument is NULL");
    }

    public void windowedMode(Monitor monitor, int desiredWidth, int desiredHeight) {
        if (monitor != null) {
            if (monitor.isConnected()) {
                GLFWVidMode display = monitor.videoMode;
                Vector2i adjustedSize = determineWindowedDimensions(display, new Vector2i(desiredWidth,desiredHeight));
                Logger.info("Attempting to set window to windowed mode on monitor: \"{}\"", monitor.name);
                Logger.info("Target monitor display resolution: ({}x{})", display.width(), display.height());
                Logger.info("Desired window resolution:         ({}x{})", desiredWidth, desiredHeight);
                Logger.info("Adjusted window resolution:        ({}x{})", adjustedSize.x, adjustedSize.y);
                if (display.width() == adjustedSize.x && display.height() == adjustedSize.y) {
                    // =========================================================================
                    // True Borderless Windowed Fullscreen
                    // =========================================================================
                    // Safe from all prior states: It forces the window to match the monitor
                    glfwSetWindowMonitor(windowHandle, 0L, monitor.xPos, monitor.yPos, display.width(), display.height(), GLFW_DONT_CARE);
                    glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
                    Logger.info("Entered borderless windowed mode on monitor: \"{}\".", monitor.name);
                } else {
                    // =========================================================================
                    // Standard Decorated Window
                    // =========================================================================
                    // Turn decorations ON first. This caches the layout property so GLFW
                    // applies window frames the instant we drop out of fullscreen/borderless.
                    glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_TRUE);
                    // Resize the window container to the target dimensions on the monitor.
                    // Handing it back to the desktop window manager (0L) forces the OS to
                    // reconstruct the decorations enabled above.
                    glfwSetWindowMonitor(windowHandle, 0L, monitor.xPos, monitor.yPos, adjustedSize.x, adjustedSize.y, GLFW_DONT_CARE);
                    // At this point, decorations are guaranteed to be active and accounted for by the OS.
                    // Query what the OS actually granted us after state transitions completed.
                    try(MemoryStack stack = MemoryStack.stackPush()) {
                        IntBuffer windowW = stack.mallocInt(1);
                        IntBuffer windowH = stack.mallocInt(1);
                        glfwGetWindowSize(windowHandle, windowW, windowH);
                        // Calculate centering math using real values and update position.
                        int targetX = monitor.xPos + (display.width()  - windowW.get(0)) / 2;
                        int targetY = monitor.yPos + (display.height() - windowH.get(0)) / 2;
                        glfwSetWindowPos(windowHandle, targetX, targetY);
                    } Logger.info("Entered decorated windowed mode on monitor: \"{}\".",monitor.name);
                } glfwSwapInterval(vsyncEnabled ? 1 : 0);
                glfwShowWindow(windowHandle);
                glfwFocusWindow(windowHandle);
                try (MemoryStack stack = MemoryStack.stackPush()){
                    IntBuffer framebufferW = stack.mallocInt(1);
                    IntBuffer framebufferH = stack.mallocInt(1);
                    glfwGetFramebufferSize(windowHandle,framebufferW,framebufferH);
                    Logger.info("New framebuffer resolution:        ({}x{})", framebufferW, framebufferH); }
            } else Logger.warn("Unable to use monitor: \"{}\" (Disconnected)", monitor.name);
        } else Logger.warn("Cannot switch to fullscreen mode: Monitor argument is NULL");
    }

    /**
     * Clamps and adjusts the desired resolution against native display dimensions. <p>
     * Passing a {@code null} OR {@code (0,0)} desired resolution defaults to the full display size.
     * Applies a dual-axis near-miss optimization: if both clamped dimensions fall within
     * 48 pixels of the display size, they automatically snap to full screen. </p>
     * @param display       native target display
     * @param desiredRes    requested dimensions, or null for native display default
     * @return new vector with optimized window dimensions
     */
    private Vector2i determineWindowedDimensions(GLFWVidMode display, Vector2i desiredRes) {
        if (desiredRes == null || (desiredRes.x == 0 && desiredRes.y == 0))
            return new Vector2i(display.width(),display.height());
        final int MIN_RES_SOFTCAP = 128;
        final int SNAP_THRESHOLD = 48;
        int displayW = display.width();
        int displayH = display.height();
        int minW = min(displayW, MIN_RES_SOFTCAP);
        int minH = min(displayH, MIN_RES_SOFTCAP);
        int windowW = clamp(minW, displayW, desiredRes.x);
        int windowH = clamp(minH, displayH, desiredRes.y);
        int xMargin = displayW - windowW;
        int yMargin = displayH - windowH;
        boolean snapX = (xMargin >= 0 && xMargin <= SNAP_THRESHOLD);
        boolean snapY = (yMargin >= 0 && yMargin <= SNAP_THRESHOLD);
        if (snapX && snapY) {
            windowW = displayW;
            windowH = displayH;
        } return new Vector2i(windowW,windowH);
    }


    private void rescaleViewport(Vector2i framebufferRes, Vector2i targetRes, Vector4i dst) {
        if (targetRes == null || targetRes.x == 0 || targetRes.y == 0)
            dst.set(0,0,framebufferRes.x,framebufferRes.y);
        else { int viewW = framebufferRes.x;
            int viewH = framebufferRes.y;
            if (viewW * targetRes.y > viewH * targetRes.x) {
                viewW = (framebufferRes.y * targetRes.x) / targetRes.y;
            } else viewH = (framebufferRes.x * targetRes.y) / targetRes.x;
            int viewX = (framebufferRes.x - viewW) / 2;
            int viewY = (framebufferRes.y - viewH) / 2;
            dst.set(viewX,viewY,viewW,viewH);
        }
    }

    private void freeInputCallbacks() {
        List<Callback> list = new ArrayList<>();
        list.add(glfwSetDropCallback(windowHandle,null));
        list.add(glfwSetKeyCallback(windowHandle,null));
        list.add(glfwSetCharCallback(windowHandle,null));
        list.add(glfwSetCursorEnterCallback(windowHandle,null));
        list.add(glfwSetCursorPosCallback(windowHandle,null));
        list.add(glfwSetMouseButtonCallback(windowHandle,null));
        list.add(glfwSetScrollCallback(windowHandle,null));
        list.add(glfwSetJoystickCallback(null));
        for (Callback c : list) if (c != null) c.free();
    }

    private void freeDisplayCallbacks() {
        List<Callback> list = new ArrayList<>();
        list.add(glfwSetMonitorCallback(null));
        list.add(glfwSetWindowSizeCallback(windowHandle,null));
        list.add(glfwSetWindowPosCallback(windowHandle,null));
        list.add(glfwSetWindowIconifyCallback(windowHandle,null));
        list.add(glfwSetFramebufferSizeCallback(windowHandle,null));
        for (Callback c : list) if (c != null) c.free();
    }

    @SuppressWarnings("resource")
    private void freeGLFWErrorCallback() {
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) errorCallback.free();
    }



    // =============================================================================
    // Monitor
    // =============================================================================


    /**
     * The Monitor Object is a Java wrapper for a physical monitor.
     * This object reflects the state of the monitor at the moment of this objects creation, meaning
     * that the Monitor Object is intended to be short lived (not kept for future reference).
     * F.Dahl, 6/7/2026
     */
    public static final class Monitor {
        /** GLFW monitor handle */
        public final long handle;
        /** Human readable name */
        public final String name;
        /** String representation of the monitor. Incorporates name and physical size.
         * GLFW do not provide unique identifiers for monitors, e.g. a serial number.
         * Therefore: If the user is connected to 2 identical monitors,
         * the monitors would share the same {@code persitentID}. This is tolerable. */
        public final String persistentID;
        /** video mode of monitor (monitor vidMode at the time of this Object creation).
         * Therefore it might not reflect the monitors current video mode.
         * The resolution of a video mode is specified in screen coordinates. */
        public final GLFWVidMode videoMode;
        /** @see org.lwjgl.glfw.GLFW#glfwGetMonitorPos(long, IntBuffer, IntBuffer) */
        public final int xPos;
        /** @see org.lwjgl.glfw.GLFW#glfwGetMonitorPos(long, IntBuffer, IntBuffer) */
        public final int yPos;
        Monitor(long handle) throws Exception {
            if (handle == 0L) throw new IllegalArgumentException("monitor == 0L");
            videoMode = glfwGetVideoMode(handle);
            if (videoMode == null) {
                throw new Exception("Monitor vidMode == NULL");
            } String monitorName = glfwGetMonitorName(handle);
            name = monitorName == null ? "Unnamed Monitor" : monitorName;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer b0 = stack.callocInt(1);
                IntBuffer b1 = stack.callocInt(1);
                glfwGetMonitorPhysicalSize(handle, b0, b1);
                persistentID = String.format("%s|%dx%dmm", name, b0.get(0),b1.get(0));
                glfwGetMonitorPos(handle,b0,b1);
                xPos = b0.get(0);
                yPos = b1.get(0);
            } this.handle = handle;
        } public boolean isConnected() {
            PointerBuffer pb = glfwGetMonitors();
            if (pb == null || pb.limit() == 0) return false;
            for (int i = 0; i < pb.limit(); i++) {
                if (pb.get(i) == handle) return true;
            } return false;
        }
    }

    /** @return the monitor recognized as the primary monitor by the OS
     * or null if no monitor was found or an error occured. */
    public Monitor primaryMonitor() {
        return monitorOrNull(glfwGetPrimaryMonitor());
    }

    /** @return the monitor the window is currently fullscreen on, or null if in widowed mode. */
    public Monitor currentMonitor() {
        return monitorOrNull(glfwGetWindowMonitor(windowHandle));
    }

    /**
     * @param persistentID semi-unique identifier for a monitor
     * @return The first monitor of connected monitors with the same persistent ID,
     * or null if no such monitor is currently connected.
     */
    public Monitor locateMonitor(String persistentID) {
        if (persistentID == null || persistentID.isBlank()) return null;
        List<Monitor> connected = connectedMonitors();
        for (Monitor monitor : connected) {
            if (persistentID.equals(monitor.persistentID))
                return monitor;
        } return null;
    }

    /** @return A list of all currently connected monitors. */
    public List<Monitor> connectedMonitors() {
        PointerBuffer pb = glfwGetMonitors();
        if (pb == null || pb.limit() == 0) return Collections.emptyList();
        List<Monitor> list = new ArrayList<>(pb.limit());
        for (int i = 0; i < pb.limit(); i++) {
            if (pb.get(i) == 0L) continue;
            try { list.add(new Monitor(pb.get(i)));
            } catch (Exception e) { Logger.warn(e);}
        } return list;
    }

    private Monitor monitorOrNull(long monitor) {
        if (monitor == 0L) return null;
        try { return new Monitor(monitor);
        } catch (Exception e) {
            Logger.warn(e);
        } return null;
    }



}
