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

    GuiWindowTest windowTest = new GuiWindowTest();
    JuiCore jui;

    public void configure(LaunchConfig config, String[] args) {
        for (String s : args) System.out.println(s);
        config.gameResolution.set(1280,720);config.gameResolution.set(1920,1080);
        config.windowedMode = true;
        config.resizableWindow = true;
        config.vsyncEnabled = true;
    }

    public void start() throws Exception {
        jui = new JuiCore();
        jui.windowRegister(windowTest,1);
    }

    public void update(double dt) {
        if (Jgen.get().keys().justPressed(GLFW_KEY_ESCAPE)) Jgen.get().exit();
    }


    public void render() {

        Jgen.get().window().presentFrame(jui.render(),true);
    }


    public void exit() {
        Disposable.free(jui);
    }
}
