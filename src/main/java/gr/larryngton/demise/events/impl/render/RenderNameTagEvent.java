package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
public class RenderNameTagEvent extends CancellableEvent {
    final Entity entity;

    public RenderNameTagEvent(Entity entity) {
        this.entity = entity;
    }
}