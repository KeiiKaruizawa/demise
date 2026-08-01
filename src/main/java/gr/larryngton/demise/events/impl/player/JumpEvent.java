package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class JumpEvent extends CancellableEvent {
    private float motionY;
    private float yaw;
    private float jumpoff;
    private boolean sprint;
}
