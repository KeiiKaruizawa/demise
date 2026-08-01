package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.MouseOverEvent;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.entity.EntityLivingBase;

@ModuleInfo(name = "Reach", description = "Increases your reach.")
public class Reach extends Module {
    private final BoolValue dynamic = new BoolValue("Dynamic", false, this);
    private final SliderValue outputMultiplier = new SliderValue("Output multiplier", 1, 0.01F, 10, 0.01F, this, dynamic::get);
    private final SliderValue range = new SliderValue("Range ", 3, 0.1f, 6, 0.01f, this, () -> !dynamic.get());

    private EntityLivingBase target;

    public Reach() {
        outputMultiplier.setDescription("For the best results, set it to your ping divided by 50.");
    }

    @EventTarget
    public void onDisable() {
        target = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(dynamic.get() ? "Dynamic" : String.valueOf(range.get()));
        target = PlayerUtils.getTarget(8, "RayTrace");
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (dynamic.get()) {
            double dist = PlayerUtils.getDistanceToEntityBox(e.getTargetEntity());

            if (dist > 3) {
                ChatUtils.sendMessageClient(dist);
            }
        }
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent event) {
        if (!getModule(KillAura.class).isEnabled()) {
            if (dynamic.get()) {
                if (target != null) {
                    double playerToTarget = mc.thePlayer.getDistance(target.getPrevPositionVector()) - mc.thePlayer.getDistance(target.getPositionVector());
                    double targetToPlayer = target.getDistance(mc.thePlayer.getPrevPositionVector()) - target.getDistance(mc.thePlayer.getPositionVector());
                    double pred = Math.max(playerToTarget + targetToPlayer, 0) * outputMultiplier.get();
                    event.setRange(3 + pred);
                } else {
                    event.setRange(3);
                }
            } else {
                event.setRange(range.get());
            }
        }
    }
}