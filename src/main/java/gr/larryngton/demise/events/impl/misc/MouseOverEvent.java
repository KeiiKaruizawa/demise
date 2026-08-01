package gr.larryngton.demise.events.impl.misc;

import gr.larryngton.demise.events.impl.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MovingObjectPosition;

@Getter
@Setter
public class MouseOverEvent implements Event {
    public MouseOverEvent(double range, float expand) {
        this.range = range;
        this.expand = expand;
    }

    private double range;
    private float expand;
    private MovingObjectPosition movingObjectPosition;
}
