package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.Event;
import net.minecraft.client.gui.ScaledResolution;

public record Render3DEvent(float partialTicks, ScaledResolution scaledResolution) implements Event {
}