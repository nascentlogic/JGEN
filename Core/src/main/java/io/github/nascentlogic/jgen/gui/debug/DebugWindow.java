package io.github.nascentlogic.jgen.gui.debug;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.JuiCore;
import io.github.nascentlogic.jgen.gui.LayoutUtils;
import io.github.nascentlogic.jgen.gui.api.Axis;
import io.github.nascentlogic.jgen.gui.api.JuiState;
import io.github.nascentlogic.jgen.gui.api.JuiWindow;
import io.github.nascentlogic.jgen.gui.api.TextAlignment;
import io.github.nascentlogic.jgen.utils.JgenUtils;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;
import org.tinylog.Logger;

import java.util.Objects;

import static io.github.nascentlogic.jgen.gui.debug.DebugStyle.*;
import static io.github.nascentlogic.jgen.utils.JgenUtils.popRect;
import static io.github.nascentlogic.jgen.utils.JgenUtils.pushRect;

/**
 * F.Dahl, 10/7/2026
 */
public abstract class DebugWindow implements JuiWindow {

    protected final String name;
    protected final Rectanglef contentArea  = new Rectanglef();
    protected final Rectanglef windowArea   = new Rectanglef();

    private final Rectanglef restoredArea   = new Rectanglef();
    private final Rectanglef animOrigin     = new Rectanglef();
    private final Rectanglef animTarget     = new Rectanglef();
    private float animProgress              = 1.0f;
    private boolean animating               = false;
    private boolean signaledToMaximize      = false;
    private boolean signaledToRestore       = false;
    private boolean signaledToCenter        = false;
    private boolean open                    = false;
    private boolean resizable               = true;
    private boolean registered              = false;

    private float minWinWidth;
    private float minWinHeight;

    public DebugWindow(int contentWidth, int contentHeight, String name) {
        this.contentArea.setMax( Math.max(contentWidth,0), Math.max(contentHeight,0));
        this.contentArea.setMin(0,0);
        this.name = Objects.requireNonNull(name);
    }

    public void process(JuiCore jui, float dt) {
        int backgroundID = processFrame(jui, dt);
        if (contentArea.isValid()) {
            processContent(contentArea,jui,backgroundID,dt);
        }
    }

    public abstract void processContent(Rectanglef contentArea, JuiCore jui, int bgID, float dt);

    private int processFrame(JuiCore jui, float dt) {

        float guiResW = jui.virtualWidth();
        float guiResH = jui.virtualHeight();

        jui.pushID(23);
        final int navID       = jui.getID("nav");
        final int bgID        = jui.getID("bg");
        final int tBorderID   = jui.getID("tb");
        final int rBorderID   = jui.getID("rb");
        final int bBorderID   = jui.getID("bb");
        final int lBorderID   = jui.getID("lb");
        final int trCornerID  = jui.getID("trc");
        final int brCornerID  = jui.getID("brc");
        final int blCornerID  = jui.getID("blc");
        final int tlCornerID  = jui.getID("tlc");
        jui.popID();

        if (jui.isPressed(bgID)) { /* */ }
        if (jui.isPressed(navID)) { /* */ }

        if (resizable) {

            boolean maximized = windowArea.lengthX() == guiResW && windowArea.lengthY() == guiResH;

            if (jui.justClicked(navID)) {
                if (maximized) signaledToRestore = true;
                else signaledToMaximize = true;
            }

            if (signaledToMaximize && !maximized) {
                if (animating) {
                    boolean maximizing = animTarget.lengthX() == guiResW && animTarget.lengthY() == guiResH;
                    if (!maximizing) {
                        Rectanglef tmp = new Rectanglef(animOrigin);
                        animOrigin.set(animTarget);
                        animTarget.set(tmp);
                        animProgress = 1.0f - animProgress;
                    }
                } else {
                    restoredArea.set(windowArea);
                    animOrigin.set(windowArea);
                    animTarget.setMin(0,0).setMax(guiResW,guiResH);
                    animProgress = 0.0f;
                    animating = true;
                }
            } else if (signaledToRestore) {
                if (animating) {
                    boolean restoring = animTarget.equals(restoredArea);
                    if (!restoring) {
                        Rectanglef tmp = new Rectanglef(animOrigin);
                        animOrigin.set(animTarget);
                        animTarget.set(tmp);
                        animProgress = 1.0f - animProgress;
                    }
                } else if (maximized) {
                    animOrigin.set(windowArea);
                    animTarget.set(restoredArea);
                    animProgress = 0.0f;
                    animating = true;
                }
            }

            if (animating) {
                animProgress += dt / WINDOW_ANIM_DURATION;
                animProgress = Math.clamp(animProgress,0.0f,1.0f);
                float t = 1.0f - (float) Math.pow(1.0f - animProgress, 3.0);// Cubic Ease-Out
                windowArea.minX = animOrigin.minX + (animTarget.minX - animOrigin.minX) * t;
                windowArea.minY = animOrigin.minY + (animTarget.minY - animOrigin.minY) * t;
                windowArea.maxX = animOrigin.maxX + (animTarget.maxX - animOrigin.maxX) * t;
                windowArea.maxY = animOrigin.maxY + (animTarget.maxY - animOrigin.maxY) * t;
                if (animProgress >= 1.0f) animating = false;
            } else {
                animProgress = 1.0f;
                if (!maximized) restoredArea.set(windowArea);
            }

            if (jui.anyDragged()) {
                if (jui.isDragged(navID, JuiState.MOUSE_LEFT)) {
                    Vector2f windowAtdragStart = jui.getVec2f(navID, windowArea.minX, windowArea.minY);
                    if (jui.justDragged(navID)) windowAtdragStart.set(windowArea.minX, windowArea.minY);
                    float targetX = windowAtdragStart.x + jui.mouseDragVectorX();
                    float targetY = windowAtdragStart.y + jui.mouseDragVectorY();
                    float width = windowArea.lengthX();
                    float height = windowArea.lengthY();
                    windowArea.setMin(targetX,targetY).setMax(targetX + width, targetY + height);
                }
                if (resizable) {
                    if (jui.isDragged(tBorderID, JuiState.MOUSE_LEFT)) {
                        if (jui.justDragged(tBorderID)) jui.persistentPut(tBorderID, windowArea.maxY);
                        float borderAtDragStart = jui.getFloat(tBorderID, windowArea.maxY);
                        float targetY = borderAtDragStart + jui.mouseDragVectorY();
                        float fixedBottomY = windowArea.minY;
                        targetY = Math.min(targetY, guiResH);
                        float rawHeight = targetY - fixedBottomY;
                        float newHeight = Math.max(minWinHeight, rawHeight);
                        windowArea.maxY = Math.min(fixedBottomY + newHeight, guiResH);
                    }
                    if (jui.isDragged(bBorderID, JuiState.MOUSE_LEFT)) {
                        if (jui.justDragged(bBorderID)) jui.persistentPut(bBorderID, windowArea.minY);
                        float borderAtDragStart = jui.getFloat(bBorderID, windowArea.minY);
                        float targetY = borderAtDragStart + jui.mouseDragVectorY();
                        float fixedTopY = windowArea.maxY;
                        targetY = Math.max(targetY, 0f);
                        float rawHeight = fixedTopY - targetY;
                        float newHeight = Math.max(minWinHeight, rawHeight);
                        windowArea.minY = Math.max(fixedTopY - newHeight, 0f);
                    }
                    if (jui.isDragged(lBorderID, JuiState.MOUSE_LEFT)) {
                        if (jui.justDragged(lBorderID)) jui.persistentPut(lBorderID, windowArea.minX);
                        float borderAtDragStart = jui.getFloat(lBorderID, windowArea.minX);
                        float targetX = borderAtDragStart + jui.mouseDragVectorX();
                        float fixedRightX = windowArea.maxX;
                        targetX = Math.max(targetX, 0f);
                        float rawWidth = fixedRightX - targetX;
                        float newWidth = Math.max(minWinWidth, rawWidth);
                        windowArea.minX = Math.max(fixedRightX - newWidth, 0f);
                    }
                    if (jui.isDragged(rBorderID)) {
                        if (jui.justDragged(rBorderID)) jui.persistentPut(rBorderID, windowArea.maxX);
                        float borderAtDragStart = jui.getFloat(rBorderID, windowArea.maxX);
                        float targetX = borderAtDragStart + jui.mouseDragVectorX();
                        float fixedLeftX = windowArea.minX;
                        targetX = Math.min(targetX, guiResW);
                        float rawWidth = targetX - fixedLeftX;
                        float newWidth = Math.max(minWinWidth, rawWidth);
                        windowArea.maxX = Math.min(fixedLeftX + newWidth, guiResW);
                    }
                    if (jui.isDragged(trCornerID, JuiState.MOUSE_LEFT)) {
                        Vector2f cornerAtDragStart = jui.getVec2f(trCornerID, windowArea.maxX, windowArea.maxY);
                        if (jui.justDragged(trCornerID)) cornerAtDragStart.set(windowArea.maxX, windowArea.maxY);
                        float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                        float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                        float fixedLeftX = windowArea.minX;
                        float fixedBottomY = windowArea.minY;
                        targetX = Math.min(targetX, guiResW);
                        targetY = Math.min(targetY, guiResH);
                        float rawWidth = targetX - fixedLeftX;
                        float rawHeight = targetY - fixedBottomY;
                        float newWidth = Math.max(minWinWidth, rawWidth);
                        float newHeight = Math.max(minWinHeight, rawHeight);
                        windowArea.maxX = Math.min(fixedLeftX + newWidth, guiResW);
                        windowArea.maxY = Math.min(fixedBottomY + newHeight, guiResH);
                    }
                    if (jui.isDragged(brCornerID, JuiState.MOUSE_LEFT)) {
                        Vector2f cornerAtDragStart = jui.getVec2f(brCornerID, windowArea.maxX, windowArea.minY);
                        if (jui.justDragged(brCornerID)) cornerAtDragStart.set(windowArea.maxX, windowArea.minY);
                        float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                        float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                        float fixedLeftX = windowArea.minX;
                        float fixedTopY = windowArea.maxY;
                        targetX = Math.min(targetX, guiResW);
                        targetY = Math.max(targetY, 0f);
                        float rawWidth = targetX - fixedLeftX;
                        float rawHeight = fixedTopY - targetY;
                        float newWidth = Math.max(minWinWidth, rawWidth);
                        float newHeight = Math.max(minWinHeight, rawHeight);
                        windowArea.maxX = Math.min(fixedLeftX + newWidth, guiResW);
                        windowArea.minY = Math.max(fixedTopY - newHeight, 0f);
                    }
                    if (jui.isDragged(blCornerID, JuiState.MOUSE_LEFT)) {
                        Vector2f cornerAtDragStart = jui.getVec2f(blCornerID, windowArea.minX, windowArea.minY);
                        if (jui.justDragged(blCornerID)) cornerAtDragStart.set(windowArea.minX, windowArea.minY);
                        float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                        float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                        float fixedRightX = windowArea.maxX;
                        float fixedTopY = windowArea.maxY;
                        targetX = Math.max(targetX, 0f);
                        targetY = Math.max(targetY, 0f);
                        float rawWidth = fixedRightX - targetX;
                        float rawHeight = fixedTopY - targetY;
                        float newWidth = Math.max(minWinWidth, rawWidth);
                        float newHeight = Math.max(minWinHeight, rawHeight);
                        windowArea.minX = Math.max(fixedRightX - newWidth, 0f);
                        windowArea.minY = Math.max(fixedTopY - newHeight, 0f);
                    }
                    if (jui.isDragged(tlCornerID, JuiState.MOUSE_LEFT)) {
                        Vector2f cornerAtDragStart = jui.getVec2f(tlCornerID, windowArea.minX, windowArea.maxY);
                        if (jui.justDragged(tlCornerID)) cornerAtDragStart.set(windowArea.minX, windowArea.maxY);
                        float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                        float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                        float fixedRightX = windowArea.maxX;
                        float fixedBottomY = windowArea.minY;
                        targetX = Math.max(targetX, 0f);
                        targetY = Math.min(targetY, guiResH);
                        float rawWidth = fixedRightX - targetX;
                        float rawHeight = targetY - fixedBottomY;
                        float newWidth = Math.max(minWinWidth, rawWidth);
                        float newHeight = Math.max(minWinHeight, rawHeight);
                        windowArea.minX = Math.max(fixedRightX - newWidth, 0f);
                        windowArea.maxY = Math.min(fixedBottomY + newHeight, guiResH);
                    }
                }
            }
            if (signaledToCenter) {
                Rectanglef screen = pushRect(guiResW,guiResH);
                LayoutUtils.center(screen, windowArea);
                popRect();
            }
        } else {
            animating = false;
            animProgress = 1.0f;
            restoredArea.set(windowArea);
        }
        signaledToCenter = false;
        signaledToRestore = false;
        signaledToMaximize = false;

        LayoutUtils.confine(0,0,guiResW,guiResH, windowArea);
        contentArea.set(windowArea);
        jui.drawRect(contentArea, BG_COLOR,bgID);

        {
            jui.drawBorders(contentArea, WINDOW_BORDER_TOTAL,false, WINDOW_EDGE_COLOR, bgID);
            LayoutUtils.pad(contentArea, WINDOW_BORDER_TOTAL);

            Rectanglef tmp = pushRect();
            Rectanglef center = LayoutUtils.pad(pushRect(contentArea),-WINDOW_PADDING);
            Color COLOR = WINDOW_BORDER_COLOR.defaultColor();
            Color COLOR_HOV = WINDOW_BORDER_COLOR.hoveredColor();

            LayoutUtils.borderTop(contentArea,tmp,WINDOW_BORDER_TOTAL,true); // invis
            jui.drawRect(LayoutUtils.pad(tmp,-1), Color.CLEAR,tBorderID);
            LayoutUtils.borderTop(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(tBorderID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,tBorderID);
            LayoutUtils.borderRight(contentArea,tmp,WINDOW_BORDER_TOTAL,true); // invis
            jui.drawRect(LayoutUtils.pad(tmp,-1), Color.CLEAR,rBorderID);
            LayoutUtils.borderRight(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(rBorderID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,rBorderID);
            LayoutUtils.borderBottom(contentArea,tmp,WINDOW_BORDER_TOTAL,true); // invis
            jui.drawRect(LayoutUtils.pad(tmp,-1), Color.CLEAR,bBorderID);
            LayoutUtils.borderBottom(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(bBorderID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,bBorderID);
            LayoutUtils.borderLeft(contentArea,tmp,WINDOW_BORDER_TOTAL,true); // invis
            jui.drawRect(LayoutUtils.pad(tmp,-1), Color.CLEAR,lBorderID);
            LayoutUtils.borderLeft(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(lBorderID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,lBorderID);

            LayoutUtils.borderTopRight(contentArea,tmp, WINDOW_BORDER_TOTAL,true);
            jui.drawRect(LayoutUtils.pad(tmp,-2), Color.CLEAR,trCornerID);
            LayoutUtils.borderTopRight(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(trCornerID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,trCornerID);
            LayoutUtils.borderBottomRight(contentArea,tmp, WINDOW_BORDER_TOTAL,true);
            jui.drawRect(LayoutUtils.pad(tmp,-2), Color.CLEAR,brCornerID);
            LayoutUtils.borderBottomRight(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(brCornerID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,brCornerID);
            LayoutUtils.borderBottomLeft(contentArea,tmp, WINDOW_BORDER_TOTAL,true);
            jui.drawRect(LayoutUtils.pad(tmp,-2), Color.CLEAR,blCornerID);
            LayoutUtils.borderBottomLeft(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(blCornerID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,blCornerID);
            LayoutUtils.borderTopLeft(contentArea,tmp, WINDOW_BORDER_TOTAL,true);
            jui.drawRect(LayoutUtils.pad(tmp,-2), Color.CLEAR,tlCornerID);
            LayoutUtils.borderTopLeft(center,tmp,WINDOW_BORDER,true); // main
            jui.drawRect(tmp,jui.isPressedOrHovered(tlCornerID,JuiState.MOUSE_LEFT) ? COLOR_HOV : COLOR,tlCornerID);

            JgenUtils.popRect(2);
        }

        {
            // NAV
            jui.pushContainerAbsolute(contentArea, Axis.VERTICAL);
            Rectanglef navBar = jui.allocate(NAV_HEIGHT,pushRect());
            Color color = NAV_COLOR.hoverLerp(jui.isPressedOrHovered(navID,JuiState.MOUSE_LEFT), jui.pressedHoveredDuration());
            jui.drawBox(navBar,NAV_BORDER,false,color,navID);
            jui.drawLabel(name,navBar,NAV_FONT,NAV_LABEL_COLOR, TextAlignment.CENTER);
            popRect();
            jui.allocateRemaining(contentArea);
            jui.popContainer();
        }
        return bgID;
    }


    public String name() { return name; }
    public void open() { open = true; }
    public void close() { open = false; }
    public boolean isOpen() { return open; }
    public void center() { if (resizable) signaledToCenter = true; }
    public void maximize() { if (resizable) signaledToMaximize = true; }
    public void resore() { if (resizable) signaledToRestore = true; }
    public boolean isResizable() { return resizable; }
    public void enableResizable(boolean enable) { resizable = enable; }

    public void onRegister(JuiCore jui) {
        if (!registered) {
            float B = WINDOW_BORDER_TOTAL;
            float N = NAV_HEIGHT;
            windowArea.set(contentArea);
            LayoutUtils.pad(windowArea,-(N + B),-B,-B,-B);
            Rectanglef screen = pushRect(jui.virtualWidth(),jui.virtualHeight());
            LayoutUtils.center(screen, windowArea);
            JgenUtils.popRect();
            contentArea.set(windowArea);
            LayoutUtils.pad(contentArea,(N + B),B,B,B);
            minWinWidth = windowArea.lengthX();
            minWinHeight = windowArea.lengthY();
            registered = true;
        } else Logger.warn("DebufWindow: \"{}\", already registered",name);
    }
}
