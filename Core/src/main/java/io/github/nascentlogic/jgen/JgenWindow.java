package io.github.nascentlogic.jgen;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * F.Dahl, 5/14/2026
 */
public class JgenWindow {


    private long windowHandle = 0L;
    private long monitorHandle = 0L;


    private boolean vsyncEnabled;


    public boolean isWindowed() { return glfwGetWindowMonitor(windowHandle) == 0L; }
    public boolean isFullscreen() { return glfwGetWindowMonitor(windowHandle) != 0L; }
    public boolean isMinimized() { return glfwGetWindowAttrib(windowHandle,GLFW_ICONIFIED) == GLFW_TRUE; }
    public boolean isMaximized() { return glfwGetWindowAttrib(windowHandle,GLFW_MAXIMIZED) == GLFW_TRUE; }
    public boolean isInputFocused() { return glfwGetWindowAttrib(windowHandle,GLFW_FOCUSED) == GLFW_TRUE; }
    public boolean isVisible() { return glfwGetWindowAttrib(windowHandle,GLFW_VISIBLE) == GLFW_TRUE; }
    public boolean isResizable() { return glfwGetWindowAttrib(windowHandle,GLFW_RESIZABLE) == GLFW_TRUE; }
    public boolean isDecorated() { return glfwGetWindowAttrib(windowHandle,GLFW_DECORATED) == GLFW_TRUE; }
    public boolean isAutoIconified() { return glfwGetWindowAttrib(windowHandle,GLFW_AUTO_ICONIFY) == GLFW_TRUE; }
    public boolean isHovered() { return glfwGetWindowAttrib(windowHandle,GLFW_HOVERED) == GLFW_TRUE; }

    public void toggleVsync(boolean enable) {
        glfwSwapBuffers(enable ? 1 : 0);
        vsyncEnabled = enable;
    }






    /**
     * @return the monitor recognized as the primary monitor by the OS
     * or null if no monitor was found or an error occured.
     */
    public Monitor primaryMonitor() {
        return monitorOrNull(glfwGetPrimaryMonitor());
    }

    /**
     * @return the monitor the window is currently fullscreen on,
     * or null if in widowed mode
     */
    public Monitor currentMonitor() {
        return monitorOrNull(glfwGetWindowMonitor(windowHandle));
    }

    /**
     * @param persistentID semi-unique identifier for a monitor
     * @return The first located monitor of connected monitors with the same persisten ID,
     * or null if no such monitor is currently connected.
     */
    public Monitor locateMonitor(String persistentID) {
        if (persistentID == null || persistentID.isBlank()) return null;
        List<Monitor> connected = connectedMonitors();
        for (Monitor monitor : connected) {
            if (persistentID.equals(monitor.persistentID))
                return monitor;
        } return null;
    }

    /**
     * @return A list of all currently connected monitors.
     */
    public List<Monitor> connectedMonitors() {
        PointerBuffer pb = glfwGetMonitors();
        if (pb == null || pb.limit() == 0) return Collections.emptyList();
        List<Monitor> list = new ArrayList<>(pb.limit());
        for (int i = 0; i < pb.limit(); i++) {
            if (pb.get(i) == 0L) continue;
            try { list.add(new Monitor(pb.get(i)));
            } catch (Exception e) { Logger.warn(e);}
        } return list;
    }


    public void windowed(int width, int height) {
        windowed(autoChooseMonitor(),width,height);
    }

    public void windowed(Monitor monitor, int width, int height) {
        if (monitor == null) {
            Logger.warn("Cannot switch to windowed mode: Monitor argument is NULL");
            return;
        }
        int displayW = monitor.videoMode.width();
        int displayH = monitor.videoMode.height();
        Logger.info("Attempting to set Window to windowed mode on monitor: \"{}\"", monitor.name);
        Logger.info("Monitor display resolution:  ({}x{})", displayW, displayH);
        Logger.info("Desired Window resolution:   ({}x{})", width, height);
        if (!monitor.isConnected()) {
            // If for some reason the monitor has been disconnected in the time between
            // the Monitor Object creation and the actual current state.
            Logger.warn("Unable to use monitor: \"{}\" (Disconnected)", monitor.name);
            return;
        }

        // Clear tracking flags as intent is windowed mode
        // Guaranteed after setting the monitor to 0L
        monitorHandle = 0L;



        // =========================================================================
        // Perfect match -> True Windowed Borderless Fullscreen (Early return)
        // =========================================================================

        if (width == displayW && height == displayH) {
            // Drop out of exclusive fullscreen (or shift monitors)
            // GLFW automatically clears borders internally if the size matches the monitor perfectly.
            glfwSetWindowMonitor(
                    windowHandle, 0L,
                    monitor.xPos, monitor.yPos,
                    width, height,
                    GLFW_DONT_CARE
            );
            // Explicit Enforcer: Sets our tracking property state in stone.
            // (Safe and redundant if GLFW already cleared it; essential if shifting from a small window).
            glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
            // Re-assert context settings and show
            glfwSwapInterval(vsyncEnabled ? 1 : 0);
            glfwShowWindow(windowHandle);
            glfwFocusWindow(windowHandle);
            Logger.info("Successfully shifted to Borderless Fullscreen Windowed Mode on Monitor: \"{}\".", monitor.name);
            return;
        }

        // =========================================================================
        // Measurements of window frame
        // =========================================================================

        // CALL 1 (Safe Safeguard): If coming from a Windowed Borderless state,
        // this forces borders ON instantly so the OS factors them into the upcoming layout.
        // (Note: If coming from Fullscreen, the OS safely ignores this line).
        glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_TRUE);
        // Drop out of fullscreen or shift our window layout baseline
        glfwSetWindowMonitor(
                windowHandle, 0L,
                monitor.xPos, monitor.yPos,
                width, height,
                GLFW_DONT_CARE
        );
        // CALL 2 (The Enforcer): If we came from Fullscreen, Call 1 was ignored.
        // This call enforces that the decorations are actively built into our fresh windowed context.
        glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_TRUE);

        // Measure the window frame extents
        int frameL, frameT, frameR, frameB;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer l = stack.mallocInt(1);
            IntBuffer t = stack.mallocInt(1);
            IntBuffer r = stack.mallocInt(1);
            IntBuffer b = stack.mallocInt(1);
            glfwGetWindowFrameSize(windowHandle, l, t, r, b);
            frameL = l.get(0);
            frameT = t.get(0);
            frameR = r.get(0);
            frameB = b.get(0);
        }

        int targetX, targetY;
        // =========================================================================
        // The entire decorated window fits inside the monitor display
        // =========================================================================
        if ((width + frameL + frameR) < displayW && (height + frameT + frameB) < displayH) {
            // Calculate standard centered position for the content area
            targetX = monitor.xPos + (displayW - width) / 2;
            targetY = monitor.yPos + (displayH - height) / 2;
            // Corrected Balance Offset: Offsets the coordinate grid to center
            // the heavy title-bar frame layout perfectly on the screen space.
            targetY += frameB - (frameT + frameB) / 2;
            Logger.info("Positioning decorated window (Centered).");
        }
        // =========================================================================
        // Window is too large (Overflow condition)
        // =========================================================================
        else { // Place the window content area exactly at the monitor's top-left origin.
            targetX = monitor.xPos;
            targetY = monitor.yPos;
            // Push the content area DOWN by the exact height of the title bar
            // to guarantee the title bar is completely visible at the top edge of the monitor.
            targetY += frameT;
            Logger.info("Positioning decorated window (Overflow Top-Left).");
        }
        glfwSetWindowPos(windowHandle, targetX, targetY); // final adjust
        // reinfoce vsync as monitor shift reset the driver state
        glfwSwapInterval(vsyncEnabled ? 1 : 0);
        glfwShowWindow(windowHandle);
        glfwFocusWindow(windowHandle);
        Logger.info("Successfully shifted to Decorated " +
                "Windowed Mode on Monitor: \"{}\".",monitor.name);
    }

    public void fullscreen() {
        fullscreen(autoChooseMonitor());
    }

    public void fullscreen(Monitor monitor) {
        if (monitor == null) {
            Logger.warn("Cannot switch to fullscreen mode: Monitor argument is NULL");
            return;
        }
        int displayW = monitor.videoMode.width();
        int displayH = monitor.videoMode.height();
        Logger.info("Attempting to set Window to Fullscreen mode on monitor: \"{}\"", monitor.name);
        Logger.info("Monitor display resolution:  ({}x{})", displayW, displayH);
        if (!monitor.isConnected()) {
            // If for some reason the monitor has been disconnected in the time between
            // the Monitor Object creation and the actual current state.
            Logger.warn("Unable to use monitor: \"{}\" (Disconnected)", monitor.name);
            return;
        }

        // Force visibility to ensure the OS window manager assigns a
        // valid presentation pipeline context before we lock onto the hardware.
        glfwShowWindow(windowHandle);
        glfwSetWindowMonitor(
                windowHandle,
                monitor.handle,
                0, 0,
                monitor.videoMode.width(),
                monitor.videoMode.height(),
                monitor.videoMode.refreshRate()
        );
        glfwSwapInterval(vsyncEnabled ? 1 : 0); // reinfoce vsync as monitor shift reset the driver state
        glfwFocusWindow(windowHandle); // just in case. Window hint should apply.
        // Capture the absolute current operational monitor assignment from GLFW
        monitorHandle = glfwGetWindowMonitor(windowHandle);
        if (monitorHandle != 0L) {
            Logger.info("Successfully bound exclusive Fullscreen mode to monitor: \"{}\"", monitor.name);
        } else { // This fallback should technically never happen given your input rules,
            // but acts as an excellent fatal driver warning indicator.
            Logger.error("CRITICAL: Operating System graphics driver failed to instantiate exclusive Fullscreen context.");
        }
    }






    private GLFWMonitorCallback monitorCallback() {
        return new GLFWMonitorCallback() {
            public void invoke(long monitor, int event) {
                if (monitor != 0L) { // just in case
                    if (event == GLFW_CONNECTED) {

                    } else if (event == GLFW_DISCONNECTED) {
                        if (monitor == monitorHandle) {
                            monitorHandle = 0L;
                        }
                    }
                }
            }
        };
    }


    /**
     * Create a temp wrapper object for monitor.
     * @param monitor glfw monitor handle
     * @return Monitor or NULL if no Display is accosiated with the handle.
     */
    private Monitor monitorOrNull(long monitor) {
        if (monitor == 0L) return null;
        try { return new Monitor(monitor);
        } catch (Exception e) {
            Logger.warn(e);
        } return null;
    }

    /**
     * Calculates which physical monitor currently contains the majority of the
     * window's content area.
     * <p>
     * If the window is currently in exclusive fullscreen mode, this returns that
     * fullscreen monitor immediately. If the window is in windowed mode, it calculates
     * the rectangular bounding box intersection area against all currently connected monitors.
     * If the window is completely off-screen, dragged into empty virtual space, or if all
     * monitors are disconnected, it falls back to the primary monitor.
     * @return The {@link Monitor} currently encompassing the window, or {@code null}
     * only if absolutely no monitors are available or retrievable by the system.
     */
    private Monitor autoChooseMonitor() {
        // Fullscreen short-circuit
        Monitor fullscreen = currentMonitor();
        if (fullscreen != null) return fullscreen;
        // ATP. we are in windowed mode
        // update state just in case
        this.monitorHandle = 0L;
        // Calculate bounding box overlaps
        Monitor bestMonitor = null;
        int maxOverlapArea = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer wx = stack.mallocInt(1);
            IntBuffer wy = stack.mallocInt(1);
            IntBuffer ww = stack.mallocInt(1);
            IntBuffer wh = stack.mallocInt(1);
            // Fetch the absolute virtual coordinates and size of our window content area
            glfwGetWindowPos(windowHandle, wx, wy);
            glfwGetWindowSize(windowHandle, ww, wh);
            int winL = wx.get(0);
            int winT = wy.get(0);
            int winR = winL + ww.get(0);
            int winB = winT + wh.get(0);
            // Loop through all connected screens to test overlapping area rectangles
            for (Monitor m : connectedMonitors()) {
                int monL = m.xPos;
                int monT = m.yPos;
                int monR = monL + m.videoMode.width();
                int monB = monT + m.videoMode.height();
                // Calculate overlapping intersection rectangle bounds
                int intersectL = Math.max(winL, monL);
                int intersectT = Math.max(winT, monT);
                int intersectR = Math.min(winR, monR);
                int intersectB = Math.min(winB, monB);
                // If the rectangles intersect, compute the cross-sectional pixel area
                if (intersectL < intersectR && intersectT < intersectB) {
                    int overlapArea = (intersectR - intersectL) * (intersectB - intersectT);
                    // The monitor displaying the largest section of the window wins
                    if (overlapArea > maxOverlapArea) {
                        maxOverlapArea = overlapArea;
                        bestMonitor = m;
                    }
                }
            }
        }
        // Fallback: If no monitor overlaps (or window is completely off-screen),
        // default to the primary desktop display monitor.
        return (bestMonitor != null) ? bestMonitor : primaryMonitor();
    }

    /** Wrapper class for a GLFW monitor */
    public static final class Monitor {
        /** GLFW monitor handle */
        public final long handle;
        /** Human readable name */
        public final String name;
        /** String representation of the monitor. Incorporates name and physical size.
         * GLFW do not provide unique identifiers for monitors, e.g. a serial number.*/
        public final String persistentID;
        /** video mode of monitor (monitor vidMode at the time of this Object creation).
         * Therefore it might not reflect the monitors current video mode.
         * The resolution of a video mode is specified in screen coordinates. */
        public final GLFWVidMode videoMode;
        public final int xPos;
        public final int yPos;
        Monitor(long handle) throws Exception {
            if (handle == 0L) throw new IllegalArgumentException("monitor == 0L");
            videoMode = glfwGetVideoMode(handle);
            if (videoMode == null) {
                throw new Exception("Monitor vidMode == null");
            } String monitorName = glfwGetMonitorName(handle);
            name = monitorName == null ? "Unnamed Monitor" : monitorName;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer b0 = stack.callocInt(1);
                IntBuffer b1 = stack.callocInt(1);
                glfwGetMonitorPhysicalSize(handle, b0, b1);
                persistentID = String.format("%s|%dx%dmm", name, b0.get(0),b1.get(0));
                glfwGetMonitorPos(handle,b0,b1);
                xPos = b0.get(0);
                yPos = b1.get(0);
            } this.handle = handle;
        } public boolean isConnected() {
            PointerBuffer pb = glfwGetMonitors();
            if (pb == null || pb.limit() == 0) return false;
            for (int i = 0; i < pb.limit(); i++) {
                if (pb.get(i) == handle) return true;
            } return false;
        }
    }

}
