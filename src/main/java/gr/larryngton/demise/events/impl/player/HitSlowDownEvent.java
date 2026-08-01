package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HitSlowDownEvent extends CancellableEvent {
    public double slowDown;
    public boolean sprint;
}
