package io.github.nascentlogic.jgen;

import org.joml.*;
import org.joml.Math;
import org.joml.primitives.Rectanglef;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * F.Dahl, 6/7/2026
 */
public class Mouse {

    public static final int NUM_BUTTONS = 3;
    public static final int LEFT  = GLFW_MOUSE_BUTTON_LEFT;
    public static final int RIGHT = GLFW_MOUSE_BUTTON_RIGHT;
    public static final int WHEEL = GLFW_MOUSE_BUTTON_MIDDLE;
    public static final double DRAG_TIME = (5.0/60.0);

    private final double[] timer = new double[NUM_BUTTONS];
    private final boolean[] buttonLast = new boolean[NUM_BUTTONS];
    private final boolean[] buttonCurrent = new boolean[NUM_BUTTONS];
    private final boolean[] buttonCallback = new boolean[NUM_BUTTONS];
    private final boolean[] draggingCurrent = new boolean[NUM_BUTTONS];
    private final boolean[] draggingLast = new boolean[NUM_BUTTONS];
    private final boolean[] dragEligible = new boolean[NUM_BUTTONS];

    private final Window window;
    private final Vector2f positionRaw = new Vector2f();
    private final Vector2f positionLast = new Vector2f();
    private final Vector2f positionCurrent = new Vector2f();
    private final Vector2f[] dragOrigin = new Vector2f[NUM_BUTTONS];
    private final Vector2f[] dragVector = new Vector2f[NUM_BUTTONS];

    private boolean cursorInWindow;
    private boolean cursorInViewport;
    private float scrollCallback;
    private float scrollCurrent;


    Mouse(Window glfwWindow) {
        window = glfwWindow;
        cursorInWindow = glfwGetWindowAttrib(window.handle(), GLFW_HOVERED) == GLFW_TRUE;
        cursorPositionRaw(positionRaw);
        positionCurrent.set(positionRaw);
        Vector2i framebufferRes = window.framebufferResolution();
        Vector2i gameResolution = window.gameResolution();
        Vector2i windowSize = window.windowSize();
        Vector4i viewport = window.viewport();
        screenToFramebuffer(positionCurrent,framebufferRes,windowSize, positionCurrent);
        framebufferToViewport(positionCurrent,viewport, positionCurrent);
        viewportToNormalized(positionCurrent,viewport, positionCurrent);
        positionCurrent.mul(gameResolution.x,gameResolution.y);
        for (int i = 0; i < NUM_BUTTONS; i++) {
            dragOrigin[i] = new Vector2f();
            dragVector[i] = new Vector2f();
        }
    }

    void processInput() {
        positionLast.set(positionCurrent);
        positionCurrent.set(positionRaw);
        scrollCurrent = scrollCallback;
        scrollCallback = 0.0f;
        Vector2i framebufferRes = window.framebufferResolution();
        Vector2i gameResolution = window.gameResolution();
        Vector2i windowSize = window.windowSize();
        Vector4i viewport = window.viewport();
        screenToFramebuffer(positionCurrent,framebufferRes,windowSize, positionCurrent);
        cursorInViewport = cursorInsideViewport(positionCurrent,viewport) && cursorInWindow;
        framebufferToViewport(positionCurrent,viewport, positionCurrent);
        viewportToNormalized(positionCurrent,viewport, positionCurrent); // clamps here
        positionCurrent.mul(gameResolution.x,gameResolution.y);
        double dt = Jgen.get().time().deltaTime();
        for (int b = 0; b < NUM_BUTTONS; b++) {
            draggingLast[b] = draggingCurrent[b];
            buttonLast[b] = buttonCurrent[b];
            buttonCurrent[b] = buttonCallback[b];
            if (buttonCurrent[b]) {
                timer[b] += dt;
                if (!buttonLast[b]) {
                    dragOrigin[b].set(positionCurrent);
                    dragVector[b].zero();
                } else {
                    dragVector[b].set(positionCurrent).sub(dragOrigin[b]);
                    if (!draggingCurrent[b]) {
                        if (timer[b] > DRAG_TIME && dragVector[b].length() > 8.0) {
                            draggingCurrent[b] = true;
                        }
                    }
                }
            } else {
                timer[b] = 0f;
                if (draggingCurrent[b]) {
                    draggingCurrent[b] = false;
                    dragVector[b].zero();
                }
            }
        }
    }

    void onWheelScroll(double amount) {
        scrollCallback += (float)amount;
    }

    void onButtonPress(int button, boolean press) {
        if (button >= 0 && button < NUM_BUTTONS) buttonCallback[button] = press;
    }

    void onCursorHover(double cx, double cy) {
        positionRaw.set((float)cx,(float)cy);
    }

    void onCursorEnter(boolean entered) {
        cursorInWindow = entered;
        if (!cursorInWindow) {
            try (MemoryStack stack = MemoryStack.stackPush()){
                DoubleBuffer cx = stack.mallocDouble(1);
                DoubleBuffer cy = stack.mallocDouble(1);
                glfwGetCursorPos(window.handle(),cx,cy);
                positionRaw.set((float)cx.get(0),(float)cy.get(0));
            }
        }
    }

    public Rectanglef dragArea(int button, Rectanglef dst) {
        if (isDragging(button)) {
            Vector2f p0 = dragOrigin[button];
            float p1x = p0.x + dragVector[button].x;
            float p1y = p0.y + dragVector[button].y;
            dst.minX = Math.min(p0.x,p1x);
            dst.minY = Math.min(p0.y,p1y);
            dst.maxX = Math.max(p0.x,p1x);
            dst.maxY = Math.max(p0.y,p1y);
        } else {
            dst.minX = 0; dst.minY = 0;
            dst.maxX = 0; dst.maxY = 0;
        } return dst;
    }

    public Vector2f position() {
        return positionCurrent;
    }

    public Vector2f dragVector(int button) {
        return dragVector[button];
    }

    public Vector2f dragOrigin(int button) {
        return dragOrigin[button];
    }

    public boolean scrolled() {
        return scrollCurrent != 0.0f;
    }

    public float scrollValue() {
        return scrollCurrent;
    }

    public boolean cursorInViewport() {
        return cursorInViewport;
    }

    public boolean cursorInWindow() {
        return cursorInWindow;
    }

    public boolean justPressed(int button) {
        return buttonCurrent[button] && !buttonLast[button];
    }

    public boolean justReleased(int button) {
        return !buttonCurrent[button] && buttonLast[button];
    }

    public boolean pressed(int button) {
        return buttonCurrent[button];
    }

    public boolean isDragging(int button) {
        return draggingCurrent[button];
    }

    public boolean justStartedDrag(int button) {
        return draggingCurrent[button] && !draggingLast[button];
    }

    public boolean justReleasedDrag(int button) {
        return !draggingCurrent[button] && draggingLast[button];
    }

    public boolean anyButtonPressed() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            if (buttonCurrent[i]) return true;
        } return false;
    }

    public boolean anyButtonDragging() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            if (draggingCurrent[i]) return true;
        } return false;
    }




    /** Convert from screen to framebuffer space. cursorScreen can == dst. (Y is inverted in this operation) */
    private void screenToFramebuffer(Vector2f cursorScreen, Vector2i framebufferRes, Vector2i windowSize, Vector2f dst) {
        if (windowSize.x == 0 || windowSize.y == 0) {
            dst.set(0.0f, 0.0f);
            return;
        }
        float normX = cursorScreen.x / windowSize.x;
        float normY = (windowSize.y - cursorScreen.y) / windowSize.y;
        float framebufferX = normX * framebufferRes.x;
        float framebufferY = normY * framebufferRes.y;
        dst.set(framebufferX,framebufferY);
    }

    /** Convert from framebuffer space to actual viewport pixel space. cursorFramebuffer can == dst. */
    private void framebufferToViewport(Vector2f cursorFramebuffer, Vector4i viewport, Vector2f dst) {
        float viewportPixelX = cursorFramebuffer.x - viewport.x;
        float viewportPixelY = cursorFramebuffer.y - viewport.y;
        dst.set(viewportPixelX,viewportPixelY);
    }

    /** Convert from viewport pixel space to normalized coordinates clamp(0.0 to 1.0). cursorViewport can == dst. */
    private void viewportToNormalized(Vector2f cursorViewport, Vector4i viewport, Vector2f dst) {
        if (viewport.z == 0 || viewport.w == 0) {
            dst.set(0.0f, 0.0f);
            return;
        }
        float normX = Math.clamp(0.0f,1.0f,cursorViewport.x / viewport.z);
        float normY = Math.clamp(0.0f,1.0f,cursorViewport.y / viewport.w);
        dst.set(normX, normY);
    }


    private boolean cursorInsideViewport(Vector2f cursorFrambuffer, Vector4i viewport) {
        boolean insideMin = cursorFrambuffer.x >= viewport.x && cursorFrambuffer.x <= (viewport.x + viewport.z);
        boolean insideMax = cursorFrambuffer.y >= viewport.y && cursorFrambuffer.y <= (viewport.y + viewport.w);
        return insideMin && insideMax;
    }

    private void cursorPositionRaw(Vector2f dst) {
        try (MemoryStack stack = MemoryStack.stackPush()){
            DoubleBuffer cx = stack.mallocDouble(1);
            DoubleBuffer cy = stack.mallocDouble(1);
            glfwGetCursorPos(window.handle(),cx,cy);
            dst.set((float)cx.get(0),(float)cy.get(0));
        }
    }


}
