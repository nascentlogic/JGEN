package io.github.nascentlogic.jgen;

import org.joml.Vector2d;
import org.joml.Vector2i;
import org.joml.Vector4i;

/**
 * F.Dahl, 6/7/2026
 */
public class Mouse {


    private final Window window;

    private Vector2d cursorScreen;
    private Vector2d cursorFramebuffer;
    private Vector2d cursorViewportClamped;
    private Vector2d cursorViewportNormalized;



    Mouse(Window window) {
        this.window = window;
    }


    void processInput() {

    }

    void onCursorHover(double cx, double cy) {

    }

    void onButtonPress(int button, boolean press) {

    }

    void onWheelScroll(double amount) {

    }

    void onCursorEnter(boolean entered) {

    }





    /** Convert from screen to framebuffer space. (Y is inverted in this operation) */
    private void screenToFramebuffer(Vector2d cursorScreen, Vector2i framebufferSize, Vector2i windowSize, Vector2d dst) {
        if (windowSize.x == 0 || windowSize.y == 0) {
            dst.set(0.0, 0.0);
            return;
        }
        double normX = cursorScreen.x / windowSize.x;
        double normY = (windowSize.y - cursorScreen.y) / windowSize.y;
        double framebufferX = normX * framebufferSize.x;
        double framebufferY = normY * framebufferSize.y;
        dst.set(framebufferX,framebufferY);
    }

    /** Convert from framebuffer space to actual viewport pixel space. */
    private void framebufferToViewport(Vector2d cursorFramebuffer, Vector4i viewport, Vector2d dst) {
        double viewportPixelX = cursorFramebuffer.x - viewport.x;
        double viewportPixelY = cursorFramebuffer.y - viewport.y;
        dst.set(viewportPixelX,viewportPixelY);
    }

    /** Convert from viewport pixel space to normalized coordinates (0.0 to 1.0). */
    private void viewportToNormalized(Vector2d cursorViewport, Vector4i viewport, Vector2d dst) {
        if (viewport.z == 0 || viewport.w == 0) {
            dst.set(0.0, 0.0);
            return;
        }
        double normX = cursorViewport.x / viewport.z;
        double normY = cursorViewport.y / viewport.w;
        dst.set(normX, normY);
    }


}
