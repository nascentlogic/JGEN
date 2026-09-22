package io.github.nascentlogic.jgen.gui.util;

import org.joml.primitives.Rectanglef;

/**
 * F.Dahl, 9/11/2026
 */
public class LayoutUtils {



    public static Rectanglef transform(Rectanglef start, Rectanglef end, float lerpFactor, Rectanglef dst) {
        if (lerpFactor <= 0.0f) return dst.set(start);
        if (lerpFactor >= 1.0f) return dst.set(end);
        dst.minX = start.minX + (end.minX - start.minX) * lerpFactor;
        dst.minY = start.minY + (end.minY - start.minY) * lerpFactor;
        dst.maxX = start.maxX + (end.maxX - start.maxX) * lerpFactor;
        dst.maxY = start.maxY + (end.maxY - start.maxY) * lerpFactor;
        return dst;
    }


    public static Rectanglef centerOrFit(Rectanglef bounds, Rectanglef target) {
        float cW = target.lengthX();
        float cH = target.lengthY();
        float pW = bounds.lengthX();
        float pH = bounds.lengthY();
        if (cW <= pW && cH <= pH) {
            float xOff = (pW - cW) * 0.5f;
            float yOff = (pH - cH) * 0.5f;
            target.minX = bounds.minX + xOff;
            target.minY = bounds.minY + yOff;
            target.maxX = target.minX + cW;
            target.maxY = target.minY + cH;
            return target;
        } return fit(bounds, target);
    }

    public static Rectanglef fit(Rectanglef bounds, Rectanglef target) {
        float cW = target.lengthX();
        float cH = target.lengthY();
        if (cW <= 0f || cH <= 0f) {
            return target.set(bounds);
        }
        float pW = bounds.lengthX();
        float pH = bounds.lengthY();
        float w = pW;
        float h = pH;
        if (w * cH > pH * cW) {
            w = (pH * cW) / cH;
        }
        else h = (pW * cH) / cW;
        float xOff = (pW - w) * 0.5f;
        float yOff = (pH - h) * 0.5f;
        target.minX = bounds.minX + xOff;
        target.minY = bounds.minY + yOff;
        target.maxX = target.minX + w;
        target.maxY = target.minY + h;
        return target;
    }

    /**
     * Constrains target to stay inside bounds without modifying target's size.
     * Useful for keeping draggable windows and UI elements inside screen boundaries.
     * Strategy:
     * 1. If bounds is smaller than target in an axis -> Center along that axis.
     * 2. If target is outside bounds in an axis  -> Translate by shortest distance to snap back in.
     */
    public static Rectanglef confine(float minX, float minY, float maxX, float maxY, Rectanglef target) {
        float tW = target.lengthX();
        float tH = target.lengthY();
        float bW = maxX - minX;
        float bH = maxY - minY;
        float newMinX = target.minX;
        float newMinY = target.minY;
        if (tW >= bW) {
            newMinX = minX + (bW - tW) * 0.5f;
        } else if (target.minX < minX) {
            newMinX = minX;
        } else if (target.maxX > maxX) {
            newMinX = maxX - tW;
        }
        if (tH >= bH) {
            newMinY = minY + (bH - tH) * 0.5f;
        } else if (target.minY < minY) {
            newMinY = minY;
        } else if (target.maxY > maxY) {
            newMinY = maxY - tH;
        }
        target.minX = newMinX;
        target.minY = newMinY;
        target.maxX = newMinX + tW;
        target.maxY = newMinY + tH;
        return target;
    }

    /**
     * Constrains target to stay inside bounds without modifying target's size.
     * Useful for keeping draggable windows and UI elements inside screen boundaries.
     * Strategy:
     * 1. If bounds is smaller than target in an axis -> Center along that axis.
     * 2. If target is outside bounds in an axis  -> Translate by shortest distance to snap back in.
     */
    public static Rectanglef confine(Rectanglef bounds, Rectanglef target) {
        return confine(bounds.minX,bounds.minY,bounds.maxX,bounds.maxY,target);
    }


    public static Rectanglef pad(Rectanglef target, float p) { return pad(target,p,p,p,p); }
    public static Rectanglef pad(Rectanglef target, float v, float h) { return pad(target,v,h,v,h); }
    public static Rectanglef pad(Rectanglef target, float t, float r, float b, float l)  {
        target.minX += l;
        target.maxX -= r;
        target.minY += b;
        target.maxY -= t;
        return target;
    }


    public static Rectanglef borderTop(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        dst.minX = bounds.minX;
        dst.maxX = bounds.maxX;
        if (extrude) {
            dst.minY = bounds.maxY;
            dst.maxY = bounds.maxY + thickness;
        } else {
            dst.minY = bounds.maxY - thickness;
            dst.maxY = bounds.maxY;
        } return dst;
    }

    public static Rectanglef borderRight(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        dst.minY = bounds.minY;
        dst.maxY = bounds.maxY;
        if (extrude) {
            dst.minX = bounds.maxX;
            dst.maxX = bounds.maxX + thickness;
        } else {
            dst.minX = bounds.maxX - thickness;
            dst.maxX = bounds.maxX;
        } return dst;
    }

    public static Rectanglef borderBottom(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        dst.minX = bounds.minX;
        dst.maxX = bounds.maxX;
        if (extrude) {
            dst.minY = bounds.minY - thickness;
            dst.maxY = bounds.minY;
        } else {
            dst.minY = bounds.minY;
            dst.maxY = bounds.minY + thickness;
        } return dst;
    }


    public static Rectanglef borderLeft(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        dst.minY = bounds.minY;
        dst.maxY = bounds.maxY;
        if (extrude) {
            dst.minX = bounds.minX - thickness;
            dst.maxX = bounds.minX;
        } else {
            dst.minX = bounds.minX;
            dst.maxX = bounds.minX + thickness;
        } return dst;
    }

    public static Rectanglef borderTopRight(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        if (extrude) {
            dst.minX = bounds.maxX;
            dst.maxX = bounds.maxX + thickness;
            dst.minY = bounds.maxY;
            dst.maxY = bounds.maxY + thickness;
        } else {
            dst.minX = bounds.maxX - thickness;
            dst.maxX = bounds.maxX;
            dst.minY = bounds.maxY - thickness;
            dst.maxY = bounds.maxY;
        } return dst;
    }

    public static Rectanglef borderBottomRight(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        if (extrude) {
            dst.minX = bounds.maxX;
            dst.maxX = bounds.maxX + thickness;
            dst.minY = bounds.minY - thickness;
            dst.maxY = bounds.minY;
        } else {
            dst.minX = bounds.maxX - thickness;
            dst.maxX = bounds.maxX;
            dst.minY = bounds.minY;
            dst.maxY = bounds.minY + thickness;
        } return dst;
    }

    public static Rectanglef borderBottomLeft(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        if (extrude) {
            dst.minX = bounds.minX - thickness;
            dst.maxX = bounds.minX;
            dst.minY = bounds.minY - thickness;
            dst.maxY = bounds.minY;
        } else {
            dst.minX = bounds.minX;
            dst.maxX = bounds.minX + thickness;
            dst.minY = bounds.minY;
            dst.maxY = bounds.minY + thickness;
        } return dst;
    }

    public static Rectanglef borderTopLeft(Rectanglef bounds, Rectanglef dst, float thickness, boolean extrude) {
        if (extrude) {
            dst.minX = bounds.minX - thickness;
            dst.maxX = bounds.minX;
            dst.minY = bounds.maxY;
            dst.maxY = bounds.maxY + thickness;
        } else {
            dst.minX = bounds.minX;
            dst.maxX = bounds.minX + thickness;
            dst.minY = bounds.maxY - thickness;
            dst.maxY = bounds.maxY;
        } return dst;
    }



    /**
     * @param viewSize size of the visible viewport
     * @param contentSize total desired size of the content (E.g. height for vertcal)
     * @return a positive ratio from 0.0 (viewSize == 0) to 1.0 (content fits entirely inside the view)
     */
    public static float scrollCalcContentRatio(float viewSize, float contentSize) {
        if (viewSize <= 0.0f || contentSize <= 0.0f || contentSize <= viewSize) return 1.0f;
        float ratio = viewSize / contentSize;
        return Math.clamp(ratio, 0.0f, 1.0f);
    }

    /** Calculates the maximum physical distance content can scroll */
    public static float scrollCalcMaxDist(float viewSize, float contentSize) {
        float vSize = Math.max(0.0f, viewSize);
        float cSize = Math.max(0.0f, contentSize);
        return Math.max(0.0f, cSize - vSize);
    }

    /**
     * Converts normalized offset: 0.0 (top or left) to 1.0 (bottom / right) into  a physical content scroll translation.
     */
    public static float scrollCalcOffset(float viewSize, float contentSize, float offsetNorm) {
        float maxScroll = scrollCalcMaxDist(viewSize, contentSize);
        float clampedNorm = Math.clamp(offsetNorm, 0.0f, 1.0f);
        return clampedNorm * maxScroll;
    }

    /**
     * Computes new normalized scroll position when mouse wheel ticks occur.
     * @param offsetNorm current scroll offset (0.0 to 1.0)
     * @param wheelDelta mouse wheel scroll delta (e.g., +1.0 or -1.0)
     * @param scrollStepPixels pixels to scroll per wheel tick (e.g., 24.0f)
     */
    public static float scrollApplyWheelScroll(float viewSize, float contentSize, float offsetNorm, float wheelDelta, float scrollStepPixels) {
        float maxScroll = scrollCalcMaxDist(viewSize, contentSize);
        if (maxScroll == 0.0f) return 0.0f;
        float pixelShift = wheelDelta * scrollStepPixels;
        float normShift = pixelShift / maxScroll;
        float clampedNorm = Math.clamp(offsetNorm, 0.0f, 1.0f);
        return Math.clamp(clampedNorm - normShift, 0.0f, 1.0f); // Subtract or add depending on whether wheel delta is inverted in your OS
    }


    /**
     * Converts physical drag displacement into normalized scroll displacement.
     * @param totalDragPixels displacement since drag started (e.g. state.totalDragDeltaY())
     * @param trackLen total length of the scrollbar track
     * @param handleLen length of the visual scrollbar handle
     * @return normalized change in scroll offset
     */
    public static float scrollCalcDragDeltaNorm(float totalDragPixels, float trackLen, float handleLen) {
        float availableTrack = Math.max(0.0f, trackLen - handleLen);
        if (availableTrack <= 0.0f) return 0.0f;
        return totalDragPixels / availableTrack;
    }

    /**
     * Computes the updated normalized scroll position during a drag operation.
     * @param initialScrollNorm scroll position (0.0 to 1.0) captured at the moment drag started
     * @param totalDragPixels displacement since drag started (e.g. state.totalDragDeltaY())
     * @param trackLen total length of the scrollbar track
     * @param handleLen length of the visual scrollbar handle
     * @return updated, clamped normalized scroll offset (0.0 to 1.0)
     */
    public static float scrollApplyDragNorm(float initialScrollNorm, float totalDragPixels, float trackLen, float handleLen) {
        float normDelta = scrollCalcDragDeltaNorm(totalDragPixels, trackLen, handleLen);
        return Math.clamp(initialScrollNorm + normDelta, 0.0f, 1.0f);
    }



}
