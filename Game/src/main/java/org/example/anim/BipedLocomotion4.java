package org.example.anim;

import org.joml.Math;
import org.joml.Vector2f;

/**
 * F.Dahl, 5/17/2026
 */
public class BipedLocomotion4 {

    protected static final float T_POSE_ANGLE = -Math.PI_OVER_2_f;

    protected static final float DEFAULT_TORSO_F = 3.5f;
    protected static final float DEFAULT_TORSO_ZETA = 1.0f;
    protected static final float DEFAULT_TORSO_R = 0.0f;

    protected static final float DEFAULT_STANCE_WIDTH_FACTOR = 0.8f;
    protected static final float DEFAULT_STEP_THRESHOLD_FACTOR = 1.2f;
    protected static final float DEFAULT_STEP_DURATION = 0.2f;

    protected static final float DOT_LIMIT = 0.7071f;
    protected static final float VELOCITY_OVERSHOOT_FACTOR = 1.2f;
    protected static final float ACCEL_SMOOTH_RATE = 10.0f;
    protected static final float TORSO_LEAN_FACTOR = 0.015f;
    protected static final float TORSO_LEAN_SMOOTH_RATE = 8.0f;

    // Tuning constant representing the maximum allowed structural leg lag stretch
    protected static final float MAX_FOOT_LAG_FACTOR = 2.0f;

    private enum MovementState {
        IDLE(1.0f),
        FORWARD(0.8f),
        STRAFE(1.2f),
        BACKWARD(0.8f);

        final float widthMod;

        MovementState(float widthMod) {
            this.widthMod = widthMod;
        }
    }

    public final Vector2f torsoOffset = new Vector2f();
    private final Vector2f smoothedAcceleration = new Vector2f();
    private float bodyRadius;
    private MovementState currentState;
    private HeadTracker headTracker;
    private TorsoTracker torsoTracker;
    private FeetTracker feetTracker;

    public BipedLocomotion4(float bodyRadius) {
        this(
                bodyRadius,
                DEFAULT_TORSO_F,
                DEFAULT_TORSO_ZETA,
                DEFAULT_TORSO_R,
                DEFAULT_STANCE_WIDTH_FACTOR,
                DEFAULT_STEP_THRESHOLD_FACTOR,
                DEFAULT_STEP_DURATION
        );
    }

    public BipedLocomotion4(
            float bodyRadius,
            float torsoF,
            float torsoZeta,
            float torsoR,
            float stanceWidthFactor,
            float stepThresholdFactor,
            float stepDuration
    ) {
        this.bodyRadius = bodyRadius;
        this.currentState = MovementState.IDLE;
        this.headTracker = new HeadTracker(T_POSE_ANGLE);
        this.torsoTracker = new TorsoTracker(T_POSE_ANGLE, torsoF, torsoZeta, torsoR);
        this.feetTracker = new FeetTracker(stanceWidthFactor, stepThresholdFactor, stepDuration);
    }

    public void update(Vector2f bodyPos, Vector2f velocity, Vector2f acceleration, Vector2f viewDir, float dt) {
        smoothedAcceleration.lerp(acceleration, dt * ACCEL_SMOOTH_RATE);

        headTracker.update(viewDir, dt);
        torsoTracker.update(headTracker, dt);

        currentState = updateMovementState(velocity, torsoTracker.currentAngle);

        Vector2f targetLean = new Vector2f(smoothedAcceleration).mul(-TORSO_LEAN_FACTOR);
        float maxLean = bodyRadius * 0.35f;
        if (targetLean.lengthSquared() > maxLean * maxLean) {
            targetLean.normalize(maxLean);
        }
        torsoOffset.lerp(targetLean, dt * TORSO_LEAN_SMOOTH_RATE);

        feetTracker.update(bodyPos, velocity, smoothedAcceleration, bodyRadius, currentState, torsoTracker.currentAngle, dt);
    }

    public void reset(Vector2f position, float facingDir) {
        currentState = MovementState.IDLE;
        torsoOffset.zero();
        smoothedAcceleration.zero();
        headTracker.reset(facingDir);
        torsoTracker.reset(facingDir);
        feetTracker.reset(position, facingDir, bodyRadius);
    }

    private MovementState updateMovementState(Vector2f velocity, float torsoAngle) {
        if (velocity.lengthSquared() < 0.0001f) {
            return MovementState.IDLE;
        }

        Vector2f normalizedVelocity = new Vector2f(velocity).normalize();
        Vector2f torsoForward = new Vector2f(Math.cos(torsoAngle), Math.sin(torsoAngle));
        float movementDot = normalizedVelocity.dot(torsoForward);

        if (movementDot > DOT_LIMIT) {
            return MovementState.FORWARD;
        }
        if (movementDot < -DOT_LIMIT) {
            return MovementState.BACKWARD;
        }
        return MovementState.STRAFE;
    }

    private static final class HeadTracker {
        float currentAngle;
        float angularVelocity;

        HeadTracker(float initialAngle) {
            currentAngle = initialAngle;
            angularVelocity = 0f;
        }

        void update(Vector2f viewDir, float dt) {
            if (viewDir.lengthSquared() < 0.0001f) {
                angularVelocity = 0f;
                return;
            }

            float targetAngle = Math.atan2(viewDir.y, viewDir.x);
            float deltaAngle = targetAngle - currentAngle;
            deltaAngle = Math.atan2(Math.sin(deltaAngle), Math.cos(deltaAngle));
            angularVelocity = deltaAngle / dt;
            currentAngle = targetAngle;
        }

        void reset(float targetAngle) {
            currentAngle = targetAngle;
            angularVelocity = 0f;
        }
    }

    private static final class TorsoTracker {
        float currentAngle;
        float angularVelocity;
        float k1;
        float k2;
        float k3;

        TorsoTracker(float initialAngle, float f, float zeta, float r) {
            currentAngle = initialAngle;
            angularVelocity = 0f;
            k1 = zeta / (Math.PI_f * f);
            k2 = 1f / ((Math.PI_TIMES_2_f * f) * (Math.PI_TIMES_2_f * f));
            k3 = (r * zeta) / (Math.PI_TIMES_2_f * f);
        }

        void update(HeadTracker head, float dt) {
            float deltaAngle = Math.atan2(Math.sin(head.currentAngle - currentAngle), Math.cos(head.currentAngle - currentAngle));
            float acceleration = (deltaAngle + k3 * head.angularVelocity - k1 * angularVelocity) / k2;
            angularVelocity += acceleration * dt;
            currentAngle += angularVelocity * dt;
        }

        void reset(float targetAngle) {
            currentAngle = targetAngle;
            angularVelocity = 0f;
        }
    }

    private static final class Foot {
        final Vector2f worldPos = new Vector2f();
        final Vector2f stepStartPos = new Vector2f();
        final Vector2f stepTargetPos = new Vector2f();
        float stepProgress = 0f;
        boolean isStepping = false;

        void reset(Vector2f pos) {
            worldPos.set(pos);
            stepStartPos.set(pos);
            stepTargetPos.set(pos);
            stepProgress = 0f;
            isStepping = false;
        }
    }

    private static final class FeetTracker {
        float stanceWidthFactor;
        float stepThresholdFactor;
        float stepDuration;
        Foot lFoot = new Foot();
        Foot rFoot = new Foot();

        FeetTracker(float stanceWidthFactor, float stepThresholdFactor, float stepDuration) {
            this.stanceWidthFactor = stanceWidthFactor;
            this.stepThresholdFactor = stepThresholdFactor;
            this.stepDuration = stepDuration;
        }

        void update(Vector2f bodyPos, Vector2f velocity, Vector2f smoothedAccel, float bodyRadius, MovementState state, float torsoAngle, float dt) {
            updateMovingFoot(lFoot, dt);
            updateMovingFoot(rFoot, dt);

            float totalStanceWidth = bodyRadius * stanceWidthFactor * state.widthMod;
            Vector2f lateralRight = new Vector2f(-Math.sin(torsoAngle), Math.cos(torsoAngle)).mul(totalStanceWidth);
            Vector2f homeLeft = new Vector2f(bodyPos).sub(lateralRight);
            Vector2f homeRight = new Vector2f(bodyPos).add(lateralRight);

            // Hard skeletal boundaries: cheat and translate feet smoothly if they overshoot the lag limit
            applyHardConstraint(lFoot, homeLeft, bodyRadius);
            applyHardConstraint(rFoot, homeRight, bodyRadius);

            if (lFoot.isStepping || rFoot.isStepping) {
                return;
            }

            float lDistSq = lFoot.worldPos.distanceSquared(homeLeft);
            float rDistSq = rFoot.worldPos.distanceSquared(homeRight);
            float threshold = bodyRadius * stepThresholdFactor;
            float threshSq = threshold * threshold;

            float t = stepDuration * VELOCITY_OVERSHOOT_FACTOR;
            Vector2f lookAheadPrediction = new Vector2f(velocity).mul(t).add(new Vector2f(smoothedAccel).mul(0.5f * t * t));

            if (lDistSq > threshSq && lDistSq >= rDistSq) {
                triggerStep(lFoot, new Vector2f(homeLeft).add(lookAheadPrediction));
            } else if (rDistSq > threshSq) {
                triggerStep(rFoot, new Vector2f(homeRight).add(lookAheadPrediction));
            }
        }

        private void applyHardConstraint(Foot foot, Vector2f homePos, float bodyRadius) {
            Vector2f toFoot = new Vector2f(foot.worldPos).sub(homePos);
            float dist = toFoot.length();
            float maxLag = bodyRadius * MAX_FOOT_LAG_FACTOR;

            if (dist > maxLag) {
                toFoot.normalize(maxLag);
                foot.worldPos.set(homePos).add(toFoot);
                foot.isStepping = false;
                foot.stepProgress = 0.0f;
            }
        }

        void triggerStep(Foot foot, Vector2f targetPos) {
            foot.isStepping = true;
            foot.stepProgress = 0f;
            foot.stepStartPos.set(foot.worldPos);
            foot.stepTargetPos.set(targetPos);
        }

        void updateMovingFoot(Foot foot, float dt) {
            if (foot.isStepping) {
                foot.stepProgress += dt / stepDuration;
                if (foot.stepProgress >= 1.0f) {
                    foot.stepProgress = 1.0f;
                    foot.isStepping = false;
                    foot.worldPos.set(foot.stepTargetPos);
                } else {
                    // // Todo: arc in the z-axis?
                    foot.worldPos.set(foot.stepStartPos).lerp(foot.stepTargetPos, foot.stepProgress);
                }
            }
        }

        void reset(Vector2f bodyPos, float facingDir, float bodyRadius) {
            float totalStanceWidth = bodyRadius * stanceWidthFactor * MovementState.IDLE.widthMod;
            Vector2f lateralRight = new Vector2f(-Math.sin(facingDir), Math.cos(facingDir)).mul(totalStanceWidth);
            lFoot.reset(new Vector2f(bodyPos).sub(lateralRight));
            rFoot.reset(new Vector2f(bodyPos).add(lateralRight));
        }
    }
}