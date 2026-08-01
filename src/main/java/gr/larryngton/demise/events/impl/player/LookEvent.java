package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LookEvent implements Event {
    public float[] rotation;
}
