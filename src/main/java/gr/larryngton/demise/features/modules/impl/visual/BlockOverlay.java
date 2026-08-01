package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ColorValue;
import gr.larryngton.demise.features.values.impl.SliderValue;

import java.awt.*;

@ModuleInfo(name = "BlockOverlay", description = "Modifies the block overlay.", legit = true)
public class BlockOverlay extends Module {
    public final BoolValue outline = new BoolValue("Outline", true, this);
    public final BoolValue filled = new BoolValue("Filled", false, this);
    public final BoolValue syncColor = new BoolValue("Sync Color", false, this);
    public final ColorValue color = new ColorValue("Color", new Color(255, 255, 255), this, () -> !syncColor.get());
    public final BoolValue interpolate = new BoolValue("Interpolate", false, this);
    public final SliderValue interpolationAmount = new SliderValue("Interpolation Amount", 0.5f, 0.01f, 1, 0.01f, this, interpolate::get);
}
