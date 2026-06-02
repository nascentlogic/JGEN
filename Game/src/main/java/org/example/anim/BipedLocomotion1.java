package org.example.anim;

import org.joml.Math;
import org.joml.Vector2f;

/**
 * F.Dahl, 5/17/2026
 */
public class BipedLocomotion1 {


    protected static final float T_POSE_ANGLE = -Math.PI_OVER_2_f;

    protected static final float DEFAULT_TORSO_F = 3.5f;
    protected static final float DEFAULT_TORSO_ZETA = 1.0f;
    protected static final float DEFAULT_TORSO_R = 0.0f;

    protected static final float DEFAULT_STANCE_WIDTH_FACTOR = 0.8f;
    protected static final float DEFAULT_STEP_THRESHOLD_FACTOR = 1.2f;
    protected static final float DEFAULT_STEP_DURATION = 0.2f;

    private enum MovementState { IDLE, FORWARD, STRAFE, BACKWARD }


    private float bodyRadius;
    private MovementState currentState;
    private HeadTracker headTracker;
    private TorsoTracker torsoTracker;
    private FeetTracker feetTracker;


    public BipedLocomotion1(float bodyRadius) {
        this(bodyRadius, DEFAULT_TORSO_F, DEFAULT_TORSO_ZETA, DEFAULT_TORSO_R,
         DEFAULT_STANCE_WIDTH_FACTOR, DEFAULT_STEP_THRESHOLD_FACTOR, DEFAULT_STEP_DURATION);
    }

    public BipedLocomotion1(float bodyRadius, float torsoF, float torsoZeta, float torsoR,
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

        // Determine Movement State via Dot Product using Torso orientation
        float speedSq = velocity.lengthSquared();
        if (speedSq < 0.0001f) {
            currentState = MovementState.IDLE;
        } else {
            float torsoCos = Math.cos(torsoTracker.currentAngle);
            float torsoSin = Math.sin(torsoTracker.currentAngle);

            // Normalize velocity inline to save allocation
            float speed = Math.sqrt(speedSq);
            float velNormX = velocity.x / speed;
            float velNormY = velocity.y / speed;

            // Dot product between normalized travel direction and torso forward vector
            float movementDot = (velNormX * torsoCos) + (velNormY * torsoSin);

            if (movementDot > 0.707f) {
                currentState = MovementState.FORWARD;
            } else if (movementDot < -0.707f) {
                currentState = MovementState.BACKWARD;
            } else {
                currentState = MovementState.STRAFE;
            }
        }

        // Proliferate structural updates down to the feet
        feetTracker.update(bodyPos, velocity, bodyRadius, currentState, torsoTracker.currentAngle, dt);
    }


    public void reset(Vector2f position, float facingDir) {
        this.currentState = MovementState.IDLE;
        this.headTracker.reset(facingDir);
        this.torsoTracker.reset(facingDir);
        this.feetTracker.reset(position, facingDir, this.bodyRadius);
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
            } float targetAngle = Math.atan2(viewDir.y, viewDir.x);
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
        final float k1, k2, k3; // Second-order constants

        TorsoTracker(float initialAngle, float f, float zeta, float r) {
            this.currentAngle = initialAngle;
            this.angularVelocity = 0f;
            this.k1 = zeta / (Math.PI_f * f);
            this.k2 = 1f / ((Math.PI_TIMES_2_f * f) * (Math.PI_TIMES_2_f  * f));
            this.k3 = (r * zeta) / (Math.PI_TIMES_2_f  * f);
        }

        void update(HeadTracker head, float dt) {
            float deltaAngle = head.currentAngle - this.currentAngle;
            deltaAngle = Math.atan2(Math.sin(deltaAngle), Math.cos(deltaAngle));
            // Semi-implicit Euler Integration utilizing head angular velocity feed-forward
            float acceleration = (deltaAngle + k3 * head.angularVelocity - k1 * angularVelocity) / k2;
            this.angularVelocity += acceleration * dt;
            this.currentAngle += this.angularVelocity * dt;
        }

        void reset(float targetAngle) {
            this.currentAngle = targetAngle;
            this.angularVelocity = 0f;
        }
    }

    private static final class Foot {
        final Vector2f worldPos = new Vector2f();
        final Vector2f stepStartPos = new Vector2f();
        final Vector2f stepTargetPos = new Vector2f();
        float stepProgress = 0f;
        boolean isStepping = false;
        void reset(float px, float py) {
            this.worldPos.set(px, py);
            this.stepStartPos.set(px, py);
            this.stepTargetPos.set(px, py);
            this.stepProgress = 0f;
            this.isStepping = false;
        }
    }

    private static final class FeetTracker {
        final float stanceWidthFactor;
        final float stepThresholdFactor;
        final float stepDuration;
        final Foot lFoot = new Foot();
        final Foot rFoot = new Foot();

        FeetTracker(float stanceWidthFactor, float stepThresholdFactor, float stepDuration) {
            this.stanceWidthFactor = stanceWidthFactor;
            this.stepThresholdFactor = stepThresholdFactor;
            this.stepDuration = stepDuration;
        }

        void update(Vector2f bodyPos, Vector2f velocity, float bodyRadius, MovementState state, float torsoAngle, float dt) {
            // 1. Update any airborne foot tracking first
            updateAirborneFoot(lFoot, dt);
            updateAirborneFoot(rFoot, dt);

            // 2. Compute dynamic stance modifier based on state
            float widthMod = 1.0f;
            if (state == MovementState.STRAFE) {
                widthMod = 1.5f;
            } else if (state == MovementState.FORWARD || state == MovementState.BACKWARD) {
                widthMod = 0.8f;
            }

            // 3. Compute current world space home positions for both feet
            float cos = Math.cos(torsoAngle);
            float sin = Math.sin(torsoAngle);
            float actualStanceHalfWidth = bodyRadius * stanceWidthFactor * widthMod;

            float rightX = -sin * actualStanceHalfWidth;
            float rightY = cos * actualStanceHalfWidth;

            float homeLx = bodyPos.x - rightX;
            float homeLy = bodyPos.y - rightY;
            float homeRx = bodyPos.x + rightX;
            float homeRy = bodyPos.y + rightY;

            // 4. If a foot is already stepping, we don't trigger a new step
            if (lFoot.isStepping || rFoot.isStepping) {
                return;
            }

            // 5. Evaluate distance thresholds to trigger a new step
            float lDistSq = lFoot.worldPos.distanceSquared(homeLx, homeLy);
            float rDistSq = rFoot.worldPos.distanceSquared(homeRx, homeRy);
            float thresh = bodyRadius * stepThresholdFactor;
            float threshSq = thresh * thresh;

            // An overshoot factor stretches steps further ahead when sprinting
            float overshootFactor = 1.2f;

            if (lDistSq > threshSq && lDistSq >= rDistSq) {
                float targetX = homeLx + (velocity.x * stepDuration * overshootFactor);
                float targetY = homeLy + (velocity.y * stepDuration * overshootFactor);
                triggerStep(lFoot, targetX, targetY);
            } else if (rDistSq > threshSq) {
                float targetX = homeRx + (velocity.x * stepDuration * overshootFactor);
                float targetY = homeRy + (velocity.y * stepDuration * overshootFactor);
                triggerStep(rFoot, targetX, targetY);
            }
        }

        void triggerStep(Foot foot, float tx, float ty) {
            foot.isStepping = true;
            foot.stepProgress = 0f;
            foot.stepStartPos.set(foot.worldPos);
            foot.stepTargetPos.set(tx, ty);
        }

        void updateAirborneFoot(Foot foot, float dt) {
            if (!foot.isStepping) return;

            foot.stepProgress += dt / stepDuration;
            if (foot.stepProgress >= 1.0f) {
                foot.stepProgress = 1.0f;
                foot.isStepping = false;
                foot.worldPos.set(foot.stepTargetPos);
            } else {
                // Baseline structural step interpolation
                foot.worldPos.x = Math.lerp(foot.stepStartPos.x, foot.stepTargetPos.x, foot.stepProgress);
                foot.worldPos.y = Math.lerp(foot.stepStartPos.y, foot.stepTargetPos.y, foot.stepProgress);

                // Add an external outward arc animation curve to prevent feet tracking through the center
                float swingArc = Math.sin(foot.stepProgress * Math.PI_f) * (foot.stepStartPos.distance(foot.stepTargetPos) * 0.3f);

                // Determine step direction vector to build a perpendicular lift vector
                float dirX = foot.stepTargetPos.x - foot.stepStartPos.x;
                float dirY = foot.stepTargetPos.y - foot.stepStartPos.y;
                float len = Math.sqrt(dirX * dirX + dirY * dirY);

                if (len > 0.001f) {
                    // Left foot arcs outward left (-perpendicular), Right arcs outward right (+perpendicular)
                    float sideSign = (foot == lFoot) ? -1.0f : 1.0f;
                    float perpX = -(dirY / len) * sideSign;
                    float perpY = (dirX / len) * sideSign;

                    foot.worldPos.x += perpX * swingArc;
                    foot.worldPos.y += perpY * swingArc;
                }
            }
        }

        void reset(Vector2f bodyPos, float facingDir, float bodyRadius) {
            float cos = Math.cos(facingDir);
            float sin = Math.sin(facingDir);
            // Compute a perpendicular vector representing the right-side lateral axis
            float actualStanceHalfWidth = bodyRadius * stanceWidthFactor;
            float rightX = -sin * actualStanceHalfWidth;
            float rightY = +cos * actualStanceHalfWidth;
            // Place feet perfectly side-by-side centered below the spawn position
            lFoot.reset(bodyPos.x - rightX, bodyPos.y - rightY);
            rFoot.reset(bodyPos.x + rightX, bodyPos.y + rightY);
        }
    }


}
