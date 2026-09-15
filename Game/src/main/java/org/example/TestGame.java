package org.example;

import io.github.nascentlogic.jgen.*;
import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.*;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.JgenMath;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.primitives.Rectanglef;

import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

/**
 * F.Dahl, 6/17/2026
 */
public class TestGame implements Game {

    static void main(String[] args) {
        Jgen.get().launch(new TestGame(),args);
    }

    JgenGui ui;
    Color quadColor = new Color("#222222");

    float rot;

    public void configure(LaunchConfig config, String[] args) {
        for (String s : args) System.out.println(s);
        config.gameResolution.set(1280,720);
        //config.gameResolution.set(1920,1080);
        config.windowedMode = true;
        config.resizableWindow = true;
        config.vsyncEnabled = true;
        // config.preferredMonitor = "Generic PnP Monitor|344x194mm";
    }

    public void start() throws Exception {

        ui = new JgenGui();
        Font font = Disk.gameLoadFont("assets/font/DMSans-Regular.ttf");
        ui.graphics().addFont(font);
        ui.graphics().bindFont(font.name,1);
        font = Disk.gameLoadFont("assets/font/OpenSans-Regular.ttf");
        ui.graphics().addFont(font);
        ui.graphics().bindFont(font.name,2);
        font = Disk.gameLoadFont("assets/font/BarlowCondensed-Regular.ttf");
        ui.graphics().addFont(font);
        ui.graphics().bindFont(font.name,3);
        font = Disk.gameLoadFont("assets/font/BebasNeue-Regular.ttf");
        ui.graphics().addFont(font);
        ui.graphics().bindFont(font.name,4);
        int y = 0;
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

        if (keys.justPressed(GLFW_KEY_ESCAPE)) Jgen.get().exit();
        // System.out.println(mouse.position().x);
        // System.out.println(Jgen.get().time().fpsEstimate());
    }

    private float accum = 0.0f;
    private int font = 0;
    private int yyy = 0;
    Rectanglef rectOld = new Rectanglef(0,0,256,256);
    public void render() {

        Mouse mouse = Jgen.get().mouse();


        if (mouse.scrolled()) {
            System.out.println(mouse.scrollValue());
        }

        ui.beginFrame();
        int id = ui.state().getID("Quad");
        Color color = quadColor;


        if (ui.state().isHovered(id)) {
            color = Color.YELLOW;
        }
        if (ui.state().isDragged(id,Mouse.LEFT)) {
            float mouseX = mouse.position().x;
            float mouseY = mouse.position().y;
            float x = mouseX - 128;
            float y = mouseY - 128;
            float x2 = mouseX + 128;
            float y2 = mouseY + 128;
            rectOld.setMin(x,y).setMax(x2,y2);
        }

        ui.graphics().draw(color,0, rectOld,id);


        if (Jgen.get().keys().justPressed(GLFW_KEY_UP)) {
            font = JgenMath.wrapi(font + 1, 5);
        } else if (Jgen.get().keys().justPressed(GLFW_KEY_DOWN)) {
            font = JgenMath.wrapi(font - 1, 5);
        }
        if (mouse.scrolled()) {
            if (mouse.scrollValue() < 0) yyy++;
            else yyy--;
        }

        guiTest2();

        ui.graphics().drawString(ui.graphics().boundFont(font).name + ": " + (yyy + 16), 40, Jgen.get().window().gameResolutionHeight() - 40,16,0,Color.GREEN);

        ui.graphics().drawString(
                "/** Full area originally given to this container. */\n" +
                        "    public Rectanglef absoluteBounds(Rectanglef dst) {\n" +
                        "        return dst.setMin(posX,posY).setMax(posX + width, posY + height);\n" +
                        "    }\n" +
                        "    /** Bounds of content allocated so far (includes consumed spacings).\n" +
                        "     * Degenerate (invalid) on the start edge when nothing has been placed. */\n" +
                        "    public Rectanglef contentBounds(Rectanglef dst) {\n" +
                        "        if (axis == Axis.VERTICAL) {\n" +
                        "            dst.minX = posX;\n" +
                        "            dst.maxX = posX + width;\n" +
                        "            if (inverseLayout) {\n" +
                        "                dst.minY = posY;\n" +
                        "                dst.maxY = posY + offset;\n" +
                        "            } else {\n" +
                        "                dst.maxY = posY + height;\n" +
                        "                dst.minY = posY + height - offset;\n" +
                        "            }\n" +
                        "        } else {\n" +
                        "            dst.minY = posY;\n" +
                        "            dst.maxY = posY + height;\n" +
                        "            if (inverseLayout) {\n" +
                        "                dst.maxX = posX + width;\n" +
                        "                dst.minX = posX + width - offset;\n" +
                        "            } else {\n" +
                        "                dst.minX = posX;\n" +
                        "                dst.maxX = posX + offset;\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return dst;\n" +
                        "    }\n" +
                        "    /**\n" +
                        "     * Calculates a bounding rectangle (placement) in a container (without mutating its state).\n" +
                        "     * @param size      Requested main-axis length\n" +
                        "     * @param dst       Output rectangle\n" +
                        "     * @return dst\n" +
                        "     */",
                rectOld.minX, rectOld.maxY,16 + yyy,font,new Color(0.5f,0.5f,0.2f,1.0f));

        Jgen.get().window().presentFrame(ui.endFrame(),true);
    }

    Rectanglef rect = new Rectanglef();

    private boolean modal;

    private void guiTest2() {

        JguiState state = ui.state();
        JguiLayout layout = ui.layoutEngine();;
        JguiStore store = ui.persistentStorage();
        JguiGfx gfx = ui.graphics();

        Color navBarColorDefault = new Color("#222266");
        Color navBarColorHovered = new Color("#666666");
        Color backgroundColor = new Color("#222244AA");
        Color leftPanelColor = new Color("664433");
        Color rightPanelColor = new Color("334466");


        Rectanglef bounds = new Rectanglef();
        Vector2i gameRes = Jgen.get().window().gameResolution(new Vector2i());
        int spacing = 10;
        int navbarHeight = 20;
        int windowWidth = 300;
        int windowHeight = 300 + navbarHeight;
        int modalSize = 40;



        for (int row = 0; row < 2; row++) {
            int winDefaultY = (windowHeight + 40) * row + 40;
            for (int col = 0; col < 2; col++) {
                int winDefaultX = (windowWidth + 40) * col + 40;
                int winID = state.pushID("Window_" + col + "_" + row);

                // Load window absolute position before layout
                Vector2f winPos = store.getVec2f(winID,winDefaultX,winDefaultY);
                if (state.isDragged(winID,Mouse.LEFT)) {
                    winPos.add(state.frameDragDeltaX(),state.frameDragDeltaY());
                } else if (state.justReleasedDrag(winID,Mouse.RIGHT)) {
                    System.out.println("Released Drag Right");
                }
                layout.pushAbsolute(winPos.x,winPos.y,windowWidth,windowHeight,spacing, JguiLayout.Axis.VERTICAL);
                    layout.allocate(navbarHeight,bounds);
                    boolean hovered = state.isHovered(winID) || state.isDragged(winID,Mouse.LEFT);
                    gfx.draw(hovered ? navBarColorHovered : navBarColorDefault,0,bounds, winID);
                    layout.pushRemaining(spacing, JguiLayout.Axis.HORIZONTAL);
                        layout.pushFixed(80,spacing, JguiLayout.Axis.VERTICAL);
                            layout.allocate(30,bounds);
                            int redBox = state.getID("redBox");
                            if (state.justSelected(redBox,Mouse.LEFT)) {
                                modal = !modal;
                            }
                            gfx.draw(Color.RED,0, bounds,redBox);
                            if (modal) {
                                System.out.println("modal");
                                layout.pushAbsolute(gameRes.x - modalSize,gameRes.y - modalSize,modalSize,modalSize, JguiLayout.Axis.VERTICAL);
                                layout.allocateRemaining(bounds);
                                gfx.draw(Color.RED,0,bounds,redBox);
                                layout.pop();
                            }
                            // can add here later. Allocate all in the mean time
                            layout.allocateRemaining(bounds);
                            gfx.draw(leftPanelColor,0,bounds,0);
                        layout.pop(); // Now we're pack in the horizontal axis
                        layout.allocateRemaining(bounds);
                        gfx.draw(rightPanelColor,0,bounds,0);
                    layout.pop();
                layout.pop();

                state.popID(); // pop window ID
            }
        }
    }

    private void guiTest(JgenGui gui) {


        int navBarID = gui.state().pushID("Container");

        // int navBarID = gui.state().getScopeID(); // previosly getCurrentID
        Vector2f conteinerPos = gui.persistentStorage().getVec2f(navBarID,300,300);
        if (gui.state().isDragged(navBarID,Mouse.LEFT)) {
            float deltaX = gui.state().frameDragDeltaX();
            float deltaY = gui.state().frameDragDeltaY();
            conteinerPos.add(deltaX,deltaY);
        }
        gui.layoutEngine().pushAbsolute(conteinerPos.x,conteinerPos.y,256,256, JguiLayout.Axis.VERTICAL);

        Rectanglef navBarBounds = gui.layoutEngine().allocate(20,rect);
        boolean navBarHovered = gui.state().isHovered(navBarID) || gui.state().isDragged(navBarID,Mouse.LEFT);
        gui.graphics().draw(navBarHovered ? Color.YELLOW : Color.RED,0,navBarBounds,navBarID);
        Rectanglef contentBounds = gui.layoutEngine().allocateRemaining(rect);
        int contentID = gui.state().getID(0);
        boolean contentHovered = gui.state().isHovered(contentID);
        gui.graphics().draw(contentHovered ? Color.GREEN : Color.CYAN,0,contentBounds,contentID);

        gui.layoutEngine().pop();
        gui.state().popID();

    }

    public void exit() {
        Disposable.free(ui);
    }
}
