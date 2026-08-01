package gr.larryngton.demise.events.impl.player;


import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public final class SneakSlowDownEvent implements Event {
    private double strafe;
    private double forward;
}
