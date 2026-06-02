package org.example.anim;

import org.joml.Math;
import org.joml.Vector2f;

/**
 * F.Dahl, 5/17/2026
 */
public class BipedLocomotion3 {

    // Constants
    protected static final float DOT_LIMIT = Math.sin(Math.PI_OVER_4_f);
    // Defaults
    protected static final float T_POSE_ANGLE = -Math.PI_OVER_2_f;
    protected static final float DEFAULT_TORSO_F = 3.5f;
    protected static final float DEFAULT_TORSO_ZETA = 1.0f;
    protected static final float DEFAULT_TORSO_R = 0.0f;
    protected static final float DEFAULT_STANCE_WIDTH_FACTOR = 0.8f;
    protected static final float DEFAULT_STEP_THRESHOLD_FACTOR = 1.2f;
    protected static final float DEFAULT_STEP_DURATION = 0.2f;
    // Micro-tuning Configuration Constants
    protected static final float EPSILON_SPEED_SQ = 0.0001f;
    protected static final float EPSILON_LENGTH = 0.001f;
    protected static final float VELOCITY_OVERSHOOT_FACTOR = 1.2f;

    private enum MovementState {
        IDLE(1.0f),
        FORWARD(0.8f),
        BACKWARD(0.8f),
        STRAFE(1.2f);
        final float widthMod;
        MovementState(float widthMod) {
            this.widthMod = widthMod;
        }
    }

    private float bodyRadius;
    private MovementState currentState;
    private HeadTracker headTracker;
    private TorsoTracker torsoTracker;
    private FeetTracker feetTracker;

    public BipedLocomotion3(float bodyRadius) {
        this(bodyRadius, DEFAULT_TORSO_F, DEFAULT_TORSO_ZETA, DEFAULT_TORSO_R,
        DEFAULT_STANCE_WIDTH_FACTOR, DEFAULT_STEP_THRESHOLD_FACTOR, DEFAULT_STEP_DURATION);
    }

    public BipedLocomotion3(float bodyRadius, float torsoF, float torsoZeta, float torsoR,
                            float stanceWidthFactor, float stepThresholdFactor, float stepDuration) {
        this.bodyRadius = bodyRadius;
        currentState = MovementState.IDLE;
        headTracker = new HeadTracker(T_POSE_ANGLE);
        torsoTracker = new TorsoTracker(T_POSE_ANGLE, torsoF, torsoZeta, torsoR);
        feetTracker = new FeetTracker(stanceWidthFactor, stepThresholdFactor, stepDuration);
    }

    public void update(Vector2f bodyPos, Vector2f velocity, Vector2f acceleration, Vector2f viewDir, float dt) {
        headTracker.update(viewDir, dt);
        torsoTracker.update(headTracker, dt);
        currentState = updateMovementState(velocity,torsoTracker.currentAngle);
        feetTracker.update(bodyPos, velocity, bodyRadius, currentState, torsoTracker.currentAngle, dt);
    }

    private MovementState updateMovementState(Vector2f velocity, float torsoAngle) {
        if (velocity.lengthSquared() < EPSILON_SPEED_SQ) return MovementState.IDLE;
        else { Vector2f normalizedVelocity = new Vector2f(velocity).normalize();
            Vector2f torsoForward = new Vector2f(Math.cos(torsoAngle), Math.sin(torsoAngle));
            float movementDot = normalizedVelocity.dot(torsoForward);
            if (movementDot > DOT_LIMIT) return MovementState.FORWARD;
            else if (movementDot < -DOT_LIMIT) return MovementState.BACKWARD;
            else return MovementState.STRAFE;
        }
    }

    public void reset(Vector2f position, float facingDir) {
        currentState = MovementState.IDLE;
        headTracker.reset(facingDir);
        torsoTracker.reset(facingDir);
        feetTracker.reset(position, facingDir, bodyRadius);
    }



    private static final class HeadTracker {
        float currentAngle;
        float angularVelocity;

        HeadTracker(float initialAngle) {
            reset(initialAngle);
        }

        void update(Vector2f viewDir, float dt) {
            if (viewDir.lengthSquared() >= EPSILON_SPEED_SQ) {
                float targetAngle = Math.atan2(viewDir.y, viewDir.x);
                float deltaAngle = Math.atan2(Math.sin(targetAngle - currentAngle), Math.cos(targetAngle - currentAngle));
                angularVelocity = deltaAngle / dt;
                currentAngle = targetAngle;
            } else angularVelocity = 0f;
        }
        void reset(float targetAngle) {
            currentAngle = targetAngle;
            angularVelocity = 0f;
        }
    }

    private static final class TorsoTracker {
        float currentAngle;
        float angularVelocity;
        float k1, k2, k3;

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
        Vector2f worldPos = new Vector2f();
        Vector2f stepStartPos = new Vector2f();
        Vector2f stepTargetPos = new Vector2f();
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

        void update(Vector2f bodyPos, Vector2f velocity, float bodyRadius, MovementState state, float torsoAngle, float dt) {
            updateMovingFoot(lFoot, dt);
            updateMovingFoot(rFoot, dt);
            // Stride lock check: do not initiate new steps if either foot is moving
            if (lFoot.isStepping || rFoot.isStepping) return;
            float totalStanceWidth = bodyRadius * stanceWidthFactor * state.widthMod;
            // Build a directional right-hand lateral vector based on the current torso orientation
            Vector2f lateralRight = new Vector2f(-Math.sin(torsoAngle), Math.cos(torsoAngle)).mul(totalStanceWidth);
            Vector2f homeLeft = new Vector2f(bodyPos).sub(lateralRight);
            Vector2f homeRight = new Vector2f(bodyPos).add(lateralRight);

            float lDistSq = lFoot.worldPos.distanceSquared(homeLeft);
            float rDistSq = rFoot.worldPos.distanceSquared(homeRight);
            float threshold = bodyRadius * stepThresholdFactor;
            float threshSq = threshold * threshold;

            // Compute look-ahead prediction vector for foot positioning based on body momentum
            Vector2f lookAheadPrediction = new Vector2f(velocity).mul(stepDuration * VELOCITY_OVERSHOOT_FACTOR);

            if (lDistSq > threshSq && lDistSq >= rDistSq) {
                Vector2f targetPos = new Vector2f(homeLeft).add(lookAheadPrediction);
                triggerStep(lFoot, targetPos);
            } else if (rDistSq > threshSq) {
                Vector2f targetPos = new Vector2f(homeRight).add(lookAheadPrediction);
                triggerStep(rFoot, targetPos);
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
                } else foot.worldPos.set(foot.stepStartPos).lerp(foot.stepTargetPos, foot.stepProgress);
            }
        }

        void reset(Vector2f bodyPos, float facingDir, float bodyRadius) {
            float totalStanceWidth = bodyRadius * stanceWidthFactor;
            Vector2f lateralRight = new Vector2f(-Math.sin(facingDir), Math.cos(facingDir)).mul(totalStanceWidth);
            Vector2f initialLeftPos = new Vector2f(bodyPos).sub(lateralRight);
            Vector2f initialRightPos = new Vector2f(bodyPos).add(lateralRight);
            lFoot.reset(initialLeftPos);
            rFoot.reset(initialRightPos);
        }
    }
}
