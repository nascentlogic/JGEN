package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.Gamepads;
import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.Keyboard;
import io.github.nascentlogic.jgen.Mouse;
import org.lwjgl.glfw.GLFW;

/**
 * F.Dahl, 9/5/2026
 */
public class JguiState {

    // todo release
    public static final int NONE           = 0;
    public static final int HOVERED        = 1;
    public static final int HOVERED_JUST   = 1 << 1;
    public static final int PRESSED        = 1 << 2;
    public static final int PRESSED_L      = 1 << 3;
    public static final int PRESSED_R      = 1 << 4;
    public static final int PRESSED_W      = 1 << 5;
    public static final int PRESSED_JUST   = 1 << 6;
    public static final int SELECTED       = 1 << 7;
    public static final int SELECTED_L     = 1 << 8;
    public static final int SELECTED_R     = 1 << 9;
    public static final int SELECTED_W     = 1 << 10;
    public static final int DRAGGED        = 1 << 11;
    public static final int DRAGGED_L      = 1 << 12;
    public static final int DRAGGED_R      = 1 << 13;
    public static final int DRAGGED_W      = 1 << 14;
    public static final int DRAGGED_JUST   = 1 << 15;
    public static final int FOCUSED        = 1 << 16;
    public static final int FOCUSED_JUST   = 1 << 17;
    public static final int FOCUSED_LOST   = 1 << 18;

    public static final int NAV_UP         = 1;
    public static final int NAV_RIGHT      = 2;
    public static final int NAV_DOWN       = 3;
    public static final int NAV_LEFT       = 4;

    private int navigation = NONE;


    public static final int NO_ID = 0;
    public static final int CURSOR_DRAG_TRESHOOLD = 4;

    private static final int ID_STACK_CAPACITY = 64;
    private final int[] idStack = new int[ID_STACK_CAPACITY];
    private int stackDepth = 0;
    private int currentWindowID = NO_ID;

    // flags + scrolled + navigated
    private int hoveredID = NO_ID;
    private int pressedID = NO_ID;
    private int selectedID = NO_ID;
    private int draggedID = NO_ID;
    private int focusedID = NO_ID;
    private int focusRequestID = NO_ID;

    private int lastHoveredID = NO_ID;
    private int lastPressedID = NO_ID;
    private int lastDraggedID = NO_ID;
    private int lastFocusedID = NO_ID;

    public static final int MOUSE_INACTIVE = -1;
    private int activeMouseBtn = MOUSE_INACTIVE;
    private int lastActiveMouseBtn = MOUSE_INACTIVE;

    private long hoveredDurationNS = 0L;
    private long pressedDurationNS = 0L;
    private long focusedDurationNS = 0L;

    private float pressStartX, pressStartY;
    private float currentMouseX, currentMouseY;
    private float lastMouseX, lastMouseY;
    private float frameDeltaX, frameDeltaY;
    private float totalDeltaX, totalDeltaY;

    protected void reset() {
        stackDepth = 0;
        currentWindowID = NO_ID;

        navigation = NONE;

        hoveredID = NO_ID;
        pressedID = NO_ID;
        selectedID = NO_ID;
        draggedID = NO_ID;
        focusedID = NO_ID;
        focusRequestID = NO_ID;

        lastHoveredID = NO_ID;
        lastPressedID = NO_ID;
        lastFocusedID = NO_ID;
        lastDraggedID = NO_ID;

        activeMouseBtn = MOUSE_INACTIVE;
        lastActiveMouseBtn = MOUSE_INACTIVE;

        hoveredDurationNS = 0L;
        pressedDurationNS = 0L;
        focusedDurationNS = 0L;

        pressStartX = 0.0f;
        pressStartY = 0.0f;
        currentMouseX = 0.0f;
        currentMouseY = 0.0f;
        lastMouseX = 0.0f;
        lastMouseY = 0.0f;
        frameDeltaX = 0.0f;
        frameDeltaY = 0.0f;
        totalDeltaX = 0.0f;
        totalDeltaY = 0.0f;
    }

    void tick(int pixelID) {
        if (stackDepth != 0) throw new IllegalStateException("Mismatched pushId/popId! Stack depth at end of last frame was " + stackDepth);
        Mouse mouse = Jgen.get().mouse();
        Keyboard keys = Jgen.get().keys();
        Gamepads gamepads = Jgen.get().gamepads();
        // =============================================================================
        // FRAME RESET
        // =============================================================================
        stackDepth = 0;

        // Preserve previous frame state snapshot
        lastHoveredID = hoveredID;
        lastPressedID = pressedID;
        lastFocusedID = focusedID;
        lastDraggedID = draggedID;
        lastActiveMouseBtn = activeMouseBtn;

        // Reset single-frame pulse events
        navigation = NONE;
        selectedID = NO_ID;
        hoveredID = pixelID;

        lastMouseX = currentMouseX;
        lastMouseY = currentMouseY;
        currentMouseX = mouse.position().x();
        currentMouseY = mouse.position().y();
        frameDeltaX = currentMouseX - lastMouseX;
        frameDeltaY = currentMouseY - lastMouseY;


        // Apply deferred focus requests
        if (focusedID != focusRequestID) {
            focusedID = focusRequestID;
        }
        // =============================================================================
        // RELEASE / END INTERACTION
        // =============================================================================
        if (pressedID != NO_ID && activeMouseBtn != MOUSE_INACTIVE) {
            if (mouse.justReleased(activeMouseBtn)) {
                // Register a selection click ONLY if it wasn't converted into a drag operation
                if (hoveredID == pressedID && draggedID == NO_ID) {
                    selectedID = pressedID;
                }
                draggedID = NO_ID;
                pressedID = NO_ID;
                activeMouseBtn = MOUSE_INACTIVE;
            }
        }
        // =============================================================================
        // CAPTURE NEW PRESS
        // =============================================================================
        if (pressedID == NO_ID && hoveredID != NO_ID) {
            for (int button = 0; button < Mouse.NUM_BUTTONS; button++) {
                if (mouse.justPressed(button)) {
                    pressedID = hoveredID;
                    activeMouseBtn = button;
                    pressStartX = currentMouseX;
                    pressStartY = currentMouseY;
                    totalDeltaX = 0f;
                    totalDeltaY = 0f;
                    break;
                }
            }
        }
        // =============================================================================
        // DRAG THRESHOLD & DELTA EVALUATION
        // =============================================================================
        if (pressedID != NO_ID) {
            totalDeltaX = currentMouseX - pressStartX;
            totalDeltaY = currentMouseY - pressStartY;
            if (draggedID == NO_ID) {
                float distSq = (totalDeltaX * totalDeltaX) + (totalDeltaY * totalDeltaY);
                if (distSq >= (CURSOR_DRAG_TRESHOOLD * CURSOR_DRAG_TRESHOOLD)) {
                    draggedID = pressedID;
                }
            }
        } else {
            totalDeltaX = 0f;
            totalDeltaY = 0f;
        }
        // =============================================================================
        // KEYBOARD / GAMEPAD SELECTION
        // =============================================================================
        // todo: scroll
        if (focusedID != NO_ID) {
            if (keys.justPressed(GLFW.GLFW_KEY_ENTER)) {
                selectedID = focusedID;
            } else if (keys.justPressed(GLFW.GLFW_KEY_UP)) {
                navigation = NAV_UP;
            } else if (keys.justPressed(GLFW.GLFW_KEY_RIGHT)) {
                navigation = NAV_RIGHT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_DOWN)) {
                navigation = NAV_DOWN;
            } else if (keys.justPressed(GLFW.GLFW_KEY_LEFT)) {
                navigation = NAV_LEFT;
            }
        }
        // =============================================================================
        // DURATION TRACKING
        // =============================================================================
        long deltaTimeNS = Jgen.get().time().deltaTimeNS();
        hoveredDurationNS = (hoveredID != NO_ID && hoveredID == lastHoveredID) ? hoveredDurationNS + deltaTimeNS : 0L;
        pressedDurationNS = (pressedID != NO_ID && pressedID == lastPressedID) ? pressedDurationNS + deltaTimeNS : 0L;
        focusedDurationNS = (focusedID != NO_ID && focusedID == lastFocusedID) ? focusedDurationNS + deltaTimeNS : 0L;
    }

    // =============================================================================
    // CURSOR HOVER / PRESS + HELPER
    // =============================================================================
    public int hoveredID() { return hoveredID; }
    public boolean isHovered(int id) { return isValid(id) && isHoveredRaw(id); }
    public boolean justHovered(int id) { return isHovered(id) && !wasHoveredRaw(id); }
    public float hoveredDuration() { return (float) (hoveredDurationNS / 1_000_000_000d); }
    public int pressedID() { return pressedID; }
    public int activeMouseBtn() { return activeMouseBtn; }
    public boolean isPressed(int id) { return isValid(id) && pressedID == id; }
    public boolean isPressed(int id, int mouseBtn) { return isPressed(id) && activeMouseBtn == mouseBtn; }
    public boolean justPressed(int id) { return isPressed(id) && pressedID != lastPressedID; }
    public boolean justPressed(int id, int mouseBtn) { return isPressed(id, mouseBtn) && pressedID != lastPressedID; }
    public float pressedDuration() { return (float) (pressedDurationNS / 1_000_000_000d); }
    private boolean isValid(int id) { return id != NO_ID; }
    private boolean isHoveredRaw(int id) { return hoveredID == id && isMouseAvailableFor(id); }
    private boolean wasHoveredRaw(int id) { return lastHoveredID == id && wasMouseAvailableFor(id); }
    private boolean isMouseAvailableFor(int id) { return pressedID == NO_ID || pressedID == id; }
    private boolean wasMouseAvailableFor(int id) { return lastPressedID == NO_ID || lastPressedID == id; }
    // =============================================================================
    // CURSOR DRAGGING
    // =============================================================================
    public int draggedID() { return draggedID; }
    public boolean isDragging() { return draggedID != NO_ID; }
    public boolean isDragged(int id) { return isValid(id) && draggedID == id; }
    public boolean isDragged(int id, int mouseBtn) { return isDragged(id) && activeMouseBtn == mouseBtn; }

    public boolean justReleased(int id, int mouseBtn) {
        return justReleased(id) && lastActiveMouseBtn == mouseBtn;
    }


    public boolean justReleased(int id) { return isValid(id) && lastPressedID == id && pressedID != id; }
    public boolean justStardedDrag(int id) { return isDragged(id) && lastDraggedID != id; }
    public boolean justReleasedDrag(int id) { return isValid(id) && lastDraggedID == id && draggedID != id; }
    public boolean justReleasedDrag(int id, int mouseBtn) { return justReleasedDrag(id) && lastActiveMouseBtn == mouseBtn; }
    public float totalDragDeltaX() { return totalDeltaX; }
    public float totalDragDeltaY() { return totalDeltaY; }
    public float frameDragDeltaX() { return frameDeltaX; }
    public float frameDragDeltaY() { return frameDeltaY; }
    public float pressedStartX() { return pressStartX; }
    public float pressedStartY() { return pressStartY; }
    // =============================================================================
    // SELECTION / FOCUS
    // =============================================================================
    public int selectedID() { return selectedID; }
    public boolean justSelected(int id) { return isValid(id) && selectedID == id; }
    public boolean justSelected(int id, int mouseBtn) { return justSelected(id) && lastActiveMouseBtn == mouseBtn; }
    public int focusedID() { return focusedID; }
    public boolean isFocused(int id) { return isValid(id) && focusedID == id; }
    public boolean justFocused(int id) { return isFocused(id) && focusedID != lastFocusedID; }
    public boolean justLostFocus(int id) { return isValid(id) && lastFocusedID == id && focusedID != lastFocusedID; }
    public void stealFocus(int id) { focusRequestID = id; }
    public void yieldFocus() { focusRequestID = NO_ID; }
    public void yieldFocus(int id) { if (isFocused(id)) yieldFocus(); }
    public float focusedDuration() { return (float) (focusedDurationNS / 1_000_000_000d); }

    public boolean navigatedUp() { return navigation == NAV_UP; }
    public boolean navigatedUp(int id) { return isFocused(id) && navigatedUp(); }
    public boolean navigateddRight() { return navigation == NAV_RIGHT; }
    public boolean navigateddRight(int id) { return isFocused(id) && navigateddRight(); }
    public boolean navigatedDown() { return navigation == NAV_DOWN; }
    public boolean navigatedDown(int id) { return isFocused(id) && navigatedDown(); }
    public boolean navigatedLeft() { return navigation == NAV_LEFT; }
    public boolean navigatedLeft(int id) { return isFocused(id) && navigatedLeft(); }


    // =============================================================================
    // COMPLETE STATE
    // =============================================================================

    public int getState(int id) {
        if (!isValid(id)) return 0;
        int state = 0;
        if (isHoveredRaw(id)) {
            state |= HOVERED;
            if (!wasHoveredRaw(id)) {
                state |= HOVERED_JUST;
            }
        }
        if (pressedID == id) {
            state |= PRESSED;
            if (pressedID != lastPressedID) {
                state |= PRESSED_JUST;
            }
            switch (activeMouseBtn) {
                case Mouse.LEFT  ->  state |= PRESSED_L;
                case Mouse.RIGHT ->  state |= PRESSED_R;
                case Mouse.WHEEL ->  state |= PRESSED_W;
            }
        }
        if (selectedID == id) {
            state |= SELECTED;
            switch (lastActiveMouseBtn) {
                case Mouse.LEFT  ->  state |= SELECTED_L;
                case Mouse.RIGHT ->  state |= SELECTED_R;
                case Mouse.WHEEL ->  state |= SELECTED_W;
            }
        }
        if (draggedID == id) {
            state |= DRAGGED;
            if (draggedID != lastDraggedID) {
                state |= DRAGGED_JUST;
            }
            switch (activeMouseBtn) {
                case Mouse.LEFT  ->  state |= DRAGGED_L;
                case Mouse.RIGHT ->  state |= DRAGGED_R;
                case Mouse.WHEEL ->  state |= DRAGGED_W;
            }
        }
        if (focusedID == id) {
            state |= FOCUSED;
            if (focusedID != lastFocusedID) {
                state |= FOCUSED_JUST;
            }
        } else if (lastFocusedID == id) {
            state |= FOCUSED_LOST;
        }
        return state;
    }
    // =============================================================================
    // DYNAMIC ID GENERATION
    // =============================================================================
    public int getCurrentWindowID() { return currentWindowID; }
    /** Returns current parent scope seed at top of stack, or FNV basis if root */
    public int getScopeID() { return currentSeed(); }
    /** Derive a unique ID under current stack scope for a String key */
    public int getID(String key) { return hash(key, currentSeed()); }
    /** Derive a unique ID under current stack scope for an integer index */
    public int getID(int index) { return hash(index, currentSeed()); }
    /** push scope + set windowID to current window id*/
    public void pushWindowID(String windowTitle) {
        // todo:
        pushID(windowTitle); // make it a stack? later
        currentWindowID = currentSeed();
    }
    /** Pushes a String scope onto the stack, combining it with the parent seed */
    public int pushID(String scope) {
        if (stackDepth >= ID_STACK_CAPACITY) throw new IllegalStateException("GUI ID Stack overflow!");
        int id = hash(scope, currentSeed());
        idStack[stackDepth++] = id;
        return id;
    } /** Pushes an integer scope onto the stack, combining it with the parent seed */
    public int pushID(int scope) {
        if (stackDepth >= ID_STACK_CAPACITY) throw new IllegalStateException("GUI ID Stack overflow!");
        int id = hash(scope, currentSeed());
        idStack[stackDepth++] = id;
        return id;
    } /** Pops the active scope off the stack */
    public void popID() {
        if (stackDepth == 0) throw new IllegalStateException("GUI ID Stack underflow!");
        stackDepth--;
    }
    // =============================================================================
    // INTERNAL HASHING
    // =============================================================================
    // --- FNV-1a Hash Constants ---
    private static final int FNV_OFFSET_32 = 0x811c9dc5;
    private static final int FNV_PRIME_32 = 0x01000193;
    private static final int HASH_SENTINEL = 1;
    private int currentSeed() {
        return stackDepth == 0 ? FNV_OFFSET_32 : idStack[stackDepth - 1];
    } private static int hash(String str, int seed) {
        if (str == null) throw new NullPointerException("ID key/scope String cannot be null");
        int hash = seed;
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            hash = (hash ^ (c & 0xFF)) * FNV_PRIME_32;
            hash = (hash ^ ((c >>> 8) & 0xFF)) * FNV_PRIME_32;
        } return hash == NO_ID ? HASH_SENTINEL : hash;
    } private static int hash(int value, int seed) {
        int hash = seed;
        hash = (hash ^ (value & 0xFF)) * FNV_PRIME_32;
        hash = (hash ^ ((value >>> 8) & 0xFF)) * FNV_PRIME_32;
        hash = (hash ^ ((value >>> 16) & 0xFF)) * FNV_PRIME_32;
        hash = (hash ^ ((value >>> 24) & 0xFF)) * FNV_PRIME_32;
        return hash == NO_ID ? HASH_SENTINEL : hash;
    }
}
