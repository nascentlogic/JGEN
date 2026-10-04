package org.example;

import io.github.nascentlogic.jgen.gui.JuiCore;
import org.joml.primitives.Rectanglef;

/**
 * F.Dahl, 9/19/2026
 */
public abstract class JuiWindowExample {

    private static final float ANIM_DURATION = 0.25f;

    String name;
    Rectanglef currentArea;
    Rectanglef restoredArea;
    Rectanglef animOrigin;
    Rectanglef animTarget;
    float animProgress;
    boolean signaledToRestore;
    boolean signaledToMaximize;
    boolean animating;

    boolean open;
    boolean resizable;
    int minWidth;
    int minHeight;



    void process(JuiCore jui, float dt) {


        int guiResW = jui.resolutionWidth();
        int guiResH = jui.resolutionHeight();


        boolean maximized = currentArea.lengthX() == guiResW
                && currentArea.lengthY() == guiResH;

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
                restoredArea.set(currentArea);
                animOrigin.set(currentArea);
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
                animOrigin.set(currentArea);
                animTarget.set(restoredArea);
                animProgress = 0.0f;
                animating = true;
            }
        }

        signaledToRestore = false;
        signaledToMaximize = false;

        if (animating) {
            animProgress += dt / ANIM_DURATION;
            animProgress = Math.clamp(animProgress,0.0f,1.0f);
            float t = 1.0f - (float) Math.pow(1.0f - animProgress, 3.0);// Cubic Ease-Out
            currentArea.minX = animOrigin.minX + (animTarget.minX - animOrigin.minX) * t;
            currentArea.minY = animOrigin.minY + (animTarget.minY - animOrigin.minY) * t;
            currentArea.maxX = animOrigin.maxX + (animTarget.maxX - animOrigin.maxX) * t;
            currentArea.maxY = animOrigin.maxY + (animTarget.maxY - animOrigin.maxY) * t;
            if (animProgress >= 1.0f) animating = false;
        } else if (!maximized) {
            restoredArea.set(currentArea);
        }


        render(jui);
    }

    abstract void render(JuiCore jui);
    public String name() { return name; }
    public void maximize() { if (resizable) signaledToMaximize = true; }
    public void restore() { if (resizable) signaledToRestore = true; }
    public boolean isOpen() { return open; }
    public void open() { open = false; }
    public void close() { open = true; }

}
