package io.github.nascentlogic.jgen.gui.debug;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.Mouse;
import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.JuiCore;
import io.github.nascentlogic.jgen.gui.LayoutUtils;
import io.github.nascentlogic.jgen.gui.api.Axis;
import io.github.nascentlogic.jgen.gui.api.JuiState;
import io.github.nascentlogic.jgen.gui.api.TextAlignment;
import org.joml.primitives.Rectanglef;

import static io.github.nascentlogic.jgen.gui.LayoutUtils.scrollApplyScroll;
import static io.github.nascentlogic.jgen.gui.LayoutUtils.scrollContentRatio;
import static io.github.nascentlogic.jgen.gui.debug.DebugStyle.*;
import static io.github.nascentlogic.jgen.utils.JgenUtils.*;

/**
 * F.Dahl, 10/6/2026
 */
public class DebugLayout {

    private static int ABSOLUTE_DEPTH;
    private static int SCROLL_DEPTH;
    private static int GROUP_DEPTH;
    private static int LIST_DEPTH;



    public static void beginAbsolute(JuiCore jui, Rectanglef absoluteArea) {
        jui.scissorPush(absoluteArea,true);
        jui.pushContainerAbsolute(absoluteArea, Axis.VERTICAL);
        ABSOLUTE_DEPTH++;
    }

    public static void endAbsolute(JuiCore jui) {
        jui.popContainer();
        jui.scissorPop();
        ABSOLUTE_DEPTH--;
    }

    public static boolean beginGroup(JuiCore jui, String name) {
        int scopeID = jui.pushID(name);
        jui.pushContainerAuto(Axis.VERTICAL);
        boolean open        = jui.getBool(scopeID,false);
        boolean hovered     = jui.isHovered(scopeID);
        boolean selected    = jui.justClicked(scopeID, JuiState.MOUSE_LEFT);
        boolean pressed     = jui.isPressed(scopeID,JuiState.MOUSE_LEFT);
        if (selected) {
            open = !open;
            jui.persistentPut(scopeID,open);
        }
        final int D = Math.min(GROUP_DEPTH,GROUP_HEIGHT.length - 1);

        Rectanglef banner = jui.allocate(GROUP_HEIGHT[0],pushRect()); // banner
        jui.drawRect(banner,Color.CLEAR,scopeID);
        if (hovered && !pressed) {
            banner.maxY += GROUP_HOVER_TRANSFORM;
            banner.maxX -= GROUP_HOVER_TRANSFORM;
            banner.minX += GROUP_HOVER_TRANSFORM;
        }
        Color labelColor = GROUP_LABEL_COLORS[D];
        Color bannerColor = GROUP_COLORS[D].hoverLerp(hovered, jui.hoveredDuration());
        jui.drawBox(banner,GROUP_BORDER,false,bannerColor,scopeID);
        jui.drawLabel(name,banner,GROUP_FONT,labelColor, hovered, TextAlignment.CENTER);

        popRect(); // banner
        GROUP_DEPTH++;
        return open;
    }


    public static void endGroup(JuiCore jui) {
        jui.popID();
        jui.popContainer();
        GROUP_DEPTH--;
    }



    private static final class ListState {
        int heightInItems;
        int itemCount; // reset at begin list
        float scrollAmount; // items scrolled. reset at begin list
        float scrollNormalized;
        boolean validSize;
        boolean elementClicked;
        ListState(int heightInItems) {
            this.heightInItems = heightInItems;
            this.validSize = true;
        }
    }







    public static void beginScrollContainer(JuiCore jui, float size) {
        final float trackWidth = 8;
        final int SCROLL_NORMAL = 0;
        final int CONTENT_HEIGHT = 1;
        final int D = Math.min(GROUP_DEPTH, GROUP_COLORS.length - 1);
        int scopeID = jui.pushID(SCROLL_DEPTH + 66);
        float[] state = jui.getObj(scopeID, float[].class, () -> new float[2]);

        Rectanglef absoluteBounds = jui.allocate(size,pushRect());
        boolean visibleTrack = size < state[CONTENT_HEIGHT] && absoluteBounds.lengthX() > trackWidth;

        float contentOffset = 0;

        if (visibleTrack) {

            int trackID = jui.getID(28);
            int handleID = jui.getID(31);

            float viewHeight = absoluteBounds.lengthY();
            float contentHeight = state[CONTENT_HEIGHT];
            float contentRatio = scrollContentRatio(viewHeight,contentHeight);

            if (jui.isFocused(handleID)) {
                if (absoluteBounds.containsPoint(jui.mousePosX(), jui.mousePosY())) {
                    if (jui.justScrolled()) state[SCROLL_NORMAL] = scrollApplyScroll(viewHeight,contentHeight,state[SCROLL_NORMAL],jui.mouseScroll(), GROUP_HEIGHT[D]);
                }
            }
            float trackHeight = viewHeight;
            float handleHeight = trackHeight * contentRatio;
            float trackSpace = Math.max(trackHeight - handleHeight, 0);

            if (jui.isDragged(handleID)) {
                jui.focusSteal(handleID);
                if (jui.justDragged(handleID)) jui.persistentPut(handleID,state[SCROLL_NORMAL]);
                float initialDragNormal = jui.getFloat(handleID,state[SCROLL_NORMAL]);
                state[SCROLL_NORMAL] = LayoutUtils.scrollApplyDragNorm(initialDragNormal,-jui.mouseDragVectorY(),trackHeight,handleHeight);
            } else if (jui.justClicked(handleID,JuiState.MOUSE_LEFT)) {
                jui.focusSteal(handleID);
            }

            contentOffset = LayoutUtils.scrollOffset(viewHeight,contentHeight,state[SCROLL_NORMAL]);

            if (trackSpace > 0) {
                Rectanglef scrollHandle = pushRect(absoluteBounds);

                Color handleColor = GROUP_COLORS[Math.min(GROUP_DEPTH, GROUP_COLORS.length - 1)]
                        .hoverLerp(jui.isPressedOrHovered(handleID),jui.pressedHoveredDuration());
                scrollHandle.minX += (scrollHandle.lengthX() - trackWidth);
                jui.drawRect(scrollHandle,BG_COLOR_DARK,handleID);
                scrollHandle.maxY -= (trackSpace * state[SCROLL_NORMAL]);
                scrollHandle.minY = scrollHandle.maxY - handleHeight;
                jui.drawBox(scrollHandle,2,false,handleColor,handleID);
                LayoutUtils.pad(scrollHandle,-2);
                jui.drawRect(scrollHandle,Color.CLEAR, handleID);

                popRect();
            }
            absoluteBounds.maxX -= trackWidth;
        }
        jui.scissorPush(absoluteBounds,true); // scissor before translation
        absoluteBounds.translate(0,contentOffset);
        jui.pushContainerAbsolute(absoluteBounds,Axis.VERTICAL);
        jui.pushContainerAuto(Axis.VERTICAL); // content
        popRect();
        SCROLL_DEPTH++;
    }


    public static void endScrollContainer(JuiCore jui) {
        final int CONTENT_HEIGHT = 1;
        float[] state = jui.getObj(jui.scopeID(), float[].class, () -> new float[2]);
        state[CONTENT_HEIGHT] = jui.popContainer().offset; // space allocated on the autocontainer
        jui.popContainer(); // pop The absolute
        jui.scissorPop();
        jui.popID();
        SCROLL_DEPTH--;
    }









    // height in items
    public static void beginList(JuiCore jui, String name, int heightInItems) {
        if (LIST_DEPTH != 0) throw new IllegalStateException();
        int scopeID = jui.pushID(name);
        ListState state = jui.getObj(scopeID, ListState.class, () -> new ListState(heightInItems));
        int lastCount = state.itemCount;
        float scrollAmount = state.scrollAmount;
        state.heightInItems = heightInItems;
        state.itemCount = 0;
        state.scrollAmount = 0;
        state.elementClicked = false;


        jui.pushContainerAuto(Axis.VERTICAL);

        float containerHeight = FIELD_MARGIN * 2 + FIELD_BORDER * 2 + FIELD_BORDER * 2 + heightInItems * LIST_ITEM_HEIGHT;
        Rectanglef rect = LayoutUtils.pad(jui.allocate(containerHeight,pushRect()), FIELD_MARGIN);
        jui.drawBorders(rect, FIELD_BORDER,false,BG_COLOR_LIGHT,scopeID);
        LayoutUtils.pad(rect, FIELD_BORDER);

        jui.pushContainerAbsolute(rect,0,Axis.HORIZONTAL,true);

        float yOffset = 0;

        if (lastCount > 0 && lastCount > heightInItems) {

            if (jui.isFocused(scopeID)) {
                if (jui.navigateDown()) scrollAmount -= 1;
                if (jui.navigatedUp()) scrollAmount += 1;

            }

            float viewHeight = heightInItems * LIST_ITEM_HEIGHT;
            float contentHeight = lastCount * LIST_ITEM_HEIGHT;
            state.scrollNormalized = scrollApplyScroll(
                    viewHeight,contentHeight,state.scrollNormalized,scrollAmount,LIST_ITEM_HEIGHT);

            float contentHeightRatio = scrollContentRatio(viewHeight,contentHeight);
            float trackBarHeight = rect.lengthY();// - (2 * FIELD_BORDER);
            float handleHeight = trackBarHeight * contentHeightRatio;
            float trackSpace = Math.max(trackBarHeight - handleHeight,0);

            int scrollHandleID = jui.getID("scrollH");
            if (jui.isDragged(scrollHandleID,JuiState.MOUSE_LEFT)) {
                if (jui.justDragged(scrollHandleID)) jui.persistentPut(scrollHandleID,state.scrollNormalized);
                float initialDragNormal = jui.getFloat(scrollHandleID,state.scrollNormalized);
                state.scrollNormalized = LayoutUtils.scrollApplyDragNorm(initialDragNormal,-jui.mouseDragVectorY(),trackBarHeight,handleHeight);
            }

            yOffset = LayoutUtils.scrollOffset(viewHeight,contentHeight,state.scrollNormalized);

            jui.allocate(TRACK_WIDTH,rect);

            if (trackSpace > 0) {
                Rectanglef trackBar = pushRect(rect);
                trackBar.maxY -= trackSpace * state.scrollNormalized;
                trackBar.minY = trackBar.maxY - handleHeight;
                Color trackColor = GROUP_COLORS[Math.min(GROUP_DEPTH, GROUP_COLORS.length - 1)].hoverLerp(jui.isPressedOrHovered(scrollHandleID),jui.pressedHoveredDuration());
                jui.drawRect(trackBar,trackColor, scrollHandleID);
                popRect();
            }

            jui.allocate(LIST_BORDER,rect);
            jui.drawRect(rect,BG_COLOR_LIGHT, scopeID);
        } else {
            state.scrollNormalized = 0;
        }



        jui.allocateRemaining(rect);
        LayoutUtils.pad(rect,FIELD_PADDING);
        jui.drawRect(rect,BG_COLOR_DARK, scopeID);
        jui.scissorPush(rect,true);

        rect.maxY += yOffset;
        jui.pushContainerAbsolute(rect,Axis.VERTICAL);





        popRect();

        LIST_DEPTH++;
    }

    public static int listItem(JuiCore jui, String label, boolean selected) {
        int scopeID = jui.scopeID();
        ListState state = jui.getObj(scopeID,ListState.class,() -> new ListState(0));

        int itemID = jui.getID(state.itemCount);
        boolean hovered = jui.isHovered(itemID);
        boolean clicked = jui.justClicked(itemID,JuiState.MOUSE_LEFT);
        boolean dClicked = jui.justDoubleClicked(itemID,JuiState.MOUSE_LEFT);
        if (clicked) state.elementClicked = true;
        Rectanglef rect = jui.allocate(LIST_ITEM_HEIGHT,pushRect());

        if (jui.scissorIntersects(rect)) {
            Mouse mouse = Jgen.get().mouse();
            if (hovered && mouse.scrolled()) state.scrollAmount += mouse.scrollValue();
            if (selected || clicked) {
                jui.drawRect(rect,BG_COLOR_LIGHT,itemID);
            }
            else jui.drawRect(rect,Color.CLEAR,itemID);
            Color labelColor = hovered ? LIST_LABEL_COLOR.hoveredColor() : LIST_LABEL_COLOR.defaultColor();
            jui.drawLabel(label,rect,LIST_FONT,labelColor,(selected || clicked),TextAlignment.LEFT);
        }

        popRect();
        state.itemCount++;
        return dClicked ? 2 : clicked ? 1 : 0;
    }



    public static void endList(JuiCore jui) {
        if (LIST_DEPTH != 1) throw new IllegalStateException();
        ListState state = jui.getObj(jui.scopeID(),ListState.class,() -> new ListState(0));
        if (state.elementClicked) {
            jui.focusSteal(jui.scopeID());
        }
        jui.popContainer(); // this is insane. Somethings redundant here
        jui.popContainer();
        jui.popContainer();
        jui.scissorPop();
        jui.popID();
        LIST_DEPTH--;
    }


    public static Rectanglef drawField(JuiCore jui, Rectanglef container, int id) {
        LayoutUtils.pad(container, FIELD_MARGIN);
        jui.drawBorders(container, FIELD_BORDER,false,BG_COLOR_LIGHT,id);
        LayoutUtils.pad(container, FIELD_BORDER + FIELD_PADDING);
        jui.drawRect(container,BG_COLOR_DARK,id);
        return container;
    }


    // public static void drawGraph

}
