package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CameraPositionEvent implements Event {
    double x;
    double y;
    double z;
}