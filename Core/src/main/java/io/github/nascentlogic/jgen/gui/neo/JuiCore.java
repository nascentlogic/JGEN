package io.github.nascentlogic.jgen.gui.neo;

import io.github.nascentlogic.jgen.Gamepads;
import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.Keyboard;
import io.github.nascentlogic.jgen.Mouse;
import io.github.nascentlogic.jgen.gui.adt.Axis;
import io.github.nascentlogic.jgen.gui.adt.JuiLayoutAPI;
import io.github.nascentlogic.jgen.gui.adt.JuiStateAPI;
import org.joml.*;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;
import org.lwjgl.glfw.GLFW;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * F.Dahl, 9/14/2026
 */
public class JuiCore implements JuiStateAPI, JuiLayoutAPI {


    int hoveredID           = NULL;
    int pressedID           = NULL;
    int selectedID          = NULL;
    int draggedID           = NULL;
    int focusedID           = NULL;
    int focusRequest        = NULL;

    int lastHoveredID       = NULL;
    int lastPressedID       = NULL;
    int lastDraggedID       = NULL;
    int lastFocusedID       = NULL;

    int activeMouseBtn      = MOUSE_NONE;
    int lastActiveMouseBtn  = MOUSE_NONE;
    int navigationBtn       = NULL;

    long hoveredDurationNS  = 0L;
    long pressedDurationNS  = 0L;
    long focusedDurationNS  = 0L;

    float mousePositionX    = 0.0f;
    float mousePositionY    = 0.0f;
    float mousePressOriginX = 0.0f;
    float mousePressOriginY = 0.0f;
    float mouseFrameDeltaX  = 0.0f;
    float mouseFrameDeltaY  = 0.0f;
    float mouseDragVectorX  = 0.0f;
    float mouseDragVectorY  = 0.0f;
    float mouseLastFrameX   = 0.0f;
    float mouseLastFrameY   = 0.0f;








    private void stateTick(int pixelID) {
        if (idStackDepth != 0) throw new IllegalStateException("Mismatched pushId/popId Stack depth");
        Mouse mouse = Jgen.get().mouse();
        Gamepads gamepads = Jgen.get().gamepads(); // later
        Keyboard keys = Jgen.get().keys();
        // =============================================================================
        // FRAME RESET
        // =============================================================================
        idStackDepth = 0;
        // Previous frame state snapshot
        lastHoveredID = hoveredID; hoveredID = pixelID;
        lastPressedID = pressedID;
        lastDraggedID = draggedID;
        lastFocusedID = focusedID; focusedID = focusRequest;
        lastActiveMouseBtn = activeMouseBtn;
        // Reset single-frame pulse events
        navigationBtn = NULL;
        selectedID = NULL;
        // mouse variables
        mouseLastFrameX = mousePositionX;
        mouseLastFrameY = mousePositionY;
        mousePositionX = mouse.position().x;
        mousePositionY = mouse.position().y;
        mouseFrameDeltaX = mousePositionX - mouseLastFrameX;
        mouseFrameDeltaY = mousePositionY - mouseLastFrameY;
        // =============================================================================
        // RELEASE / END INTERACTION
        // =============================================================================
        if (pressedID != NULL && activeMouseBtn != MOUSE_NONE) {
            if (mouse.justReleased(activeMouseBtn)) {
                // Register a selection click ONLY if it wasn't converted into a drag operation
                if (hoveredID == pressedID && draggedID == NULL) selectedID = pressedID;
                draggedID = NULL;
                pressedID = NULL;
                activeMouseBtn = MOUSE_NONE;
            }
        }
        // =============================================================================
        // CAPTURE NEW PRESS
        // =============================================================================
        if (pressedID == NULL && hoveredID != NULL) {
            for (int button = 0; button < MOUSE_BUTTONS; button++) {
                if (mouse.justPressed(button)) {
                    pressedID = hoveredID;
                    activeMouseBtn = button;
                    mousePressOriginX = mousePositionX;
                    mousePressOriginY = mousePositionY;
                    mouseDragVectorX = 0f;
                    mouseDragVectorY = 0f;
                    break;
                }
            }
        }
        // =============================================================================
        // DRAG THRESHOLD & DELTA EVALUATION
        // =============================================================================
        if (pressedID != NULL) {
            mouseDragVectorX = mousePositionX - mousePressOriginX;
            mouseDragVectorY = mousePositionY - mousePressOriginY;
            if (draggedID == NULL) {
                float xSqr = mouseDragVectorX * mouseDragVectorX;
                float ySqr = mouseDragVectorY * mouseDragVectorY;
                if (xSqr + ySqr >= (DRAG_THRESHOLD * DRAG_THRESHOLD)) draggedID = pressedID;
            }
        } else {
            mouseDragVectorX = 0f;
            mouseDragVectorY = 0f;
        }
        // =============================================================================
        // KEYBOARD / GAMEPAD SELECTION
        // =============================================================================

        if (focusedID != NULL) {
            if (keys.justPressed(GLFW.GLFW_KEY_ENTER)) {
                selectedID = focusedID;
                navigationBtn = NAV_SELECT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_UP)) {
                navigationBtn = NAV_UP;
            } else if (keys.justPressed(GLFW.GLFW_KEY_RIGHT)) {
                navigationBtn = NAV_RIGHT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_DOWN)) {
                navigationBtn = NAV_DOWN;
            } else if (keys.justPressed(GLFW.GLFW_KEY_LEFT)) {
                navigationBtn = NAV_LEFT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_PAGE_UP)) {
                navigationBtn = NAV_START;
            } else if (keys.justPressed(GLFW.GLFW_KEY_PAGE_DOWN)) {
                navigationBtn = NAV_END;
            } else if (keys.justPressed(GLFW.GLFW_KEY_ESCAPE)) {
                navigationBtn = NAV_EXIT;
            }
        }
        // =============================================================================
        // DURATION TRACKING
        // =============================================================================
        long deltaTimeNS = Jgen.get().time().deltaTimeNS();
        hoveredDurationNS = (hoveredID != NULL && hoveredID == lastHoveredID) ? hoveredDurationNS + deltaTimeNS : 0L;
        pressedDurationNS = (pressedID != NULL && pressedID == lastPressedID) ? pressedDurationNS + deltaTimeNS : 0L;
        focusedDurationNS = (focusedID != NULL && focusedID == lastFocusedID) ? focusedDurationNS + deltaTimeNS : 0L;
    }

    public int currentHoveredID() { return hoveredID; }
    public int currentPressedID() { return pressedID; }
    public int currentDraggedID() { return draggedID; }
    public int currentSelectedID() { return selectedID; }
    public int currentFocusedID() { return focusedID; }
    public int lastFrameHoveredID() { return lastHoveredID; }
    public int lastFramePressedID() { return lastPressedID; }
    public int lastFrameDraggedID() { return lastDraggedID; }
    public int lastFrameFocusedID() { return lastFocusedID; }
    public int navigationBtn() { return navigationBtn; }
    public int mouseActiveBtn() { return activeMouseBtn; }
    public int mouseLastActiveBtn() { return lastActiveMouseBtn; }
    public float hoveredDuration() { return (float) (hoveredDurationNS / 1_000_000_000d); }
    public float pressedDuration() { return (float) (pressedDurationNS / 1_000_000_000d); }
    public float focusedDuration() { return (float) (focusedDurationNS / 1_000_000_000d); }
    public float mousePosX() { return mousePositionX; }
    public float mousePosY() { return mousePositionY; }
    public float mousePressOriginX() { return mousePressOriginX; }
    public float mousePressOriginY() { return mousePressOriginY; }
    public float mouseFrameDeltaX() { return mouseFrameDeltaX; }
    public float mouseFrameDeltaY() { return mouseFrameDeltaY; }
    public float mouseDragVectorX() { return mouseDragVectorX; }
    public float mouseDragVectorY() { return mouseDragVectorY; }
    public void focusSteal(int id) { focusRequest = id; }
    public void focusYield() { focusRequest = NULL; }


    // =============================================================================
    // ID STACK / HASHING
    // =============================================================================
    final int[] idStack = new int[ID_STACK_CAP];
    int idStackDepth = 0;
    public int scopeID() {
        return idStackDepth == 0 ? FNV_OFFSET_32 : idStack[idStackDepth - 1];
    } public int pushID(String scope) {
        if (idStackDepth >= ID_STACK_CAP) {
            throw new IllegalStateException("id stack overflow!");
        } int newScope = JuiStateAPI.hash(scope, scopeID());
        idStack[idStackDepth++] = newScope;
        return newScope;
    } public int pushID(int scope) {
        if (idStackDepth >= ID_STACK_CAP) {
            throw new IllegalStateException("id stack overflow!");
        } int newScope = JuiStateAPI.hash(scope, scopeID());
        idStack[idStackDepth++] = newScope;
        return newScope;
    } public void popID() {
        if (idStackDepth == 0) {
            throw new IllegalStateException("id stack underflow!");
        } idStackDepth--;
    }

    // =============================================================================
    // CORE LAYOUT
    // =============================================================================
    private final Rectanglef contBounds = new Rectanglef();
    private final Container[] containerStack = new Container[CONTAINER_STACK_CAP];
    private int contStackDepth;
    public int containerStackDepth() { return contStackDepth; }
    public Container currentContainer() { return contStackDepth == 0 ? null : containerStack[contStackDepth - 1]; }
    public Container containerRoot() { return contStackDepth == 0 ? null : containerStack[0];}
    private Container peekContainerUnchecked() { return containerStack[contStackDepth - 1]; }
    private Container pushContainerInternal() {
        if (contStackDepth >= CONTAINER_STACK_CAP) {
            throw new IllegalStateException("container stack overflow!");
        } return containerStack[contStackDepth++];
    } private Container popContainerInternal() {
        if (contStackDepth == 0) {
            throw new IllegalStateException("container stack underflow!");
        } return containerStack[--contStackDepth];
    } public Rectanglef allocateSpace(float size, Rectanglef dst) {
        if (containerStackEmpty()) throw new IllegalStateException("empty container stack alloc!");
        if (size < 0.0f) throw new IllegalArgumentException("container negative size alloc!");
        return peekContainerUnchecked().allocate(size,dst);
    } public Rectanglef allocateRemaining(Rectanglef dst) {
        if (containerStackEmpty()) throw new IllegalStateException("empty container stack alloc!");
        Container container = peekContainerUnchecked();
        return container.allocate(container.availableSpace(),dst);
    } public void pushContainerAbsolute(float x, float y, float w, float h, float spacing, Axis axis, boolean inverseLayout) {
        if (spacing < 0.0f) throw new IllegalArgumentException("negative container spacing request!");
        if (w < 0.0f || h < 0.0f) throw new IllegalArgumentException("negative container area request!");
        pushContainerInternal().init(x,y,w,h,spacing,axis,inverseLayout, Container.SIZE_ABSOLUTE);
    } public void pushContainerFixed(float size, float spacing, Axis axis, boolean inverseLayout) {
        if (containerStackEmpty()) throw new IllegalStateException("no parent container on stack!");
        if (spacing < 0.0f) throw new IllegalArgumentException("negative container spacing request!");
        if (size < 0.0f) throw new IllegalArgumentException("negative container size request!");
        peekContainerUnchecked().computePlacement(size, contBounds);
        pushContainerInternal().init(contBounds.minX, contBounds.minY,
                contBounds.lengthX(), contBounds.lengthY(), spacing, axis, inverseLayout, size);
    } public void pushContainerRemaining(float spacing, Axis axis, boolean inverseLayout) {
        if (containerStackEmpty()) throw new IllegalStateException("no parent container on stack!");
        pushContainerFixed(peekContainerUnchecked().availableSpace(), spacing, axis, inverseLayout);
    } public void pushContainerAuto(float spacing, Axis axis, boolean inverseLayout) {
        if (containerStackEmpty()) throw new IllegalStateException("no parent container on stack!");
        if (spacing < 0.0f) throw new IllegalArgumentException("negative container spacing request!");
        Container parent = peekContainerUnchecked();
        parent.computePlacement(parent.availableSpace(), contBounds);
        pushContainerInternal().init(contBounds.minX, contBounds.minY,
                contBounds.lengthX(), contBounds.lengthY(), spacing, axis, inverseLayout, Container.SIZE_AUTO);
    } public Container popContainer() {
        Container container = popContainerInternal();
        if (!containerStackEmpty() && !container.isAbsolute()) {
            Container parent = peekContainerUnchecked();
            float spaceToCommit = (container.requestedSize == Container.SIZE_AUTO )
                    ? container.contentSize()
                    : container.requestedSize;
            if (spaceToCommit > 0.0f) {
                if (parent.itemCount > 0) parent.offset += parent.spacing;
                parent.offset += spaceToCommit;
                parent.itemCount++;
            }
        } return container;
    }

    // =============================================================================
    // PERSISTENT INTERNAL STORAGE
    // =============================================================================
    private final Map<Integer, Object> persistentStorage = HashMap.newHashMap(1024);
    private void logCollision(int id, Class<?> expected, Class<?> found) {
        Logger.warn("Persistent object collision [ID: {}]! Expected {}, found {}. Overwriting state.",
                id, expected.getSimpleName(), found.getSimpleName());
    } public void persistentPut(int id, Object object) { persistentStorage.put(id, object); }
    public void persistentRemove(int id) { persistentStorage.remove(id); }
    public void persistentClear() { persistentStorage.clear(); }
    public boolean persistentContains(int id) { return persistentStorage.containsKey(id); }
    @SuppressWarnings("unchecked")
    public <T> T getObj(int id, Class<T> clazz, Supplier<T> supplier) {
        Object val = persistentStorage.get(id);
        if (clazz.isInstance(val)) return (T) val;
        if (val != null) logCollision(id, clazz, val.getClass());
        T newInstance = supplier.get();
        persistentStorage.put(id, newInstance);
        return newInstance;
    } public int getInt(int id, int defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Integer i) return i;
        if (val != null) logCollision(id, Integer.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public long getLong(int id, long defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Long l) return l;
        if (val != null) logCollision(id, Long.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public float getFloat(int id, float defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Float f) return f;
        if (val != null) logCollision(id, Float.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public double getDouble(int id, double defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Double d) return d;
        if (val != null) logCollision(id, Double.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public boolean getBool(int id, boolean defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Boolean b) return b;
        if (val != null) logCollision(id, Boolean.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public Vector2f getVec2f(int id, Vector2f defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector2f v) return v;
        if (val != null) logCollision(id, Vector2f.class, val.getClass());
        Vector2f copy = new Vector2f(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector3f getVec3f(int id, Vector3f defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector3f v) return v;
        if (val != null) logCollision(id, Vector3f.class, val.getClass());
        Vector3f copy = new Vector3f(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector4f getVec4f(int id, Vector4f defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector4f v) return v;
        if (val != null) logCollision(id, Vector4f.class, val.getClass());
        Vector4f copy = new Vector4f(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector2i getVec2i(int id, Vector2i defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector2i v) return v;
        if (val != null) logCollision(id, Vector2i.class, val.getClass());
        Vector2i copy = new Vector2i(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector3i getVec3i(int id, Vector3i defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector3i v) return v;
        if (val != null) logCollision(id, Vector3i.class, val.getClass());
        Vector3i copy = new Vector3i(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector4i getVec4i(int id, Vector4i defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector4i v) return v;
        if (val != null) logCollision(id, Vector4i.class, val.getClass());
        Vector4i copy = new Vector4i(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Rectanglef getRectf(int id, Rectanglef defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Rectanglef r) return r;
        if (val != null) logCollision(id, Rectanglef.class, val.getClass());
        Rectanglef copy = new Rectanglef(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    } public Rectanglei getRecti(int id, Rectanglei defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Rectanglei r) return r;
        if (val != null) logCollision(id, Rectanglei.class, val.getClass());
        Rectanglei copy = new Rectanglei(defaultValue);
        persistentStorage.put(id, copy);
        return copy;
    }





}
