package io.github.nascentlogic.jgen.gui.neo;

import io.github.nascentlogic.jgen.gui.adt.Axis;
import org.joml.primitives.Rectanglef;

/**
 * F.Dahl, 9/14/2026
 */
public class Container {

    /** Popped container size is the combined size of it's children */
    public static final float SIZE_AUTO = -1.0f;
    /** Ignore parent restrictions */
    public static final float SIZE_ABSOLUTE = -2.0f;
    /** Layout axis */
    public Axis axis;
    /** itemCount determines spacing: max(0, count - 1) */
    public int itemCount;
    /** Bottom Left X position */
    public float posX;
    /** Bottom Left Y position */
    public float posY;
    /** Absolute width of container (independent of content) */
    public float width;
    /** Absolute height of container (independent of content) */
    public float height;
    /** Spacing between internal items */
    public float spacing;
    /** Consumed main-axis space (content + spacings) */
    public float offset;
    /** SIZE_AUTO, SIZE_ABSOLUTE, or fixed size >= 0.0f */
    public float requestedSize;
    /** Inverted direction of the layout (E.g. top to bottom vs. bottom to top) */
    public boolean inverseLayout; // top to bottom vs. bottom to top (vertical) left to right vs. right to left (horizontal)
    public void init(float x, float y, float w, float h, float spacing, Axis axis, boolean inverseLayout, float requestedSize) {
        this.axis = axis;
        this.posX = x;
        this.posY = y;
        this.width = w;
        this.height = h;
        this.offset = 0.0f;
        this.itemCount = 0;
        this.spacing = spacing;
        this.inverseLayout = inverseLayout;
        this.requestedSize = requestedSize;
    }

    /** Allocates space in container and return the allocated area.
     * @param size allocated size along the axis.
     * @return the allocated area or an invalid (zeroed out) area if size <= 0. */
    public Rectanglef allocate(float size, Rectanglef dst) {
        if (size <= 0) return dst.setMin(0,0).setMax(0,0);
        computePlacement(size,dst);
        offset += size + (itemCount > 0 ? spacing : 0.0f);
        itemCount++;
        return dst;
    }
    /** Full area originally given to this container. */
    public Rectanglef absoluteBounds(Rectanglef dst) {
        return dst.setMin(posX,posY).setMax(posX + width, posY + height);
    }
    /** Bounds of content allocated so far (includes consumed spacings).
     * Degenerate (invalid) on the start edge when nothing has been placed. */
    public Rectanglef contentBounds(Rectanglef dst) {
        if (axis == Axis.VERTICAL) {
            dst.minX = posX;
            dst.maxX = posX + width;
            if (inverseLayout) {
                dst.minY = posY;
                dst.maxY = posY + offset;
            } else {
                dst.maxY = posY + height;
                dst.minY = posY + height - offset;
            }
        } else {
            dst.minY = posY;
            dst.maxY = posY + height;
            if (inverseLayout) {
                dst.maxX = posX + width;
                dst.minX = posX + width - offset;
            } else {
                dst.minX = posX;
                dst.maxX = posX + offset;
            }
        }
        return dst;
    }
    /**
     * Calculates a bounding rectangle (placement) in a container (without mutating its state).
     * @param size      Requested main-axis length
     * @param dst       Output rectangle
     * @return dst
     */
    public Rectanglef computePlacement(float size, Rectanglef dst) {
        float off = offset + (itemCount > 0 ? spacing : 0.0f);
        if (axis == Axis.VERTICAL) {
            dst.minX = posX;
            dst.maxX = posX + width;
            if (inverseLayout) {
                float b = posY + off;
                dst.minY = b;
                dst.maxY = b + size;
            } else {
                float t = posY + height - off;
                dst.maxY = t;
                dst.minY = t - size;
            }
        } else {
            dst.minY = posY;
            dst.maxY = posY + height;
            if (inverseLayout) {
                float r = posX + width - off;
                dst.maxX = r;
                dst.minX = r - size;
            } else {
                float l = posX + off;
                dst.minX = l;
                dst.maxX = l + size;
            }
        }
        return dst;
    }
    /** True if no element is placed in the container */
    public boolean isEmpty() { return itemCount == 0; }
    /** If the container is absolute */
    public boolean isAbsolute() { return requestedSize == SIZE_ABSOLUTE; }
    /** Main-axis space consumed so far (content + inserted spacings). */
    public float contentSize() {
        return offset;
    }
    /** Largest content size still available for the next item.
     * Spacing that would be inserted before a subsequent item is already
     * subtracted. Never negative (clamped).*/
    public float availableSpace() {
        float main = (axis == Axis.VERTICAL) ? height : width;
        if (itemCount == 0) return main;
        return Math.max(main - offset - spacing, 0.0f);
    }
    /** True if an item of the given size can be placed without overflow.*/
    public boolean canFit(float desiredSize) {
        return availableSpace() >= desiredSize;
    }
}
