package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gui.util.Axis;
import io.github.nascentlogic.jgen.gui.util.Container;
import org.joml.primitives.Rectanglef;

import java.util.Objects;

/**
 * F.Dahl, 9/14/2026
 */
public interface JuiLayoutAPI {



    // =============================================================================
    // ABSOLUTE / FIXED SIZE CONTAINER
    // =============================================================================

    /** {@link #pushContainerAbsolute(float, float, float, float, float, Axis, boolean)} */
    default void pushContainerAbsolute(Rectanglef bounds, float spacing, Axis axis, boolean inverseLayout) {
        pushContainerAbsolute(bounds.minX,bounds.minY,bounds.lengthX(),bounds.lengthY(),spacing,axis,inverseLayout);
    }
    /** {@link #pushContainerAbsolute(float, float, float, float, float, Axis, boolean)} */
    default void pushContainerAbsolute(Rectanglef bounds, float spacing, Axis axis) {
        pushContainerAbsolute(bounds.minX,bounds.minY,bounds.lengthX(),bounds.lengthY(),spacing,axis,false);
    }
    /** {@link #pushContainerAbsolute(float, float, float, float, float, Axis, boolean)} */
    default void pushContainerAbsolute(Rectanglef bounds, Axis axis) {
        pushContainerAbsolute(bounds.minX,bounds.minY,bounds.lengthX(),bounds.lengthY(),0,axis,false);
    }
    /** {@link #pushContainerAbsolute(float, float, float, float, float, Axis, boolean)} */
    default void pushContainerAbsolute(float x, float y, float w, float h, Axis axis) { pushContainerAbsolute(x,y,w,h,0,axis,false); }
    /** {@link #pushContainerAbsolute(float, float, float, float, float, Axis, boolean)} */
    default void pushContainerAbsolute(float x, float y, float w, float h, float spacing, Axis axis) { pushContainerAbsolute(x,y,w,h,spacing,axis,false); }
    /** Push an absolute container on the stack.
     * The root container must be absolute, but not the other way around.
     * @param x bottom left x coordinate of the container
     * @param y bottom left y coordinate of the container
     * @param w width of the container
     * @param h height of hte container
     * @param spacing fixed internal spacing between the containers elements (default = 0)
     * @param axis the axix to allign it's elements.
     * @param inverseLayout direction along the axis to position elements (default = false)
     * @throws IllegalArgumentException if the area or the spacing is negative */
    void pushContainerAbsolute(float x, float y, float w, float h, float spacing, Axis axis, boolean inverseLayout);

    /** {@link #pushContainerFixed(float, float, Axis, boolean)}*/
    default void pushContainerFixed(float size, Axis axis) { pushContainerFixed(size,0,axis,false); }
    /** {@link #pushContainerFixed(float, float, Axis, boolean)}*/
    default void pushContainerFixed(float size, float spacing, Axis axis) { pushContainerFixed(size,spacing,axis,false); }
    /** Push a parent relative container onto the stack.
     * if the size is greater than the containers remaining space, it will simply over-allocate. Which is fine.
     * @param size fixed size allocated along the parent axis. Must be >= 0
     * @param spacing fixed internal spacing between the containers elements (default = 0)
     * @param axis the axix to allign it's elements.
     * @param inverseLayout direction along the axis to position elements (default = false)
     * @throws IllegalArgumentException if spacing or size is negative
     * @throws IllegalStateException if the stack is empty */
    void pushContainerFixed(float size, float spacing, Axis axis, boolean inverseLayout);

    /** {@link #pushContainerRemaining(float, Axis, boolean)}*/
    default void pushContainerRemaining(Axis axis) { pushContainerRemaining(0,axis,false); }
    /** {@link #pushContainerRemaining(float, Axis, boolean)}*/
    default void pushContainerRemaining(float spacing, Axis axis) { pushContainerRemaining(spacing,axis,false); }
    /** Push a parent relative container onto the stack.
     * Filling out the remaninder space of the parent container.
     * @param spacing fixed internal spacing between the containers elements (default = 0)
     * @param axis the axix to allign it's elements.
     * @param inverseLayout direction along the axis to position elements (default = false)
     * @throws IllegalArgumentException if spacing is negative
     * @throws IllegalStateException if the stack is empty */
    void pushContainerRemaining(float spacing, Axis axis, boolean inverseLayout);

    // =============================================================================
    // AUTO FITTING CONTAINER
    // =============================================================================
    /** {@link #pushContainerAuto(float, Axis, boolean)}*/
    default void pushContainerAuto(Axis axis) { pushContainerAuto(0,axis,false); }
    /** {@link #pushContainerAuto(float, Axis, boolean)}*/
    default void pushContainerAuto(float spacing, Axis axis) { pushContainerAuto(spacing,axis,false); }
    /** Push a parent relative container onto the stack.
     * Auto-fittingh containers will take up as much spce as it needs from it's parent.
     * @param spacing fixed internal spacing between the containers elements (default = 0)
     * @param axis the axix to allign it's elements.
     * @param inverseLayout direction along the axis to position elements (default = false)
     * @throws IllegalArgumentException if spacing is negative
     * @throws IllegalStateException if the stack is empty */
    void pushContainerAuto(float spacing, Axis axis, boolean inverseLayout);

    // =============================================================================
    // ALLOC + HELPERS
    // =============================================================================

    /** Pops current container of the stack and returns it.
     * Popped child containers will allacate the parent (new current container) */
    Container popContainer();
    /** Current container on stack or null if stack is empty */
    Container currentContainer();
    /** Current root container or null. stack[0] */
    Container containerRoot();
    /** Allocate fixed size space on the current container.
     * size is allocated along the containers axis.
     * using up all space across the containers cross-axis.
     * if the size is greater than the containers remaining space, it
     * will simply over-allocate. Which is fine.
     * The container content size is not actually modified before it "pops/ends" */
    Rectanglef allocateSpace(float size, Rectanglef dst);
    /** Allocate {@code count} fixed size space on the current container.
     * size is allocated along the containers axis.
     * using up all space across the containers cross-axis.
     * if the size is greater than the containers remaining space, it
     * will simply over-allocate. Which is fine.
     * The container content size is not actually modified before it "pops/ends" */
    default Rectanglef[] allocateSpace(float size, Rectanglef[] dst, int count) {
        Objects.checkFromIndexSize(0, count, dst.length);
        for (int i = 0; i < count; i++) {
            allocateSpace(size, dst[i]);
        } return dst;
    }
    /** Allocate size equal the remaining space of the current container.
     * size is allocated along the containers axis.
     * using up all space across the containers cross-axis.
     * If the container has no availible space, the returned area will be invalid (zeroed out).
     * The container content size is not actually modified before it "pops/ends" */
    Rectanglef allocateRemaining(Rectanglef dst);
    /** Current depth of the container stack (0 == empty) */
    int containerStackDepth();
    default boolean containerStackEmpty() { return containerStackDepth() <= 0; }

}
