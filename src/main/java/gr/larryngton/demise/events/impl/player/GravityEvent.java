package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GravityEvent implements Event {
    private double gravityDecrement;
    private double gravityMulti;
}