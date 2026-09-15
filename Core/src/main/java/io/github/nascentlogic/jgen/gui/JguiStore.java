package io.github.nascentlogic.jgen.gui;

import org.joml.*;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * F.Dahl, 9/9/2026
 */
public class JguiStore {

    private final Map<Integer, Object> map = HashMap.newHashMap(1024); // replace later

    public int getInt(int id, int defaultValue) {
        Object val = map.get(id);
        if (val instanceof Integer i) return i;
        if (val != null) logCollision(id, Integer.class, val.getClass());
        map.put(id, defaultValue);
        return defaultValue;
    }

    public boolean getBoolean(int id, boolean defaultValue) {
        Object val = map.get(id);
        if (val instanceof Boolean b) return b;
        if (val != null) logCollision(id, Boolean.class, val.getClass());
        map.put(id, defaultValue);
        return defaultValue;
    }

    public float getFloat(int id, float defaultValue) {
        Object val = map.get(id);
        if (val instanceof Float f) return f;
        if (val != null) logCollision(id, Float.class, val.getClass());
        map.put(id, defaultValue);
        return defaultValue;
    }

    public Vector2f getVec2f(int id, float defaultX, float defaultY) {
        Object val = map.get(id);
        if (val instanceof Vector2f vec) return vec;
        if (val != null) logCollision(id, Vector2f.class, val.getClass());
        Vector2f vec = new Vector2f(defaultX, defaultY);
        map.put(id, vec);
        return vec;
    }

    public Vector3f getVec3f(int id, float defaultX, float defaultY, float defaultZ) {
        Object val = map.get(id);
        if (val instanceof Vector3f vec) return vec;
        if (val != null) logCollision(id, Vector3f.class, val.getClass());
        Vector3f vec = new Vector3f(defaultX, defaultY, defaultZ);
        map.put(id, vec);
        return vec;
    }

    public Vector4f getVec4f(int id, float defaultX, float defaultY, float defaultZ, float defaultW) {
        Object val = map.get(id);
        if (val instanceof Vector4f vec) return vec;
        if (val != null) logCollision(id, Vector4f.class, val.getClass());
        Vector4f vec = new Vector4f(defaultX, defaultY, defaultZ, defaultW);
        map.put(id, vec);
        return vec;
    }

    public Vector2i getVec2i(int id, int defaultX, int defaultY) {
        Object val = map.get(id);
        if (val instanceof Vector2i vec) return vec;
        if (val != null) logCollision(id, Vector2i.class, val.getClass());
        Vector2i vec = new Vector2i(defaultX, defaultY);
        map.put(id, vec);
        return vec;
    }

    public Vector3i getVec3i(int id, int defaultX, int defaultY, int defaultZ) {
        Object val = map.get(id);
        if (val instanceof Vector3i vec) return vec;
        if (val != null) logCollision(id, Vector3i.class, val.getClass());
        Vector3i vec = new Vector3i(defaultX, defaultY, defaultZ);
        map.put(id, vec);
        return vec;
    }

    public Vector4i getVec4i(int id, int defaultX, int defaultY, int defaultZ, int defaultW) {
        Object val = map.get(id);
        if (val instanceof Vector4i vec) return vec;
        if (val != null) logCollision(id, Vector4i.class, val.getClass());
        Vector4i vec = new Vector4i(defaultX, defaultY, defaultZ, defaultW);
        map.put(id, vec);
        return vec;
    }

    public Rectanglef getRectf(int id, float minX, float minY, float maxX, float maxY) {
        Object val = map.get(id);
        if (val instanceof Rectanglef rect) return rect;
        if (val != null) logCollision(id, Rectanglef.class, val.getClass());
        Rectanglef rect = new Rectanglef(minX, minY, maxX, maxY);
        map.put(id, rect);
        return rect;
    }

    public Rectanglei getRecti(int id, int minX, int minY, int maxX, int maxY) {
        Object val = map.get(id);
        if (val instanceof Rectanglei rect) return rect;
        if (val != null) logCollision(id, Rectanglei.class, val.getClass());
        Rectanglei rect = new Rectanglei(minX, minY, maxX, maxY);
        map.put(id, rect);
        return rect;
    }


    @SuppressWarnings("unchecked")
    public <T> T get(int id, Class<T> type, Supplier<T> factory) {
        Object val = map.get(id);
        if (type.isInstance(val)) return (T) val;
        if (val != null) logCollision(id, type, val.getClass());
        T newInstance = factory.get();
        map.put(id, newInstance);
        return newInstance;
    }

    public boolean contains(int id) { return map.containsKey(id); }
    public void remove(int id) { map.remove(id); }
    public void clear() { map.clear(); }

    private void logCollision(int id, Class<?> expected, Class<?> found) {
        Logger.warn("GUI Storage ID collision [ID: {}]! Expected {}, found {}. Overwriting state.",
                id, expected.getSimpleName(), found.getSimpleName());
    }

}
