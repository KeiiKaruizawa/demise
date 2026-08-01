package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import net.minecraft.entity.passive.EntityHorse;

@ModuleInfo(name = "PerfectHorseJump", description = "Hors.")
public class PerfectHorseJump extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Legit", "Always charged"}, "Legit", this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        if (mode.is("Always charged") && mc.thePlayer.ridingEntity instanceof EntityHorse) {
            mc.thePlayer.horseJumpPowerCounter = 9;
            mc.thePlayer.horseJumpPower = 1;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mode.is("Legit") && mc.thePlayer.ridingEntity instanceof EntityHorse && e.isJumping() && mc.thePlayer.horseJumpPowerCounter == 9) {
            e.setJumping(false);
        }
    }
}
