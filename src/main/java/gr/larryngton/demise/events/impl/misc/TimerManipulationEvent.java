package gr.larryngton.demise.events.impl.misc;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TimerManipulationEvent implements Event {
    private long time;
}
