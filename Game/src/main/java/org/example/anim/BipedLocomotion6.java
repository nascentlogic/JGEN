package org.example.anim;

import org.joml.Math;
import org.joml.Vector2f;

/**
 * Procedural Bipedal Locomotion System
 * Optimized for circular entities in a 2D grid environment.
 * * F.Dahl, 5/17/2026
 */
public class BipedLocomotion6 {

    protected static final float T_POSE_ANGLE = -Math.PI_OVER_2_f;

    // Default Torso Tracking Coefficients (Second-Order Dynamics)
    protected static final float DEFAULT_TORSO_F = 3.5f;
    protected static final float DEFAULT_TORSO_ZETA = 1.0f;
    protected static final float DEFAULT_TORSO_R = 0.0f;

    // Default Locomotion Configuration Parameters
    protected static final float DEFAULT_STANCE_WIDTH_FACTOR = 0.8f;
    protected static final float DEFAULT_STEP_THRESHOLD_FACTOR = 1.2f;
    protected static final float DEFAULT_STEP_DURATION = 0.2f;

    // Locomotion Constants
    protected static final float DOT_LIMIT = 0.7071f;
    protected static final float VELOCITY_OVERSHOOT_FACTOR = 1.2f;
    protected static final float ACCEL_SMOOTH_RATE = 10.0f;
    protected static final float TORSO_LEAN_FACTOR = 0.015f;
    protected static final float TORSO_LEAN_SMOOTH_RATE = 8.0f;
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

    public BipedLocomotion6(float bodyRadius) {
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

    public BipedLocomotion6(
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
        // Exponentially smooth linear acceleration to eliminate single-frame physics spikes
        smoothedAcceleration.lerp(acceleration, dt * ACCEL_SMOOTH_RATE); // todo: what if dt * smootrate > 1 ?
        // Update orientation trackers hierarchically
        headTracker.update(viewDir, dt);
        torsoTracker.update(headTracker, dt);
        // Deduce active animation state relative to the updated torso direction
        currentState = updateMovementState(velocity, torsoTracker.currentAngle);

        // Calculate structural upper-body lagging lean based on directional inertia
        Vector2f targetLean = new Vector2f(smoothedAcceleration).mul(-TORSO_LEAN_FACTOR);
        float maxLean = bodyRadius * 0.35f;
        if (targetLean.lengthSquared() > maxLean * maxLean) {
            targetLean.normalize(maxLean);
        }

        // Todo: what's torsoOffset. anim only?
        torsoOffset.lerp(targetLean, dt * TORSO_LEAN_SMOOTH_RATE); // todo: what if dt * smootrate > 1 ?

        // Execute kinematic foot placement cycles
        feetTracker.update(
                bodyPos,
                velocity,
                smoothedAcceleration,
                bodyRadius,
                currentState,
                torsoTracker.currentAngle,
                dt
        );
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
        if (velocity.lengthSquared() < 0.0001f) return MovementState.IDLE;
        Vector2f normalizedVelocity = new Vector2f(velocity).normalize();
        Vector2f torsoForward = new Vector2f(Math.cos(torsoAngle), Math.sin(torsoAngle));
        float movementDot = normalizedVelocity.dot(torsoForward);
        if (movementDot > DOT_LIMIT) return MovementState.FORWARD;
        if (movementDot < -DOT_LIMIT) return MovementState.BACKWARD;
        return MovementState.STRAFE;
    }

    public static final class HeadTracker {
        public float currentAngle;
        public float angularVelocity;

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

    public static final class TorsoTracker {
        public float currentAngle;
        public float angularVelocity;
        // Second-Order Dynamics Coefficients
        private final float k1;
        private final float k2;
        private final float k3;

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

    public static final class Foot {
        public final Vector2f worldPos = new Vector2f();
        public final Vector2f stepStartPos = new Vector2f();
        public final Vector2f stepTargetPos = new Vector2f();
        public float stepProgress = 0f;
        public boolean isStepping = false;
        void reset(Vector2f pos) {
            worldPos.set(pos);
            stepStartPos.set(pos);
            stepTargetPos.set(pos);
            stepProgress = 0f;
            isStepping = false;
        }

        /**
         * Calculates the virtual z-axis lift arc height based on current step progress
         */
        public float getStepHeight(float maxStepHeight) {
            if (!isStepping) {
                return 0.0f;
            }
            float radians = stepProgress * Math.PI_f;
            return Math.sin(radians) * maxStepHeight;
        }
    }

    public static final class FeetTracker {
        private final float stanceWidthFactor;
        private final float stepThresholdFactor;
        private final float stepDuration;

        public final Foot lFoot = new Foot();
        public final Foot rFoot = new Foot();

        FeetTracker(float stanceWidthFactor, float stepThresholdFactor, float stepDuration) {
            this.stanceWidthFactor = stanceWidthFactor;
            this.stepThresholdFactor = stepThresholdFactor;
            this.stepDuration = stepDuration;
        }

        void update(Vector2f bodyPos, Vector2f velocity, Vector2f smoothedAccel, float bodyRadius, MovementState state, float torsoAngle, float dt) {
            // Update active step interpolations
            updateMovingFoot(lFoot, dt); // todo: one foot is always first. Should depend. Test behaviour.
            updateMovingFoot(rFoot, dt);

            // Calculate lateral home anchors relative to torso facing alignment
            float totalStanceWidth = bodyRadius * stanceWidthFactor * state.widthMod;
            Vector2f lateralRight = new Vector2f(-Math.sin(torsoAngle), Math.cos(torsoAngle)).mul(totalStanceWidth);
            Vector2f homeLeft = new Vector2f(bodyPos).sub(lateralRight);
            Vector2f homeRight = new Vector2f(bodyPos).add(lateralRight);

            // Execute hard constraints to instantly catch feet during explosive impulses
            applyHardConstraint(lFoot, homeLeft, bodyRadius);
            applyHardConstraint(rFoot, homeRight, bodyRadius);

            // Enforce single-step lockout stability
            if (lFoot.isStepping) {
                return;
            }

            if (rFoot.isStepping) {
                return;
            }

            // Evaluate tracking error thresholds
            float lDistSq = lFoot.worldPos.distanceSquared(homeLeft);
            float rDistSq = rFoot.worldPos.distanceSquared(homeRight);
            float threshold = bodyRadius * stepThresholdFactor; // todo: MovementState is not incorporated?
            float threshSq = threshold * threshold;

            // Structural Look-Ahead Prediction Math (Inertial Anticipation)
            float t = stepDuration * VELOCITY_OVERSHOOT_FACTOR;
            Vector2f lookAheadPrediction = new Vector2f(velocity).mul(t).add(new Vector2f(smoothedAccel).mul(0.5f * t * t));

            if (lDistSq > threshSq) {
                if (lDistSq >= rDistSq) {
                    Vector2f target = new Vector2f(homeLeft).add(lookAheadPrediction);
                    triggerStep(lFoot, target);
                    return;
                }
            }

            if (rDistSq > threshSq) {
                Vector2f target = new Vector2f(homeRight).add(lookAheadPrediction);
                triggerStep(rFoot, target);
            }
        }

        private void applyHardConstraint(Foot foot, Vector2f homePos, float bodyRadius) {
            Vector2f toFoot = new Vector2f(foot.worldPos).sub(homePos);
            float dist = toFoot.length(); // todo: squared instead
            float maxLag = bodyRadius * MAX_FOOT_LAG_FACTOR;

            if (dist > maxLag) {
                toFoot.normalize(maxLag);
                foot.worldPos.set(homePos).add(toFoot);
                foot.isStepping = false;
                foot.stepProgress = 0.0f;
            }
        }

        private void triggerStep(Foot foot, Vector2f targetPos) {
            foot.isStepping = true;
            foot.stepProgress = 0f;
            foot.stepStartPos.set(foot.worldPos);
            foot.stepTargetPos.set(targetPos);
        }

        private void updateMovingFoot(Foot foot, float dt) {
            // todo: forgot the z-axis arc again
            if (foot.isStepping) {
                foot.stepProgress += dt / stepDuration;
                if (foot.stepProgress >= 1.0f) {
                    foot.stepProgress = 1.0f;
                    foot.isStepping = false;
                    foot.worldPos.set(foot.stepTargetPos);
                } else {
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