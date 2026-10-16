package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.Mouse;
import org.joml.*;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;

import java.lang.Math;
import java.util.function.Supplier;

/**
 * F.Dahl, 9/14/2026
 */
public interface JuiState {


    int NULL                = 0;
    int DRAG_THRESHOLD      = 4;
    long DOUBLE_CLICK_THRESHOLD_NS = 300_000_000L;

    int MOUSE_NONE          = -1;
    int MOUSE_LEFT          = Mouse.LEFT;
    int MOUSE_RIGHT         = Mouse.RIGHT;
    int MOUSE_WHEEL         = Mouse.WHEEL;
    int MOUSE_BUTTONS       = Mouse.NUM_BUTTONS;

    int NAV_UP              = 1;
    int NAV_RIGHT           = 2;
    int NAV_DOWN            = 3;
    int NAV_LEFT            = 4;
    int NAV_START           = 5;
    int NAV_END             = 6;
    int NAV_SELECT          = 7;
    int NAV_EXIT            = 8;

    int HOVERED             = 1;
    int HOVERED_JUST        = 1 << 1;
    int PRESSED             = 1 << 2;
    int PRESSED_L           = 1 << 3;
    int PRESSED_R           = 1 << 4;
    int PRESSED_W           = 1 << 5;
    int PRESSED_JUST        = 1 << 6;
    int CLICKED             = 1 << 7;
    int CLICKED_L           = 1 << 8;
    int CLICKED_R           = 1 << 9;
    int CLICKED_W           = 1 << 10;
    int DRAGGED             = 1 << 11;
    int DRAGGED_L           = 1 << 12;
    int DRAGGED_R           = 1 << 13;
    int DRAGGED_W           = 1 << 14;
    int DRAGGED_JUST        = 1 << 15;
    int FOCUSED             = 1 << 16;
    int FOCUSED_JUST        = 1 << 17;
    int FOCUSED_LOST        = 1 << 18;
    int RELEASED_PRESS      = 1 << 19;
    int RELEASED_PRESS_L    = 1 << 20;
    int RELEASED_PRESS_R    = 1 << 21;
    int RELEASED_PRESS_W    = 1 << 22;
    int RELEASED_DRAG       = 1 << 23;
    int RELEASED_DRAG_L     = 1 << 24;
    int RELEASED_DRAG_R     = 1 << 25;
    int RELEASED_DRAG_W     = 1 << 26;
    int DOUBLE_CLICKED      = 1 << 27;
    int DOUBLE_CLICKED_L    = 1 << 28;
    int DOUBLE_CLICKED_R    = 1 << 29;
    int DOUBLE_CLICKED_W    = 1 << 30;


    int currentHoveredID();
    int currentPressedID();
    int currentDraggedID();
    int currentClickedID();
    int currentDoubleClicked();
    int currentFocusedID();

    int lastFrameHoveredID();
    int lastFramePressedID();
    int lastFrameDraggedID();
    int lastFrameFocusedID();

    int navigationBtn();
    int mouseActiveBtn();
    int mouseLastActiveBtn();

    float hoveredDuration();
    float pressedDuration();
    float focusedDuration();
    /** Max(pressed,hovered)*/
    default float pressedHoveredDuration() {
        return Math.max(hoveredDuration(),pressedDuration());
    }

    float mouseScroll();
    float mousePosX();
    float mousePosY();
    float mousePressOriginX();
    float mousePressOriginY();
    float mouseFrameDeltaX();
    float mouseFrameDeltaY();
    float mouseDragVectorX();
    float mouseDragVectorY();


    default Vector2f mousePos(Vector2f dst) { return dst.set(mousePosX(), mousePosY()); }
    default Vector2f mousePressOrigin(Vector2f dst) { return dst.set(mousePressOriginX(), mousePressOriginY()); }
    default Vector2f mouseFrameDelta(Vector2f dst) { return dst.set(mouseFrameDeltaX(), mouseFrameDeltaY()); }
    default Vector2f mouseDragVector(Vector2f dst) { return dst.set(mouseDragVectorX(), mouseDragVectorY()); }


    default boolean isPressedOrHovered(int id) { return isHovered(id) || isPressed(id); }
    default boolean isPressedOrHovered(int id, int mouseBtn) { return isHovered(id) || isPressed(id,mouseBtn); }

    default boolean isHovered(int id) { return isValid(id) && isHoveredRaw(id); }
    default boolean isPressed(int id) {
        boolean pressed = isValid(id) && currentPressedID() == id;
        if (pressed) windowClaimFocus();
        return pressed;
    }
    default boolean isPressed(int id, int mouseBtn) { return isPressed(id) && mouseActiveBtn() == mouseBtn; }
    default boolean isDragged(int id) {
        boolean dragged = isValid(id) && currentDraggedID() == id;
        if (dragged) windowClaimFocus();
        return dragged;

    }
    default boolean isDragged(int id, int mouseBtn) { return isDragged(id) && mouseActiveBtn() == mouseBtn; }
    default boolean isFocused(int id) {
        boolean focused = isValid(id) && currentFocusedID() == id;
        if (focused) windowClaimFocus();
        return focused;
    }

    // aby
    default boolean anyHovered() { return currentHoveredID() != NULL; }
    default boolean anyPressed() { return currentPressedID() != NULL; }
    default boolean anyDragged() { return currentDraggedID() != NULL; }
    default boolean anyJustClicked() { return currentClickedID() != NULL; }
    default boolean anyDoubleClicked() { return currentDoubleClicked() != NULL; }
    default boolean anyFocused() { return currentFocusedID() != NULL; }

    // impulses
    default boolean justScrolled() { return mouseScroll() != 0.0f; }
    default boolean justHovered(int id) { return isHovered(id) && !wasHoveredRaw(id); }
    default boolean justPressed(int id) { return isPressed(id) && currentPressedID() != lastFramePressedID(); }
    default boolean justPressed(int id, int mouseBtn) { return isPressed(id, mouseBtn) && currentPressedID() != lastFramePressedID(); }
    default boolean justDragged(int id) { return isDragged(id) && lastFrameDraggedID() != id; }
    default boolean justDragged(int id, int mouseBtn) { return justDragged(id) && mouseActiveBtn() == mouseBtn; }
    default boolean justClicked(int id) {
        boolean clicked =  isValid(id) && currentClickedID() == id;
        if (clicked) windowClaimFocus();
        return clicked;
    }

    default boolean justDoubleClicked(int id) {
        boolean clicked =  isValid(id) && currentDoubleClicked() == id;
        if (clicked) windowClaimFocus();
        return clicked;
    }

    // todo just scolled etc. (consume scrollamount?) --------------------------------------

    default boolean justDoubleClicked(int id, int mouseBtn) { return justDoubleClicked(id) && mouseActiveBtn() == mouseBtn; }
    default boolean justClicked(int id, int mouseBtn) { return justClicked(id) && mouseLastActiveBtn() == mouseBtn; }
    default boolean justFocused(int id) { return isFocused(id) && currentFocusedID() != lastFrameFocusedID(); }
    default boolean justLostFocus(int id) { return isValid(id) && lastFrameFocusedID() == id && currentFocusedID() != lastFrameFocusedID(); }
    default boolean justReleased(int id) { return isValid(id) && lastFramePressedID() == id && currentPressedID() != id; }
    default boolean justReleased(int id, int mouseBtn) { return justReleased(id) && mouseLastActiveBtn() == mouseBtn; }
    default boolean justReleasedDrag(int id) { return isValid(id) && lastFrameDraggedID() == id && currentDraggedID() != id; }
    default boolean justReleasedDrag(int id, int mouseBtn) { return justReleasedDrag(id) && mouseLastActiveBtn() == mouseBtn; }

    // impulse Keyboard / Gamepad menu navigation
    default boolean navigatedUp() { return navigationBtn() == NAV_UP; }
    default boolean navigatedUp(int id) { return isFocused(id) && navigatedUp(); }
    default boolean navigateRight() { return navigationBtn() == NAV_RIGHT; }
    default boolean navigateRight(int id) { return isFocused(id) && navigateRight(); }
    default boolean navigateDown() { return navigationBtn() == NAV_DOWN; }
    default boolean navigateDown(int id) { return isFocused(id) && navigateDown(); }
    default boolean navigateLeft() { return navigationBtn() == NAV_LEFT; }
    default boolean navigateLeft(int id) { return isFocused(id) && navigateLeft(); }
    default boolean navigateToStart() { return navigationBtn() == NAV_START; }
    default boolean navigateToStart(int id) { return isFocused(id) && navigateToStart(); }
    default boolean navigateToEnd() { return navigationBtn() == NAV_END; }
    default boolean navigateToEnd(int id) { return isFocused(id) && navigateToEnd(); }
    default boolean navigateSelect() { return navigationBtn() == NAV_SELECT; }
    default boolean navigateSelect(int id) { return isFocused(id) && navigateSelect(); }
    default boolean navigateExit() { return navigationBtn() == NAV_EXIT; }
    default boolean navigateExit(int id) { return isFocused(id) && navigateExit(); }


    void focusSteal(int id);
    void focusYield();
    default void focusYield(int id) { if (isFocused(id)) focusYield(); }

    private boolean isValid(int id) { return id != NULL; }
    private boolean isHoveredRaw(int id) { return currentHoveredID() == id && isMouseAvailableFor(id); }
    private boolean wasHoveredRaw(int id) { return lastFrameHoveredID() == id && wasMouseAvailableFor(id); }
    private boolean isMouseAvailableFor(int id) { return currentPressedID() == NULL || currentPressedID() == id; }
    private boolean wasMouseAvailableFor(int id) { return lastFramePressedID() == NULL || lastFramePressedID() == id; }

    default int stateOf(int id) {
        if (!isValid(id)) return 0;
        int mask = 0;
        if (isHoveredRaw(id)) {
            mask |= HOVERED;
            if (!wasHoveredRaw(id)) {
                mask |= HOVERED_JUST;
            }
        }
        if (currentPressedID() == id) {
            mask |= PRESSED;
            if (currentPressedID() != lastFramePressedID()) {
                mask |= PRESSED_JUST;
            }
            switch (mouseActiveBtn()) {
                case MOUSE_LEFT  ->  mask |= PRESSED_L;
                case MOUSE_RIGHT ->  mask |= PRESSED_R;
                case MOUSE_WHEEL ->  mask |= PRESSED_W;
            }
            if (currentDraggedID() == id) {
                mask |= DRAGGED;
                if (lastFrameDraggedID() != id) {
                    mask |= DRAGGED_JUST;
                }
                switch (mouseActiveBtn()) {
                    case MOUSE_LEFT  ->  mask |= DRAGGED_L;
                    case MOUSE_RIGHT ->  mask |= DRAGGED_R;
                    case MOUSE_WHEEL ->  mask |= DRAGGED_W;
                }
            }
            if (currentDoubleClicked() == id) {
                mask |= DOUBLE_CLICKED;
                switch (mouseActiveBtn()) {
                    case MOUSE_LEFT  ->  mask |= DOUBLE_CLICKED_L;
                    case MOUSE_RIGHT ->  mask |= DOUBLE_CLICKED_R;
                    case MOUSE_WHEEL ->  mask |= DOUBLE_CLICKED_W;
                }
            }
        } else if (lastFramePressedID() == id) {
            mask |= RELEASED_PRESS;
            switch (mouseLastActiveBtn()) {
                case MOUSE_LEFT  ->  mask |= RELEASED_PRESS_L;
                case MOUSE_RIGHT ->  mask |= RELEASED_PRESS_R;
                case MOUSE_WHEEL ->  mask |= RELEASED_PRESS_W;
            }
            if (lastFrameDraggedID() == id) {
                mask |= RELEASED_DRAG;
                switch (mouseLastActiveBtn()) {
                    case MOUSE_LEFT  ->  mask |= RELEASED_DRAG_L;
                    case MOUSE_RIGHT ->  mask |= RELEASED_DRAG_R;
                    case MOUSE_WHEEL ->  mask |= RELEASED_DRAG_W;
                }
            }
        }
        if (currentClickedID() == id) {
            mask |= CLICKED;
            switch (mouseLastActiveBtn()) {
                case MOUSE_LEFT  ->  mask |= CLICKED_L;
                case MOUSE_RIGHT ->  mask |= CLICKED_R;
                case MOUSE_WHEEL ->  mask |= CLICKED_W;
            }
        }
        if (currentFocusedID() == id) {
            mask |= FOCUSED;
            if (currentFocusedID() != lastFrameFocusedID()) {
                mask |= FOCUSED_JUST;
            }
        } else if (lastFrameFocusedID() == id) {
            mask |= FOCUSED_LOST;
        }
        return mask;
    }

    // --- FNV-1a Hash Constants ---
    int FNV_OFFSET_32   = 0x811c9dc5;
    int FNV_PRIME_32    = 0x01000193;
    int HASH_SENTINEL   = 1;



    /** Returns current parent scope seed at top of stack, or FNV basis if root */
    int scopeID();
    /** Derive a unique ID under current stack scope for an integer index */
    default int getID(int key) { return hash(key, scopeID()); }
    /** Derive a unique ID under current stack scope for a String key */
    default int getID(String key) { return hash(key, scopeID()); }
    /** Pushes a String scope onto the stack, combining it with the parent seed.
     * @return the new scope ID */
    int pushID(String scope);
    /** Pushes an integer scope onto the stack, combining it with the parent seed.
     * @return the new scope ID */
    int pushID(int scope);
    /** Pops the active scope off the stack */
    void popID();

    static int hash(String str, int seed) {
        int hash = seed;
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            hash = (hash ^ (c & 0xFF)) * FNV_PRIME_32;
            hash = (hash ^ ((c >>> 8) & 0xFF)) * FNV_PRIME_32;
        } return hash == NULL ? HASH_SENTINEL : hash;
    }

    static int hash(int value, int seed) {
        int hash = seed;
        hash = (hash ^ (value & 0xFF)) * FNV_PRIME_32;
        hash = (hash ^ ((value >>> 8) & 0xFF)) * FNV_PRIME_32;
        hash = (hash ^ ((value >>> 16) & 0xFF)) * FNV_PRIME_32;
        hash = (hash ^ ((value >>> 24) & 0xFF)) * FNV_PRIME_32;
        return hash == NULL ? HASH_SENTINEL : hash;
    }


    <T extends JuiWindow> T windowGet(String name, Class<T> clazz);
    JuiWindow windowGet(String name);
    void windowRegister(JuiWindow window, int layer);
    void windowOpen(String name);
    void windowClose(String name);
    void windowClaimFocus();
    void windowLayerShow(int layer);
    void windowLayerHide(int layer);
    boolean windowLayerVisible(int layer);


    int getInt(int id, int defaultValue);
    long getLong(int id, long defaultValue);
    float getFloat(int id, float defaultValue);
    double getDouble(int id, double defaultValue);
    boolean getBool(int id, boolean defaultValue);
    Vector2f getVec2f(int id, float x, float y);
    Vector3f getVec3f(int id, float x, float y, float z);
    Vector4f getVec4f(int id, float x, float y, float z, float w);
    Vector2i getVec2i(int id, int x, int y);
    Vector3i getVec3i(int id, int x, int y, int z);
    Vector4i getVec4i(int id, int x, int y, int z, int w);
    Rectanglef getRectf(int id, float minX, float minY, float maxX, float maxY);
    Rectanglei getRecti(int id, int minX, int minY, int maxX, int maxY);
    <T> T getObj(int id, Class<T> clazz, Supplier<T> supplier);
    void persistentPut(int id, Object object);
    void persistentRemove(int id);
    void persistentClear();
    boolean persistentContains(int id);

}
