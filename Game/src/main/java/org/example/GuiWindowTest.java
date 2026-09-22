package org.example;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.gui.JuiCore;
import io.github.nascentlogic.jgen.gui.api.JuiStateAPI;
import io.github.nascentlogic.jgen.gui.text.ManagedText;
import io.github.nascentlogic.jgen.gui.text.TextBlock;
import io.github.nascentlogic.jgen.gui.util.Axis;
import io.github.nascentlogic.jgen.gui.util.Container;
import io.github.nascentlogic.jgen.gui.util.LayoutUtils;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.Vector2f;
import org.joml.primitives.Rectanglef;

import java.util.ArrayList;
import java.util.List;

/**
 * F.Dahl, 9/18/2026
 */
public class GuiWindowTest implements Disposable {

    private static final float ANIM_DURATION = 0.33f;

    private static final class WindowState {
        boolean closed = false;
        boolean resizable = true;


        Rectanglef currentArea;
        Rectanglef restoredArea = new Rectanglef();
        Rectanglef animOrigin = new Rectanglef();
        Rectanglef animTarget = new Rectanglef();
        float animProgress = 1.0f;
        boolean signaledToRestore = false;
        boolean signaledToMaximize = false;
        boolean animating = false;

        final float minWidth;
        final float minHeight;
        WindowState(float x, float y, float w, float h) {
            currentArea = new Rectanglef(x,y,x+w,y+h);
            restoredArea.set(currentArea);
            minWidth = w;
            minHeight = h;
        }
    }


    private static final class Item implements Disposable {
        final ManagedText description;
        final ManagedText name;
        final Color color;
        Item(String name, String description, String colorHex) {
            this.description = new TextBlock(description);
            this.name = new TextBlock(name);
            this.color = new Color(colorHex);
        } public void free() {
            Disposable.free(description,name);
        }
    }


    private final List<Item> playerInventory = new ArrayList<>();
    private final List<Item> shopInventory = new ArrayList<>();
    private final Rectanglef tmpRect1 = new Rectanglef();
    private final Rectanglef tmpRect2 = new Rectanglef();

    int inventoryBorderThickness = 4;
    int inventoryWindowW = 400;
    int inventoryWindowH = 400;
    int inventoryNavbarHeight = 20;
    final Color inventoryNavColor = new Color("883333FF");
    final Color inventoryNavColorHovered = new Color("CC5555FF");
    final Color inventoryBgColor = new Color("333333FF");
    final Color inventoryBorderColor = new Color("777777FF");
    final Color inventoryBorderColorHovered = new Color("BBBBBBFF");

    GuiWindowTest() {
        initializeInventories();
    }


    public void render(JuiCore jui) {
        float guiResW = jui.resolutionWidth();
        float guiResH = jui.resolutionHeight();
        float horizontalPadding = 20f;
        float combinedWidth = inventoryWindowW * 2 + horizontalPadding;
        float playerInventoryX = (guiResW - combinedWidth) * 0.5f;
        float shopInventoryX = playerInventoryX + horizontalPadding + inventoryWindowW;
        float inventoryY = (guiResH - inventoryWindowH) * 0.5f;

        renderInventory("Player Inventory",playerInventory,jui,playerInventoryX,inventoryY);
        renderInventory("Shop Inventory",shopInventory,jui,shopInventoryX,inventoryY);
    }

    private void renderInventory(String name, List<Item> items, JuiCore jui, float defaultPosX, float defaultPosY) {

        int windowID = jui.pushID(name);
        WindowState windowState = jui.getObj(windowID, WindowState.class, () -> new WindowState(defaultPosX,defaultPosY,inventoryWindowW,inventoryWindowH));
        if (windowState.closed) return;

        int navID       = jui.getID("nav");
        int tBorderID   = jui.getID("tb");
        int rBorderID   = jui.getID("rb");
        int bBorderID   = jui.getID("bb");
        int lBorderID   = jui.getID("lb");
        int trCornerID  = jui.getID("trc");
        int brCornerID  = jui.getID("brc");
        int blCornerID  = jui.getID("blc");
        int tlCornerID  = jui.getID("tlc");

        int guiResW = jui.resolutionWidth();
        int guiResH = jui.resolutionHeight();


        Rectanglef winBounds = windowState.currentArea;

        if (windowState.resizable) {

            boolean maximized = windowState.currentArea.lengthX() == guiResW
                    && windowState.currentArea.lengthY() == guiResH;

            if (windowState.signaledToMaximize && !maximized) {
                if (windowState.animating) {
                    boolean maximizing = windowState.animTarget.lengthX() == guiResW && windowState.animTarget.lengthY() == guiResH;
                    if (!maximizing) {
                        Rectanglef tmp = new Rectanglef(windowState.animOrigin);
                        windowState.animOrigin.set(windowState.animTarget);
                        windowState.animTarget.set(tmp);
                        windowState.animProgress = 1.0f - windowState.animProgress;
                    }
                } else {
                    windowState.restoredArea.set(windowState.currentArea);
                    windowState.animOrigin.set(windowState.currentArea);
                    windowState.animTarget.setMin(0,0).setMax(guiResW,guiResH);
                    windowState.animProgress = 0.0f;
                    windowState.animating = true;
                }
            } else if (windowState.signaledToRestore) {
                if (windowState.animating) {
                    boolean restoring = windowState.animTarget.equals(windowState.restoredArea);
                    if (!restoring) {
                        Rectanglef tmp = new Rectanglef(windowState.animOrigin);
                        windowState.animOrigin.set(windowState.animTarget);
                        windowState.animTarget.set(tmp);
                        windowState.animProgress = 1.0f - windowState.animProgress;
                    }
                } else if (maximized) {
                    windowState.animOrigin.set(windowState.currentArea);
                    windowState.animTarget.set(windowState.restoredArea);
                    windowState.animProgress = 0.0f;
                    windowState.animating = true;
                }
            }

            windowState.signaledToRestore = false;
            windowState.signaledToMaximize = false;

            if (windowState.animating) {
                windowState.animProgress += (float) Jgen.get().time().deltaTime() / ANIM_DURATION;
                windowState.animProgress = Math.clamp(windowState.animProgress,0.0f,1.0f);
                float t = 1.0f - (float) Math.pow(1.0f - windowState.animProgress, 3.0);// Cubic Ease-Out
                windowState.currentArea.minX = windowState.animOrigin.minX + (windowState.animTarget.minX - windowState.animOrigin.minX) * t;
                windowState.currentArea.minY = windowState.animOrigin.minY + (windowState.animTarget.minY - windowState.animOrigin.minY) * t;
                windowState.currentArea.maxX = windowState.animOrigin.maxX + (windowState.animTarget.maxX - windowState.animOrigin.maxX) * t;
                windowState.currentArea.maxY = windowState.animOrigin.maxY + (windowState.animTarget.maxY - windowState.animOrigin.maxY) * t;
                if (windowState.animProgress >= 1.0f) windowState.animating = false;
            } else if (!maximized) {
                windowState.restoredArea.set(windowState.currentArea);
            }

        }



        if (jui.justSelected(navID)) {
            boolean maximized = windowState.currentArea.lengthX() == guiResW
                    && windowState.currentArea.lengthY() == guiResH;
            if (maximized) {
                if (windowState.resizable)
                    windowState.signaledToRestore = true;
            } else {
                if (windowState.resizable)
                    windowState.signaledToMaximize = true;
            }

        }


        if (jui.currentDraggedID() != JuiStateAPI.NULL) {
            if (jui.isDragged(navID, JuiStateAPI.MOUSE_LEFT)) {
                Vector2f windowAtdragStart = jui.getVec2f(navID,winBounds.minX,winBounds.minY);
                if (jui.justDragged(navID)) windowAtdragStart.set(winBounds.minX,winBounds.minY);
                float targetX = windowAtdragStart.x + jui.mouseDragVectorX();
                float targetY = windowAtdragStart.y + jui.mouseDragVectorY();
                float width = winBounds.lengthX();
                float height = winBounds.lengthY();
                winBounds.setMin(targetX,targetY).setMax(targetX + width, targetY + height);
            }
            if (jui.isDragged(tBorderID,JuiStateAPI.MOUSE_LEFT)) {
                if (jui.justDragged(tBorderID)) jui.persistentPut(tBorderID, winBounds.maxY);
                float borderAtDragStart = jui.getFloat(tBorderID, winBounds.maxY);
                float targetY = borderAtDragStart + jui.mouseDragVectorY();
                float fixedBottomY = winBounds.minY;
                targetY = Math.min(targetY, guiResH);
                float rawHeight = targetY - fixedBottomY;
                float newHeight = Math.max(windowState.minHeight, rawHeight);
                winBounds.maxY = Math.min(fixedBottomY + newHeight, guiResH);
            }
            if (jui.isDragged(bBorderID,JuiStateAPI.MOUSE_LEFT)) {
                if (jui.justDragged(bBorderID)) jui.persistentPut(bBorderID, winBounds.minY);
                float borderAtDragStart = jui.getFloat(bBorderID, winBounds.minY);
                float targetY = borderAtDragStart + jui.mouseDragVectorY();
                float fixedTopY = winBounds.maxY;
                targetY = Math.max(targetY, 0f);
                float rawHeight = fixedTopY - targetY;
                float newHeight = Math.max(windowState.minHeight, rawHeight);
                winBounds.minY = Math.max(fixedTopY - newHeight, 0f);
            }
            if (jui.isDragged(lBorderID,JuiStateAPI.MOUSE_LEFT)) {
                if (jui.justDragged(lBorderID)) jui.persistentPut(lBorderID, winBounds.minX);
                float borderAtDragStart = jui.getFloat(lBorderID, winBounds.minX);
                float targetX = borderAtDragStart + jui.mouseDragVectorX();
                float fixedRightX = winBounds.maxX;
                targetX = Math.max(targetX, 0f);
                float rawWidth = fixedRightX - targetX;
                float newWidth = Math.max(windowState.minWidth, rawWidth);
                winBounds.minX = Math.max(fixedRightX - newWidth, 0f);
            }
            if (jui.isDragged(rBorderID)) {
                if (jui.justDragged(rBorderID)) jui.persistentPut(rBorderID, winBounds.maxX);
                float borderAtDragStart = jui.getFloat(rBorderID, winBounds.maxX);
                float targetX = borderAtDragStart + jui.mouseDragVectorX();
                float fixedLeftX = winBounds.minX;
                targetX = Math.min(targetX, guiResW);
                float rawWidth = targetX - fixedLeftX;
                float newWidth = Math.max(windowState.minWidth, rawWidth);
                winBounds.maxX = Math.min(fixedLeftX + newWidth, guiResW);
            }
            if (jui.isDragged(trCornerID, JuiStateAPI.MOUSE_LEFT)) {
                Vector2f cornerAtDragStart = jui.getVec2f(trCornerID, winBounds.maxX, winBounds.maxY);
                if (jui.justDragged(trCornerID)) cornerAtDragStart.set(winBounds.maxX, winBounds.maxY);
                float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                float fixedLeftX = winBounds.minX;
                float fixedBottomY = winBounds.minY;
                targetX = Math.min(targetX, guiResW);
                targetY = Math.min(targetY, guiResH);
                float rawWidth = targetX - fixedLeftX;
                float rawHeight = targetY - fixedBottomY;
                float newWidth = Math.max(windowState.minWidth, rawWidth);
                float newHeight = Math.max(windowState.minHeight, rawHeight);
                winBounds.maxX = Math.min(fixedLeftX + newWidth, guiResW);
                winBounds.maxY = Math.min(fixedBottomY + newHeight, guiResH);
            }
            if (jui.isDragged(brCornerID, JuiStateAPI.MOUSE_LEFT)) {
                Vector2f cornerAtDragStart = jui.getVec2f(brCornerID, winBounds.maxX, winBounds.minY);
                if (jui.justDragged(brCornerID)) cornerAtDragStart.set(winBounds.maxX, winBounds.minY);
                float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                float fixedLeftX = winBounds.minX;
                float fixedTopY = winBounds.maxY;
                targetX = Math.min(targetX, guiResW);
                targetY = Math.max(targetY, 0f);
                float rawWidth = targetX - fixedLeftX;
                float rawHeight = fixedTopY - targetY;
                float newWidth = Math.max(windowState.minWidth, rawWidth);
                float newHeight = Math.max(windowState.minHeight, rawHeight);
                winBounds.maxX = Math.min(fixedLeftX + newWidth, guiResW);
                winBounds.minY = Math.max(fixedTopY - newHeight, 0f);
            }
            if (jui.isDragged(blCornerID, JuiStateAPI.MOUSE_LEFT)) {
                Vector2f cornerAtDragStart = jui.getVec2f(blCornerID, winBounds.minX, winBounds.minY);
                if (jui.justDragged(blCornerID)) cornerAtDragStart.set(winBounds.minX, winBounds.minY);
                float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                float fixedRightX = winBounds.maxX;
                float fixedTopY = winBounds.maxY;
                targetX = Math.max(targetX, 0f);
                targetY = Math.max(targetY, 0f);
                float rawWidth = fixedRightX - targetX;
                float rawHeight = fixedTopY - targetY;
                float newWidth = Math.max(windowState.minWidth, rawWidth);
                float newHeight = Math.max(windowState.minHeight, rawHeight);
                winBounds.minX = Math.max(fixedRightX - newWidth, 0f);
                winBounds.minY = Math.max(fixedTopY - newHeight, 0f);
            }
            if (jui.isDragged(tlCornerID, JuiStateAPI.MOUSE_LEFT)) {
                Vector2f cornerAtDragStart = jui.getVec2f(tlCornerID, winBounds.minX, winBounds.maxY);
                if (jui.justDragged(tlCornerID)) cornerAtDragStart.set(winBounds.minX, winBounds.maxY);
                float targetX = cornerAtDragStart.x + jui.mouseDragVectorX();
                float targetY = cornerAtDragStart.y + jui.mouseDragVectorY();
                float fixedRightX = winBounds.maxX;
                float fixedBottomY = winBounds.minY;
                targetX = Math.max(targetX, 0f);
                targetY = Math.min(targetY, guiResH);
                float rawWidth = fixedRightX - targetX;
                float rawHeight = targetY - fixedBottomY;
                float newWidth = Math.max(windowState.minWidth, rawWidth);
                float newHeight = Math.max(windowState.minHeight, rawHeight);
                winBounds.minX = Math.max(fixedRightX - newWidth, 0f);
                winBounds.maxY = Math.min(fixedBottomY + newHeight, guiResH);
            }
        }





        LayoutUtils.confine(0,0,guiResW,guiResH,winBounds); // keep it inside screen

        jui.deferredEnable();
        jui.drawLabel(name,winBounds.minX + 20, winBounds.maxY - 20, 1,20,Color.WHITE);
        jui.deferredDisable();

        jui.drawRect(winBounds,inventoryBgColor);




        int windowBorderThickness = Math.max(0,inventoryBorderThickness);
        Rectanglef windowContentBounds = tmpRect1.set(windowState.currentArea);

        if (windowBorderThickness > 0) {
            LayoutUtils.pad(windowContentBounds,windowBorderThickness);

            LayoutUtils.borderTop(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(tBorderID) ? inventoryBorderColorHovered : inventoryBorderColor,tBorderID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-2),Color.CLEAR,tBorderID);

            LayoutUtils.borderRight(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(rBorderID) ? inventoryBorderColorHovered : inventoryBorderColor,rBorderID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-2),Color.CLEAR,rBorderID);

            LayoutUtils.borderBottom(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(bBorderID) ? inventoryBorderColorHovered : inventoryBorderColor,bBorderID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-2),Color.CLEAR,bBorderID);

            LayoutUtils.borderLeft(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(lBorderID) ? inventoryBorderColorHovered : inventoryBorderColor,lBorderID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-2),Color.CLEAR,lBorderID);

            LayoutUtils.borderTopRight(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(trCornerID) ? inventoryBorderColorHovered : inventoryBorderColor,trCornerID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-4),Color.CLEAR,trCornerID);

            LayoutUtils.borderBottomRight(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(brCornerID) ? inventoryBorderColorHovered : inventoryBorderColor,brCornerID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-4),Color.CLEAR,brCornerID);

            LayoutUtils.borderBottomLeft(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(blCornerID) ? inventoryBorderColorHovered : inventoryBorderColor,blCornerID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-4),Color.CLEAR,blCornerID);

            LayoutUtils.borderTopLeft(windowContentBounds,tmpRect2,windowBorderThickness,true);
            jui.drawRect(tmpRect2,jui.isHovered(tlCornerID) ? inventoryBorderColorHovered : inventoryBorderColor,tlCornerID);
            jui.drawRect(LayoutUtils.pad(tmpRect2,-4),Color.CLEAR,tlCornerID);

        }





        jui.pushContainerAbsolute(windowContentBounds,4,Axis.VERTICAL);
        Rectanglef navBar = jui.allocateSpace(inventoryNavbarHeight,tmpRect1);
        boolean navHovered = jui.isHovered(navID) || jui.isDragged(navID);
        jui.drawRect(navBar,navHovered ? inventoryNavColorHovered : inventoryNavColor,navID);
        Rectanglef remaining = jui.allocateRemaining(tmpRect2);
        jui.scissorPush(remaining);
        Font font = jui.fontGetBound(0);
        jui.drawText(textBlock,remaining.minX,remaining.maxY - font.ascent * 18 / font.size,0,18,Color.WHITE);
        jui.scissorPop();

        Container container = jui.popContainer();





        jui.deferredFlush();

        jui.popID();
    }

    TextBlock textBlock = new TextBlock("" +
            "public int currentHoveredID() { return hoveredID; }\n" +
            "public int currentPressedID() { return pressedID; }\n" +
            "public int currentDraggedID() { return draggedID; }\n" +
            "public int currentSelectedID() { return selectedID; }\n" +
            "public int currentFocusedID() { return focusedID; }\n" +
            "public int lastFrameHoveredID() { return lastHoveredID; }\n" +
            "public int lastFramePressedID() { return lastPressedID; }\n" +
            "public int lastFrameDraggedID() { return lastDraggedID; }\n" +
            "public int lastFrameFocusedID() { return lastFocusedID; }\n" +
            "public int navigationBtn() { return navigationBtn; }\n" +
            "public int mouseActiveBtn() { return activeMouseBtn; }\n" +
            "public int mouseLastActiveBtn() { return lastActiveMouseBtn; }\n" +
            "public float hoveredDuration() { return (float) (hoveredDurationNS / 1_000_000_000d); }\n" +
            "public float pressedDuration() { return (float) (pressedDurationNS / 1_000_000_000d); }\n" +
            "public float focusedDuration() { return (float) (focusedDurationNS / 1_000_000_000d); }");



    private void initializeInventories() {
        playerInventory.add(new Item("Long Sword","Meticulously crafted steel sword","#777777"));
        playerInventory.add(new Item("Coin Purse","A leather bag with 3 coins inside","#777777"));
        playerInventory.add(new Item("Oil Painting","A painting of a lighthouse by the coast","#777777"));
        playerInventory.add(new Item("Dice","A pair of red six-sided dice","#777777"));
        shopInventory.add(new Item("Rope","60ft of stron rope","#777777"));
        shopInventory.add(new Item("Rope","60ft of stron rope","#777777"));
        shopInventory.add(new Item("Javelin","Wooden handled throwing weapon","#777777"));
        shopInventory.add(new Item("Coin Purse","A leather bag with 55 coins inside","#777777"));
        shopInventory.add(new Item("Apples","A corded basket filled with green apples","#777777"));
        shopInventory.add(new Item("Jacket","A navy blue wool jacket","#777777"));
    }


    public void free() {
        for (Item item : playerInventory) item.free();
        for (Item item : shopInventory) item.free();
        textBlock.free();
    }


}
