package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Shaders", description = "Renders shaders on HUD elements.", legit = true)
public class Shaders extends Module {
    public final BoolValue blur = new BoolValue("Blur", true, this);
    public final SliderValue blurRadius = new SliderValue("Blur radius", 10, 1, 50, 1, this, blur::get);
    public final BoolValue shadow = new BoolValue("Shadow", true, this);
    public final SliderValue shadowRadius = new SliderValue("Shadow radius", 17, 1, 50, 1, this, shadow::get);
    public final BoolValue bloom = new BoolValue("Bloom", false, this);
}