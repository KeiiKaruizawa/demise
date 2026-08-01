package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ColorValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

import java.awt.*;

@ModuleInfo(name = "Atmosphere", description = "Changes the atmosphere.", legit = true)
public class Atmosphere extends Module {
    private final BoolValue time = new BoolValue("Time Editor", true, this);
    private final SliderValue timeValue = new SliderValue("Time", 18000, 0, 24000, 1000, this, time::get);
    private final BoolValue weather = new BoolValue("Weather Editor", true, this);
    private final ModeValue weatherValue = new ModeValue("Weather", new String[]{"Clean", "Rain", "Thunder"}, "Clean", this, weather::get);
    public final BoolValue worldColor = new BoolValue("World Color", true, this);
    public final ColorValue worldColorRGB = new ColorValue("World Color RGB", Color.WHITE, this, worldColor::get);
    public final BoolValue worldFog = new BoolValue("World Fog", false, this);
    public final ColorValue worldFogRGB = new ColorValue("World Fog RGB", Color.WHITE, this, worldFog::get);
    public final BoolValue noNightVisionSky = new BoolValue("No Night Vision Sky", false, this);

    @EventTarget
    private void onUpdate(UpdateEvent event) {
        if (time.get())
            mc.theWorld.setWorldTime((long) timeValue.get());
        if (weather.get()) {
            switch (weatherValue.get()) {
                case "Rain":
                    mc.theWorld.setRainStrength(1);
                    mc.theWorld.setThunderStrength(0);
                    break;
                case "Thunder":
                    mc.theWorld.setRainStrength(1);
                    mc.theWorld.setThunderStrength(1);
                    break;
                default:
                    mc.theWorld.setRainStrength(0);
                    mc.theWorld.setThunderStrength(0);
            }
        }
    }

    @EventTarget
    private void onPacket(PacketEvent event) {
        if (time.get() && event.getPacket() instanceof S03PacketTimeUpdate)
            event.setCancelled(true);
    }
}
