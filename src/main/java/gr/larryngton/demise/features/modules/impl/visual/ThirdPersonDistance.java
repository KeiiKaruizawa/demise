package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "ThirdPersonDistance", description = "Modifies the third person camera's distance to the player.", legit = true)
public class ThirdPersonDistance extends Module {
    public final SliderValue cameraDistance = new SliderValue("Distance", 4.0f, 1.0f, 8.0f, 1.0f, this);
}