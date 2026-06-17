package io.github.nascentlogic.jgen;

/**
 * F.Dahl, 6/16/2026
 */
public class JgenTime {

    private long startTimeNS;
    private long deltaTimeNS;
    private long lastFrameNS;
    private long frameCount;
    private final FrameRateLimiter frameRateLimiter;
    // fps estimate -------------------------------------------------
    private static final long FPS_UPDATE_INTERVAL = 500_000_000L;
    private long fpsUpdateAccum;
    private long lastFpsUpdateNS;
    private long lastFpsFrameCount;
    private double fpsEstimate;


    JgenTime() {
        frameRateLimiter = new FrameRateLimiter();
        reset();
    }

    void reset() {
        startTimeNS = System.nanoTime() - 1_000L;
        deltaTimeNS = 0L;
        lastFrameNS = startTimeNS;
        frameCount = 0L;
        frameRateLimiter.reset();
        // fps estimate:
        fpsUpdateAccum = 0L;
        lastFpsUpdateNS = 0L;
        lastFpsFrameCount = 0L;
        fpsEstimate = 0.0;
    }

    void tick() {
        long timeNow = System.nanoTime();
        deltaTimeNS = timeNow - lastFrameNS;
        lastFrameNS = timeNow;
        frameCount++;
        // fps estimate:
        fpsUpdateAccum += deltaTimeNS;
        if (fpsUpdateAccum >= FPS_UPDATE_INTERVAL) {
            long framesThisWindow = frameCount - lastFpsFrameCount;
            long exactTimeElapsedNS = timeNow - lastFpsUpdateNS;
            fpsEstimate = framesThisWindow / (exactTimeElapsedNS / 1_000_000_000.0);
            fpsUpdateAccum -= FPS_UPDATE_INTERVAL;
            lastFpsUpdateNS = timeNow;
            lastFpsFrameCount = frameCount;
        }
    }

    /** Caps the frame rate to target fps when vsync is disabled. */
    void sync(int targetFps, boolean vsync) { frameRateLimiter.sync(targetFps, vsync); }
    /** The estimated fps, meassured every 0.5 seconds. */
    public double fpsEstimate() { return fpsEstimate; }
    /** Current frame of the main loop */
    public long frameCount() { return frameCount; }
    /** Main loop runtime in nano seconds */
    public long runTimeNS() { return System.nanoTime() - startTimeNS; }
    /** Time in nano seconds from the previous frame to current frame */
    public long deltaTimeNS() { return deltaTimeNS; }
    /** Main loop runtime in seconds */
    public double runTime() { return (double) runTimeNS() / 1_000_000_000L; }
    /** Time in seconds from the previous frame to current frame */
    public double deltaTime() { return (double) deltaTimeNS / 1_000_000_000L; }


    static final class FrameRateLimiter {
        /*  Robust frame rate limiter for when VSync is disabled.
            This is a cleaned-up and improved version of the classic LWJGL 2 Display.sync()
            hybrid timing method (originally popularized by kappaOne/Riven).
            It uses the industry-standard three-phase approach:
                1. Coarse Thread.sleep()     - saves CPU when plenty of time remains
                2. Thread.yield()            - medium precision
                3. Busy-wait                 - final high precision
            Key improvements over the original:
                - Strong protection against frame bursts after hitches (GC, loading, etc.)
                - Simpler and more maintainable adaptation logic
                - Clean reset behavior when changing FPS limits */
        private static final long INITIAL_YIELD_NS = 1_500_000L;
        private static final long MAX_YIELD_NS     = 8_000_000L;
        private static final long MIN_YIELD_NS     = 200_000L;
        private static final long MAX_CATCHUP_NS   = 12_000_000L;
        private int targetFpsLast;
        private long lastFrameStartTime;
        private long adaptiveYieldDuration;
        private boolean vsyncEnabledLastFrame;

        FrameRateLimiter() { reset(); }

        void reset() {
            targetFpsLast = -1;
            vsyncEnabledLastFrame = false;
        }

        void sync(int targetFps, boolean vsyncEnabled) {
            if (targetFps <= 0)
                throw new IllegalArgumentException("Target FPS must be greater than 0");
            if (vsyncEnabled) {
                vsyncEnabledLastFrame = true;
                return;
            } if (targetFps != targetFpsLast || vsyncEnabledLastFrame) {
                vsyncEnabledLastFrame = false;
                lastFrameStartTime = System.nanoTime();
                adaptiveYieldDuration = INITIAL_YIELD_NS;
                targetFpsLast = targetFps;
                return;
            }
            final long targetNanosPerFrame = 1_000_000_000L / targetFps;
            final long awakeDurationNeeded = adaptiveYieldDuration + 500_000L;
            final long sleepThreshold = targetNanosPerFrame - awakeDurationNeeded;
            long frameOverrun = 0L;

            try {
                while (true) {
                    long frameTimeElapsed = System.nanoTime() - lastFrameStartTime;
                    if (frameTimeElapsed < sleepThreshold) {
                        // Stage 1: Far from target. Save CPU.
                        Thread.sleep(1);
                    } else if (frameTimeElapsed < targetNanosPerFrame) {
                        // Stage 2: Close to target. Give up time slice.
                        Thread.yield();
                    } else { // Stage 3: Target hit or missed. Calculate the error.
                        frameOverrun = frameTimeElapsed - targetNanosPerFrame;
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                long now = System.nanoTime();
                long correction = Math.min(frameOverrun, MAX_CATCHUP_NS);
                lastFrameStartTime = now - correction;
                // Tune the threshold based on the overrun
                if (frameOverrun > adaptiveYieldDuration + 300_000L) {
                    adaptiveYieldDuration = Math.min(adaptiveYieldDuration + 50_000L, MAX_YIELD_NS);
                } else if (frameOverrun < adaptiveYieldDuration - 300_000L) {
                    adaptiveYieldDuration = Math.max(MIN_YIELD_NS, adaptiveYieldDuration - 30_000L);
                }
            }
        }
    }
}
