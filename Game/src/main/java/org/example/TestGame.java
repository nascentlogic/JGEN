package org.example;

import io.github.nascentlogic.jgen.*;
import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gfx.Framebuffer;
import io.github.nascentlogic.jgen.io.BitmapAtlas;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.JgenMath;
import org.joml.Matrix4f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;

/**
 * F.Dahl, 6/17/2026
 */
public class TestGame implements Game {

    static void main(String[] args) {
        Jgen.get().launch(new TestGame(),args);
    }

    Matrix4f projView = new Matrix4f();
    Color quadColor = new Color("#FF000033");
    SpriteBatch batch;
    float rot;

    public void configure(LaunchConfig config, String[] args) {
        for (String s : args) System.out.println(s);
        config.gameResolution.set(1280,720);
        config.windowedMode = true;
        config.resizableWindow = true;
        config.vsyncEnabled = true;
        // config.preferredMonitor = "Generic PnP Monitor|344x194mm";
    }

    public void start() throws Exception {

        batch = new SpriteBatch(256);
        JgenMath.screenSpaceMatrix(1280,720,projView);
        BitmapAtlas atlas = Disk.gameLoadAtlas("objects","assets","atlas");
        atlas.free();


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

        rot += (float) dt;


        // System.out.println(Jgen.get().time().fpsEstimate());
    }

    public void render() {
        Window window = Jgen.get().window();
        Framebuffer.bindDefaultDraw();
        Framebuffer.viewportDefault();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
        batch.begin(projView,true);
        batch.draw(null,quadColor,256,255,512,512,0,0,1,1,rot);
        batch.end();

        //glEnable(GL_SCISSOR_TEST);
        //Vector4i viewport = window.viewport(new Vector4i());
        //glScissor(viewport.x, viewport.y, viewport.z, viewport.w);
        //glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        //glClear(GL_COLOR_BUFFER_BIT);
        //glDisable(GL_SCISSOR_TEST);
    }

    public void exit() {
        Disposable.free(batch);
    }
}
