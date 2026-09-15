package io.github.nascentlogic.jgen.gui;


import org.joml.primitives.Rectanglef;

/**
 * F.Dahl, 9/9/2026
 */
public class JguiLayout {


    public enum Axis { VERTICAL, HORIZONTAL }
    private final Rectanglef tmpBounds = new Rectanglef();
    private static final int STACK_CAP = 64;
    private Container[] stack = new Container[STACK_CAP];
    private int stackDepth;

    JguiLayout() {
        for (int i = 0; i < STACK_CAP; i++) {
            stack[i] = new Container();
        }
    }

    protected void resetLayoutStack() {
        if (stackDepth != 0) throw new IllegalStateException("Layout mismatched Stack depth at end of last frame: " + stackDepth);
        stackDepth = 0;
    }


    // =============================================================================
    // ABSOLUTE / FIXED SIZE CONTAINER
    // =============================================================================

    // beginContainer / endContainer // allocateSpace

    public void pushAbsolute(float x, float y, float w, float h, Axis axis) { pushAbsolute(x,y,w,h,0,axis,false); }
    public void pushAbsolute(float x, float y, float w, float h, float spacing, Axis axis) { pushAbsolute(x,y,w,h,spacing,axis,false); }
    public void pushAbsolute(float x, float y, float w, float h, float spacing, Axis axis, boolean inverseLayout) {
        // pushStack().init(x, y, w, h, spacing, axis, inverseLayout); // old way
        if (axis == null) throw new NullPointerException("Axis cannot be null");
        if (w < 0.0f || h < 0.0f) throw new IllegalArgumentException("Container width and height must be >= 0");
        if (spacing < 0.0f) throw new IllegalArgumentException("Container spacing must be >= 0");
        pushStack().init(x, y, w, h, spacing, axis, inverseLayout, Container.SIZE_ABSOLUTE);
    }

    public void pushFixed(float size, Axis axis) { pushFixed(size,0,axis,false); }
    public void pushFixed(float size, float spacing, Axis axis) { pushFixed(size,spacing,axis,false); }
    public void pushFixed(float size, float spacing, Axis axis, boolean inverseLayout) {
        if (isEmpty()) throw new IllegalStateException("Cannot push nested container onto an empty stack!");
        if (axis == null) throw new NullPointerException("Axis cannot be null");
        if (size < 0.0f) throw new IllegalArgumentException("Container size must be >= 0");
        if (spacing < 0.0f) throw new IllegalArgumentException("Container spacing must be >= 0");
        Container parent = peekUnchecked();
        computePlacementRect(parent, size, tmpBounds);
        pushStack().init(tmpBounds.minX, tmpBounds.minY, tmpBounds.lengthX(), tmpBounds.lengthY(), spacing, axis, inverseLayout, size);
    }

    public void pushRemaining(Axis axis) { pushRemaining(0,axis,false); }
    public void pushRemaining(float spacing, Axis axis) { pushRemaining(spacing,axis,false); }
    public void pushRemaining(float spacing, Axis axis, boolean inverseLayout) {
        if (isEmpty()) throw new IllegalStateException("Cannot push nested container onto an empty stack!");
        pushFixed(peekUnchecked().availableSpace(), spacing, axis, inverseLayout);
    }

    // =============================================================================
    // AUTO FITTING CONTAINER
    // =============================================================================

    public void pushAuto(Axis axis) { pushAuto(0,axis,false); }
    public void pushAuto(float spacing, Axis axis) { pushAuto(spacing,axis,false); }
    public void pushAuto(float spacing, Axis axis, boolean inverseLayout) {
        if (isEmpty()) throw new IllegalStateException("Cannot push nested container onto an empty stack!");
        if (axis == null) throw new NullPointerException("Axis cannot be null");
        if (spacing < 0.0f) throw new IllegalArgumentException("Container spacing must be >= 0");
        Container parent = peekUnchecked();
        computePlacementRect(parent, parent.availableSpace(), tmpBounds);
        pushStack().init(tmpBounds.minX, tmpBounds.minY, tmpBounds.lengthX(), tmpBounds.lengthY(), spacing, axis, inverseLayout, Container.SIZE_AUTO);
    }

    // =============================================================================
    // POP CONTAINER
    // =============================================================================

    public Container pop() {
        Container child = popStack();
        if (!isEmpty() && !child.isAbsolute()) {
            Container parent = peekUnchecked();
            // fixed vs. auto sized
            float spaceToCommit = (child.requestedSize == Container.SIZE_AUTO)
                    ? child.contentSize()
                    : child.requestedSize;
            if (spaceToCommit > 0.0f) {
                if (parent.itemCount > 0) parent.offset += parent.spacing;
                parent.offset += spaceToCommit;
                parent.itemCount++;
            }
        }
        return child;
    }

    public Container current() {
        return peekStack();
    }

    public Container currentRoot() {
        return stackDepth == 0 ? null : stack[0];
    }

    // =============================================================================
    // UI ELEMENT ALLOCATION
    // =============================================================================

    /**
     * Allocates a rectangle of the requested content size along the main axis.
     * Always fills the cross-axis. Spacing is inserted between items only.
     * @param size content size on the main axis; must be &gt;= 0
     * @param dst  receives the result (min/max). Zeroed if size == 0.
     * @return dst
     * @throws IllegalArgumentException if size &lt; 0
     * @throws IllegalStateException if the stack is empty
     */
    public Rectanglef allocate(float size, Rectanglef dst) {
        if (isEmpty()) throw new IllegalStateException("Layout empty stack alloc!");
        if (size < 0.0f) throw new IllegalArgumentException("Layout negative size alloc!");
        return allocateOnContainer(peekUnchecked(), size, dst);
    }

    /**
     * Allocates all remaining content budget.
     * If none remains, dst is zeroed and container state is unchanged.
     */
    public Rectanglef allocateRemaining(Rectanglef dst) {
        if (isEmpty()) throw new IllegalStateException("Layout empty stack alloc!");
        float available = peekUnchecked().availableSpace();
        return allocateOnContainer(peekUnchecked(), available, dst);
    }

    private static Rectanglef allocateOnContainer(Container container, float size, Rectanglef dst) {
        if (size <= 0.0f) return writeInvalid(dst);
        computePlacementRect(container, size, dst);
        container.offset += size + (container.itemCount > 0 ? container.spacing : 0.0f);
        container.itemCount++;
        return dst;
    }

    public int stackDepth() {
        return stackDepth;
    }

    public boolean isEmpty() {
        return stackDepth == 0;
    }

    public void clear() {
        stackDepth = 0;
    }

    /**
     * Calculates a bounding rectangle (placement) on a container without mutating its state.
     * @param container The target container (parent or current)
     * @param size      Requested main-axis length
     * @param dst       Output rectangle
     * @return dst
     */
    static private Rectanglef computePlacementRect(Container container, float size, Rectanglef dst) {
        float startOffset = container.offset + (container.itemCount > 0 ? container.spacing : 0.0f);
        if (container.axis == Axis.VERTICAL) {
            dst.minX = container.posX;
            dst.maxX = container.posX + container.width;
            if (container.inverseLayout) {
                float bottom = container.posY + startOffset;
                dst.minY = bottom;
                dst.maxY = bottom + size;
            } else {
                float top = container.posY + container.height - startOffset;
                dst.maxY = top;
                dst.minY = top - size;
            }
        } else {
            dst.minY = container.posY;
            dst.maxY = container.posY + container.height;
            if (container.inverseLayout) {
                float right = container.posX + container.width - startOffset;
                dst.maxX = right;
                dst.minX = right - size;
            } else {
                float left = container.posX + startOffset;
                // float left = container.posX + container.offset + (container.itemCount > 0 ? container.spacing : 0.0f);
                dst.minX = left;
                dst.maxX = left + size;
            }
        }
        return dst;
    }



    private Container pushStack() {
        if (stackDepth >= STACK_CAP) throw new IllegalStateException("Layout stack overflow!");
        return stack[stackDepth++];
    }

    private Container popStack() {
        if (stackDepth == 0) throw new IllegalStateException("Layout stack underflow!");
        return stack[--stackDepth];
    }

    private Container peekUnchecked() {
        return stack[stackDepth - 1];
    }

    private Container peekStack() {
        return stackDepth == 0 ? null : stack[stackDepth - 1];
    }

    private static Rectanglef writeInvalid(Rectanglef dst) {
        return dst.setMin(0,0).setMax(0,0);
    }





    public static final class Container {
        public static final float SIZE_AUTO = -1.0f;
        public static final float SIZE_ABSOLUTE = -2.0f;
        public Axis axis;
        public int itemCount;
        public float posX, posY; // bottom-left
        public float width, height;
        public float spacing; // spacing between items
        public float offset; // consumed main-axis space (content + spacings)
        public float requestedSize; // SIZE_AUTO, SIZE_ABSOLUTE, or fixed size >= 0.0f
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
            this.requestedSize = requestedSize; // Added line
        }

        public boolean isEmpty() {
            return itemCount == 0;
        }

        /** Full area originally given to this container. */
        public Rectanglef absoluteBounds(Rectanglef dst) {
            return dst.setMin(posX,posY).setMax(posX + width, posY + height);
        }

        /** Bounds of content allocated so far (includes consumed spacings).
         * Degenerate on the start edge when nothing has been placed. */
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

        public boolean isAbsolute() {
            return requestedSize == SIZE_ABSOLUTE;
        }

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
}
