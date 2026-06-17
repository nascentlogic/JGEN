package io.github.nascentlogic.jgen;

import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.utils.JgenUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.Objects;

import static io.github.nascentlogic.jgen.utils.JgenUtils.formatBytes;
import static org.joml.Math.min;

/**
 * F.Dahl, 5/10/2026
 */
public final class Jgen {

    private static Jgen insntance;
    private Jgen() { /* */ }
    public static Jgen get() {
        if (insntance == null) {
            insntance = new Jgen();
        } return insntance;
    }

    private Window window;
    public Window window() { return window; }
    public Mouse mouse() { return window.mouse(); }
    public Keyboard keys() { return window.keyboard(); }
    public Gamepads gamepads() { return window.gamepads(); }

    private Game game;
    public Game game() { return game; }
    public <T extends Game> T game(Class<T> clazz) {
        if (game.getClass() != clazz) {
            throw new ClassCastException("");
        } return clazz.cast(game);
    }

    private JgenTime time;
    public JgenTime time() { return time; }

    public void exit() { window.signalToClose(); }



    public void launch(Game game, String[] args) {
        Objects.requireNonNull(game,"game cannot be null");

        try {
            Disk.initialize();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        logSystemInfo();

        this.game = game;
        LaunchConfig config = new LaunchConfig();
        game.configure(config,args == null ? new String[0] : args);

        try {
            Logger.info("CREATING WINDOW");
            window = new Window(config);
        } catch (Exception e) {
            Logger.error(e);
            return;
        }
        logGraphicsInfo();

        try {
            Logger.info("STARTING GAME");
            game.start();
            time = new JgenTime();
            Logger.info("ENTERING MAINLOOP");

            while (!window.shouldClose()) {
                time.tick();
                window.processInput();
                game.update(time.deltaTime());
                int fpsCap = window.targetFps();
                if (!window.isMinimized()) {
                    game.render();
                    window.swapBuffers();
                    time.sync(fpsCap,window.isVsyncEnabled());
                } else {
                    fpsCap = min(fpsCap,window.backGroundFps());
                    time.sync(fpsCap,false);
                }
            }
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            Logger.info("EXITING GAME");
            try { game.exit();
            } catch (Exception e) {
                Logger.error(e);
            } finally { Logger.info("CLOSING WINDOW");
                window.terminate();
            }
        }
    }

    private static void logSystemInfo() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n\n=== SYSTEM & JVM INFORMATION ===");
        sb.append("\n\tUsername          : ").append(System.getProperty("user.name"));
        sb.append("\n\tOS                : ").append(System.getProperty("os.name"))
                .append(" (").append(System.getProperty("os.version")).append(")");
        sb.append("\n\tOS Architecture   : ").append(System.getProperty("os.arch"));
        String cpu = System.getenv("PROCESSOR_IDENTIFIER"); // windows specific
        if (cpu != null) sb.append("\n\tCPU               : ").append(cpu);
        sb.append("\n\tJava Version      : ").append(System.getProperty("java.version"));
        sb.append("\n\tJava Home         : ").append(System.getProperty("java.home"));
        sb.append("\n\tJVM Vendor        : ").append(System.getProperty("java.vm.vendor"));
        Runtime runtime = Runtime.getRuntime();
        sb.append("\n--- Memory ---");
        sb.append("\n\tMax Heap (-Xmx)   : ").append(formatBytes(runtime.maxMemory()));
        sb.append("\n--- Paths ---");
        sb.append("\n\tRoot Path         : ").append(Paths.get("").toAbsolutePath());
        sb.append("\n\tWorking Dir       : ").append(System.getProperty("user.dir"));
        sb.append("\n\tUser Data         : ").append(Disk.userDataDirectory());
        sb.append("\n\tGame Root         : ").append(Disk.gameRootDirectory());
        sb.append("\n--- JVM Arguments ---");
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            sb.append("\n\t   ").append(arg);
        } sb.append("\n");
        Logger.info(sb.toString());
    }

    private static void logGraphicsInfo() {
        long windowHandle = Jgen.get().window().handle();
        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n\n=== GRAPHICS & OPENGL INFORMATION ===");
        sb.append("\n\tLWJGL Version     : ").append(Version.getVersion());
        String vendor   = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String version  = GL11.glGetString(GL11.GL_VERSION);
        String glsl     = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        sb.append("\n\tGPU Vendor        : ").append(vendor != null ? vendor : "Unknown");
        sb.append("\n\tGPU Renderer      : ").append(renderer != null ? renderer : "Unknown");
        sb.append("\n\tOpenGL Version    : ").append(version != null ? version : "Unknown");
        sb.append("\n\tGLSL Version      : ").append(glsl != null ? glsl : "Unknown");
        // Window Properties
        int profile = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_OPENGL_PROFILE);
        String profileStr = (profile == GLFW.GLFW_OPENGL_CORE_PROFILE) ? "Core" :
                (profile == GLFW.GLFW_OPENGL_COMPAT_PROFILE) ? "Compatibility" : "Any/ES";
        sb.append("\n\tContext Profile   : ").append(profileStr);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            var caps = GL.getCapabilities();
            // GPU VRAM Diagnostics
            sb.append("\n--- Video Memory (VRAM) ---");
            if (caps.GL_NVX_gpu_memory_info) {
                GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX, buf);
                sb.append("\n\tDedicated VRAM    : ").append(JgenUtils.formatBytes(buf.get(0) * 1024L)).append(" (NVIDIA)");
                GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX, buf);
                sb.append("\n\tAvailable VRAM    : ").append(JgenUtils.formatBytes(buf.get(0) * 1024L));
            } else if (caps.GL_ATI_meminfo) {
                GL11.glGetIntegerv(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI, buf);
                sb.append("\n\tFree VRAM Pool    : ").append(JgenUtils.formatBytes(buf.get(0) * 1024L)).append(" (AMD/ATI)");
            } else sb.append("\n\tVRAM Tracking     : Not Supported by Hardware Driver");
            // Critical Hardware Limitations
            sb.append("\n--- Hardware Engine Limitations ---");
            GL11.glGetIntegerv(GL11.GL_MAX_TEXTURE_SIZE, buf);
            sb.append("\n\tMax Texture Dim   : ").append(buf.get(0)).append(" x ").append(buf.get(0));
            GL11.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, buf);
            sb.append("\n\tMax Fragment Texture Units : ").append(buf.get(0));
            GL11.glGetIntegerv(GL21.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, buf);
            sb.append("\n\tMax Combined Texture Units : ").append(buf.get(0));
            GL11.glGetIntegerv(GL20.GL_MAX_VERTEX_ATTRIBS, buf);
            sb.append("\n\tMax Vertex Attributes      : ").append(buf.get(0));
            GL11.glGetIntegerv(GL30.GL_MAX_COLOR_ATTACHMENTS, buf);
            sb.append("\n\tMax FBO Color Attachments  : ").append(buf.get(0));
            GL11.glGetIntegerv(GL20.GL_MAX_DRAW_BUFFERS, buf);
            sb.append("\n\tMax MRT Draw Buffers       : ").append(buf.get(0));
            // Uniform Buffer Object (UBO) Pipeline Constraints (Guaranteed by GL 3.1+)
            sb.append("\n--- Uniform Buffer Object (UBO) Limitations ---");
            GL11.glGetIntegerv(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS, buf);
            sb.append("\n\tMax Global UBO Bindings    : ").append(buf.get(0));
            GL11.glGetIntegerv(GL31.GL_MAX_UNIFORM_BLOCK_SIZE, buf);
            sb.append("\n\tMax Per-UBO Block Size     : ").append(JgenUtils.formatBytes(buf.get(0)));
            GL11.glGetIntegerv(GL31.GL_MAX_VERTEX_UNIFORM_BLOCKS, buf);
            sb.append("\n\tMax Vertex Uniform Blocks  : ").append(buf.get(0));
            GL11.glGetIntegerv(GL31.GL_MAX_FRAGMENT_UNIFORM_BLOCKS, buf);
            sb.append("\n\tMax Fragment Uniform Blocks: ").append(buf.get(0));
        } sb.append("\n");
        Logger.info(sb.toString());
    }

}
