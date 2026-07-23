package org.example;

import io.github.nascentlogic.jgen.*;
import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.Matrix4f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;

/**
 * F.Dahl, 6/17/2026
 */
public class TestGame implements Game {

    static void main(String[] args) {
        Jgen.get().launch(new TestGame(),args);
    }

    Matrix4f projView = new Matrix4f();
    Color quadColor = new Color("#76428a");
    SpriteRenderer batch;
    float rot;

    public void configure(LaunchConfig config, String[] args) {
        for (String s : args) System.out.println(s);
        config.gameResolution.set(1280,720);
        // config.gameResolution.set(1920,1080);
        config.windowedMode = true;
        config.resizableWindow = true;
        config.vsyncEnabled = true;
        // config.preferredMonitor = "Generic PnP Monitor|344x194mm";
    }

    public void start() throws Exception {
        batch = new SpriteRenderer(Jgen.get().window().gameResolution(new Vector2i()), 256);
        // BitmapAtlas atlas = Disk.gameLoadAtlas("objects","assets","atlas");
        // atlas.free();
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
        rot += (float) (dt / 16.0);
        // System.out.println(Jgen.get().time().fpsEstimate());
    }

    public void render() {
        Window window = Jgen.get().window();
        batch.begin();
        batch.draw(null,quadColor,256,256,512,512,0,0,1,1,rot);
        batch.end();
        window.presentFrame(batch.texture(),true);
    }

    public void exit() {
        Disposable.free(batch);
    }
}
