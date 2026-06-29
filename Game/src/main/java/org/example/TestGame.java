package org.example;

import io.github.nascentlogic.jgen.*;
import io.github.nascentlogic.jgen.gfx.Program;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.io.Shader;
import io.github.nascentlogic.jgen.io.TextureAtlas;
import io.github.nascentlogic.jgen.utils.JgenUtils;
import org.joml.Vector2i;
import org.joml.Vector4i;
import org.joml.primitives.Rectanglef;
import org.tinylog.Logger;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * F.Dahl, 6/17/2026
 */
public class TestGame implements Game {

    static void main(String[] args) {
        Jgen.get().launch(new TestGame(),args);
    }

    public void configure(LaunchConfig config, String[] args) {
        for (String s : args) System.out.println(s);
        config.gameResolution.set(1280,720);
        config.windowedMode = true;
        config.resizableWindow = true;
        config.vsyncEnabled = true;
        // config.preferredMonitor = "Generic PnP Monitor|344x194mm";
    }

    public void start() throws Exception {

        Window window = Jgen.get().window();


        long time = Jgen.get().time().runTimeNS();
        TextureAtlas atlas = Disk.gameLoadAtlas("objects","assets","atlas");
        time = Jgen.get().time().runTimeNS() - time;
        Logger.info("Time: {}", JgenUtils.formatNanos(time));

        List<Shader> shaders = Disk.gameLoadShaders("assets","shaders");
        new Program(shaders.getFirst());

        int y = 0;


        // window.toggleVsync(true);
        // List<Window.Monitor> monitors = window.connectedMonitors();
        // for (Window.Monitor monitor : monitors) {
        //     System.out.println(monitor.persistentID);
        // }

    }

    public void update(double dt) {

        Window window = Jgen.get().window();
        Mouse mouse = Jgen.get().mouse();
        Keyboard keys = Jgen.get().keys();
        if (keys.justPressed(GLFW_KEY_A)) {
            Window.Monitor currentMonitor = window.currentMonitor();
            if (currentMonitor == null) {
                // in windowed
                Window.Monitor primaryMonitor = window.primaryMonitor();
                if (primaryMonitor != null) {
                    window.fullscreenMode(primaryMonitor);
                }

            } else {
                // in exclusive fullscreen
                Vector2i gameResolution = window.gameResolution(new Vector2i());
                window.windowedMode(currentMonitor,gameResolution.x,gameResolution.y);

            }
        }

        if (mouse.isDragging(Mouse.LEFT)) {
            Rectanglef a = mouse.dragArea(Mouse.LEFT,new Rectanglef());
            if(a.isValid()) System.out.printf("Area: min(%.2f,%.2f), max(%.2f,%.2f)%n",a.minX,a.minY,a.maxX,a.maxY);
        }
        // System.out.println(Jgen.get().time().fpsEstimate());
    }

    public void render() {
        Window window = Jgen.get().window();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,GL_NONE);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        glEnable(GL_SCISSOR_TEST);
        Vector4i viewport = window.viewport(new Vector4i());
        glScissor(viewport.x, viewport.y, viewport.z, viewport.w);
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        glDisable(GL_SCISSOR_TEST);
    }

    public void exit() {

    }
}
