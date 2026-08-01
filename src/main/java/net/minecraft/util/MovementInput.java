package net.minecraft.util;

import lombok.Getter;

public class MovementInput {
    public float moveStrafe;
    public float moveForward;
    public boolean jump;
    public boolean sneak;

    public MovementInput() {
    }

    public MovementInput(float moveForward, float moveStrafe, boolean jump, boolean sneak) {
        this.moveForward = moveForward;
        this.moveStrafe = moveStrafe;
        this.jump = jump;
        this.sneak = sneak;
    }

    public MovementInput(MovementInput initial) {
        this.moveForward = initial.moveForward;
        this.moveStrafe = initial.moveStrafe;
        this.jump = initial.jump;
        this.sneak = initial.sneak;
    }

    public MovementInput copy() {
        return new MovementInput(this);
    }

    public MovementInput setForward(float forward) {
        this.moveForward = forward;
        return this;
    }

    public MovementInput setStrafe(float strafe) {
        this.moveStrafe = strafe;
        return this;
    }

    public MovementInput setJump(boolean jump) {
        this.jump = jump;
        return this;
    }

    public MovementInput setSneak(boolean sneak) {
        this.sneak = sneak;
        return this;
    }

    public void updatePlayerMoveState() {
    }

    public enum Action {
        NONE(new MovementInput()),

        FORWARD(new MovementInput().setForward(1)),
        FORWARD_JUMP(new MovementInput().setForward(1).setJump(true)),
        FORWARD_LEFT(new MovementInput().setForward(1).setStrafe(1)),
        FORWARD_RIGHT(new MovementInput().setForward(1).setStrafe(-1)),
        FORWARD_JUMP_RIGHT(new MovementInput().setForward(1).setJump(true).setStrafe(-1)),
        FORWARD_JUMP_LEFT(new MovementInput().setForward(1).setJump(true).setStrafe(1)),

        BACKWARD(new MovementInput().setForward(-1)),
        BACKWARD_JUMP(new MovementInput().setForward(-1).setJump(true)),
        BACKWARD_LEFT(new MovementInput().setForward(-1).setStrafe(1)),
        BACKWARD_JUMP_LEFT(new MovementInput().setForward(-1).setJump(true).setStrafe(1)),
        BACKWARD_RIGHT(new MovementInput().setForward(-1).setStrafe(-1)),
        BACKWARD_JUMP_RIGHT(new MovementInput().setForward(-1).setJump(true).setStrafe(-1)),

        LEFT(new MovementInput().setStrafe(1)),
        RIGHT(new MovementInput().setStrafe(-1)),

        JUMP(new MovementInput().setJump(true)),
        JUMP_LEFT(new MovementInput().setJump(true).setStrafe(1)),
        JUMP_RIGHT(new MovementInput().setJump(true).setStrafe(-1));

        @Getter
        private final MovementInput input;

        Action(MovementInput associatedInput) {
            this.input = associatedInput;
        }
    }
}
