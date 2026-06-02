package io.github.nascentlogic.jgen;

/**
 * F.Dahl, 5/11/2026
 *
 * Robust frame rate limiter for when VSync is disabled.
 *
 * This is a cleaned-up and improved version of the classic LWJGL 2 Display.sync()
 * hybrid timing method (originally popularized by kappaOne/Riven).
 *
 * It uses the industry-standard three-phase approach:
 * 1. Coarse Thread.sleep()     - saves CPU when plenty of time remains
 * 2. Thread.yield()            - medium precision
 * 3. Busy-wait                 - final high precision
 *
 * Key improvements over the original:
 * - Strong protection against frame bursts after hitches (GC, loading, etc.)
 * - Simpler and more maintainable adaptation logic
 * - Clean reset behavior when changing FPS limits
 */
class FrameTimeLimiter {


    private int targetFpsLast = -1;
    private long lastFrameStartTime;
    private long adaptiveYieldDuration;
    private boolean vsyncEnabledLastFrame = false;

    private static final long INITIAL_YIELD_NS = 1_500_000L;
    private static final long MAX_YIELD_NS     = 8_000_000L;
    private static final long MIN_YIELD_NS     = 200_000L;
    private static final long MAX_CATCHUP_NS   = 12_000_000L;

    public FrameTimeLimiter() { /* */ }

    public void sync(int targetFps, boolean vsyncEnabled) {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("Target FPS must be greater than 0. Received: " + targetFps);
        }

        if (vsyncEnabled) {
            vsyncEnabledLastFrame = true;
            return;
        }

        if (targetFps != targetFpsLast || vsyncEnabledLastFrame) {
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
                } else {
                    // Stage 3: Target hit or missed. Calculate the error.
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
                adaptiveYieldDuration = Math.min(adaptiveYieldDuration + 100_000L, MAX_YIELD_NS);
            } else if (frameOverrun < adaptiveYieldDuration - 300_000L) {
                adaptiveYieldDuration = Math.max(MIN_YIELD_NS, adaptiveYieldDuration - 20_000L);
            }
        }
    }

}
