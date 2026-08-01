package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.SafeWalkEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import net.minecraft.item.ItemBlock;

@ModuleInfo(name = "SafeWalk", description = "Prevents you from falling from blocks.")
public class SafeWalk extends Module {
    private final BoolValue heldBlocks = new BoolValue("Held Blocks Check", true, this);
    private final BoolValue pitchCheck = new BoolValue("Pitch Check", true, this);
    private final SliderValue minPitch = new SliderValue("Min Pitch", 55, 50, 90, 1, this, pitchCheck::get);
    public final SliderValue maxPitch = new SliderValue("Max Pitch", 75, 50, 90, 1, this, pitchCheck::get);

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (canSafeWalk()) {
            event.setCancelled(true);
        }
    }

    public boolean canSafeWalk() {
        return mc.thePlayer.onGround && (!heldBlocks.get() || mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) && (!pitchCheck.get() || MathUtils.inBetween(minPitch.getMin(), maxPitch.getMax(), mc.thePlayer.rotationPitch));
    }
}