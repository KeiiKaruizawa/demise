package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.HitSlowDownEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "KeepSprint", description = "Modifies slowdown on hit.")
public class KeepSprint extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Smart", "Hypixel"}, "Normal", this);
    private final SliderValue hurtTime = new SliderValue("HurtTime", 7, 0, 10, 1, this, () -> mode.is("Smart"));
    private final SliderValue motion = new SliderValue("Motion", 0.6F, 0.6F, 1, 0.01F, this);
    private final SliderValue hurtMotion = new SliderValue("Hurt motion", 0.6F, 0.6F, 1, 0.01F, this, () -> mode.is("Smart"));
    private final SliderValue attackMotion = new SliderValue("Attack motion", 1.0F, 0.0F, 1.0F, 0.05F, this, () -> mode.is("Hypixel"));
    private final SliderValue chance = new SliderValue("Chance", 100, 0, 100, 1, this, () -> mode.is("Hypixel"));

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onHitSlowDown(HitSlowDownEvent e) {
        Velocity velocity = getModule(Velocity.class);
        if (velocity.isEnabled() && velocity.mode.is("Reduce")) return;

        switch (mode.get()) {
            case "Normal":
                e.setSlowDown(motion.get());
                e.setSprint(motion.get() < 1);
                break;
            case "Smart":
                if (mc.thePlayer.hurtTime >= hurtTime.getInt()) {
                    e.setSlowDown(hurtMotion.get());
                    e.setSprint(hurtMotion.get() < 1);
                } else {
                    e.setSlowDown(motion.get());
                    e.setSprint(motion.get() < 1);
                }
                break;
            case "Hypixel":
                if (chance.getInt() != 100 && Math.random() * 100 >= chance.getInt()) {
                    break;
                }

                e.setSlowDown(attackMotion.get());
                e.setSprint(true);
                break;
        }
    }
}