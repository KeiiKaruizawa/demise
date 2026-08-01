package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.Event;
import net.minecraft.client.gui.ScaledResolution;

public record Render2DEvent(float partialTicks, ScaledResolution scaledResolution) implements Event {
}