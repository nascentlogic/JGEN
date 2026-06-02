package org.example.anim;

import org.joml.Math;
import org.joml.Vector2f;

/**
 * F.Dahl, 5/17/2026
 */
public class BipedLocomotion2 {

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

    public BipedLocomotion2(float bodyRadius) {
        this(bodyRadius, DEFAULT_TORSO_F, DEFAULT_TORSO_ZETA, DEFAULT_TORSO_R,
                DEFAULT_STANCE_WIDTH_FACTOR, DEFAULT_STEP_THRESHOLD_FACTOR, DEFAULT_STEP_DURATION);
    }

    public BipedLocomotion2(float bodyRadius, float torsoF, float torsoZeta, float torsoR,
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

        float speedSq = velocity.lengthSquared();
        if (speedSq < EPSILON_SPEED_SQ) {
            currentState = MovementState.IDLE;
        } else {

            float speed = Math.sqrt(speedSq);
            float movementDot = ((velocity.x / speed) * Math.cos(torsoTracker.currentAngle)) +
                    ((velocity.y / speed) * Math.sin(torsoTracker.currentAngle));
            currentState = (movementDot > DOT_LIMIT) ? MovementState.FORWARD :
                    (movementDot < -DOT_LIMIT) ? MovementState.BACKWARD : MovementState.STRAFE;
        }
        feetTracker.update(bodyPos, velocity, bodyRadius, currentState, torsoTracker.currentAngle, dt);
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
            currentAngle = initialAngle;
            angularVelocity = 0f;
        }

        void update(Vector2f viewDir, float dt) {
            if (viewDir.lengthSquared() < EPSILON_SPEED_SQ) {
                angularVelocity = 0f;
                return;
            }
            float targetAngle = Math.atan2(viewDir.y, viewDir.x);
            float deltaAngle = Math.atan2(Math.sin(targetAngle - currentAngle), Math.cos(targetAngle - currentAngle));
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
            angularVelocity += ((deltaAngle + k3 * head.angularVelocity - k1 * angularVelocity) / k2) * dt;
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

        void reset(float px, float py) {
            worldPos.set(px, py);
            stepStartPos.set(px, py);
            stepTargetPos.set(px, py);
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

            float cos = Math.cos(torsoAngle), sin = Math.sin(torsoAngle);
            float rx = -sin * (bodyRadius * stanceWidthFactor * state.widthMod);
            float ry = cos * (bodyRadius * stanceWidthFactor * state.widthMod);
            float hLx = bodyPos.x - rx, hLy = bodyPos.y - ry;
            float hRx = bodyPos.x + rx, hRy = bodyPos.y + ry;

            if (lFoot.isStepping || rFoot.isStepping) return;

            float lDistSq = lFoot.worldPos.distanceSquared(hLx, hLy);
            float rDistSq = rFoot.worldPos.distanceSquared(hRx, hRy);
            float threshSq = (bodyRadius * stepThresholdFactor) * (bodyRadius * stepThresholdFactor);

            if (lDistSq > threshSq && lDistSq >= rDistSq) {
                triggerStep(lFoot, hLx + (velocity.x * stepDuration * VELOCITY_OVERSHOOT_FACTOR), hLy + (velocity.y * stepDuration * VELOCITY_OVERSHOOT_FACTOR));
            } else if (rDistSq > threshSq) {
                triggerStep(rFoot, hRx + (velocity.x * stepDuration * VELOCITY_OVERSHOOT_FACTOR), hRy + (velocity.y * stepDuration * VELOCITY_OVERSHOOT_FACTOR));
            }
        }

        void triggerStep(Foot foot, float tx, float ty) {
            foot.isStepping = true;
            foot.stepProgress = 0f;
            foot.stepStartPos.set(foot.worldPos);
            foot.stepTargetPos.set(tx, ty);
        }

        void updateMovingFoot(Foot foot, float dt) {
            if (!foot.isStepping) return;
            foot.stepProgress += dt / stepDuration;
            if (foot.stepProgress >= 1.0f) {
                foot.stepProgress = 1.0f;
                foot.isStepping = false;
                foot.worldPos.set(foot.stepTargetPos);
            } else {
                foot.worldPos.x = Math.lerp(foot.stepStartPos.x, foot.stepTargetPos.x, foot.stepProgress);
                foot.worldPos.y = Math.lerp(foot.stepStartPos.y, foot.stepTargetPos.y, foot.stepProgress);
            }
        }

        void reset(Vector2f bodyPos, float facingDir, float bodyRadius) {
            float rx = -Math.sin(facingDir) * (bodyRadius * stanceWidthFactor);
            float ry = Math.cos(facingDir) * (bodyRadius * stanceWidthFactor);
            lFoot.reset(bodyPos.x - rx, bodyPos.y - ry);
            rFoot.reset(bodyPos.x + rx, bodyPos.y + ry);
        }
    }
}
