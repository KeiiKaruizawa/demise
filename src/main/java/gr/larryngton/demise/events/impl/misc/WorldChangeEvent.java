package gr.larryngton.demise.events.impl.misc;

import gr.larryngton.demise.events.impl.Event;
import net.minecraft.world.World;

public record WorldChangeEvent(World oldWorld, World newWorld) implements Event {
}
