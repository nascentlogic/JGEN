package org.example;

import io.github.nascentlogic.jgen.Game;
import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.LaunchConfig;
import io.github.nascentlogic.jgen.Window;
import org.joml.Vector4i;

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
        // window.toggleVsync(true);
        // List<Window.Monitor> monitors = window.connectedMonitors();
        // for (Window.Monitor monitor : monitors) {
        //     System.out.println(monitor.persistentID);
        // }

    }

    public void update(double dt) {

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
