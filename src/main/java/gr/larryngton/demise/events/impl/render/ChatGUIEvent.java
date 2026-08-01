package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChatGUIEvent implements Event {
    public int mouseX, mouseY;
}
