package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MouseMoveEvent implements Event {
    private int deltaX;
    private int deltaY;
    private State state;

    public enum State {
        PRE, UPDATE, AFTER_ANGLE
    }
}
