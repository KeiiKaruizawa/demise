package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@Getter
@Setter
@AllArgsConstructor
public class KillEvent implements Event {
    private EntityPlayer target;
}
