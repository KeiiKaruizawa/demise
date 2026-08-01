package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public final class SlowDownEvent implements Event {
    private float strafe;
    private float forward;
    private boolean sprinting;
}