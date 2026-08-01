package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Item Physics", description = "Adds physics to dropped items.", legit = true)
public class ItemPhysics extends Module {
    public final SliderValue rotationSpeed = new SliderValue("Rotation speed", 2, 0, 10, 1, this);
}
