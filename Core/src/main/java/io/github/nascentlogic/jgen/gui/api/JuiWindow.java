package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gui.JuiCore;
import org.joml.primitives.Rectanglef;

/**
 * F.Dahl, 9/19/2026
 */
public abstract class JuiWindow {


    String name;
    Rectanglef currentArea;
    Rectanglef restoredArea;
    Rectanglef animOrigin;
    Rectanglef animTarget;
    float animProgress;
    boolean signaledToRestore;
    boolean signaledToMaximize;
    boolean signaledToClose;
    boolean animating;
    private static final float ANIM_DURATION = 0.2f;

    boolean closed;
    boolean resizable;
    int minWidth;
    int minHeight;
    int layer;


    
    

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

    
    
    void maximize() {
        if (resizable) signaledToMaximize = true;
    }
    
    void restore() {
        if (resizable) signaledToRestore = true;
    }

    void open() {
        closed = false;
    }

    void close() {
        closed = true;
    }

}
