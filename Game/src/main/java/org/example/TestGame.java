package org.example;

import io.github.nascentlogic.jgen.*;

import io.github.nascentlogic.jgen.gui.JuiCore;
import io.github.nascentlogic.jgen.utils.Disposable;

import static org.lwjgl.glfw.GLFW.*;

/**
 * F.Dahl, 6/17/2026
 */
public class TestGame implements Game {

    static void main(String[] args) {
        Jgen.get().launch(new TestGame(),args);
    }

    GuiWindowTest windowTest;
    JuiCore jui;

    public void configure(LaunchConfig config, String[] args) {
        for (String s : args) System.out.println(s);
        config.gameResolution.set(1280,720);config.gameResolution.set(1920,1080);
        config.windowedMode = true;
        config.resizableWindow = true;
        config.vsyncEnabled = true;
    }

    public void start() throws Exception {
        windowTest = new GuiWindowTest();
        jui = new JuiCore();
    }

    public void update(double dt) {
        Keyboard keys = Jgen.get().keys();
        if (keys.justPressed(GLFW_KEY_ESCAPE)) Jgen.get().exit();
        // System.out.println(mouse.position().x);
        // System.out.println(Jgen.get().time().fpsEstimate());
    }

    public void render() {
        jui.beginFrame();
        windowTest.render(jui);
        Jgen.get().window().presentFrame(jui.endFrame(),true);
    }


    public void exit() {
        Disposable.free(jui,windowTest);
    }
}
