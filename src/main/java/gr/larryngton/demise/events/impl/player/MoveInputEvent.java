package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MovementInput;

@Getter
@Setter
@AllArgsConstructor
public class MoveInputEvent extends CancellableEvent {
    private float forward;
    private float strafe;
    private boolean jumping;
    private boolean sneaking;

    public boolean moving() {
        return forward != 0 || strafe != 0 || jumping || sneaking;
    }

    public void setMovementInput(MovementInput target) {
        this.forward = target.moveForward;
        this.strafe = target.moveStrafe;
        this.jumping = target.jump;
        this.sneaking = target.sneak;
    }
}