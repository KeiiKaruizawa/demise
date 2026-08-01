package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.SimulatedPlayer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

@ModuleInfo(name = "JumpReset", description = "Automatically jump resets in order to reduce velocity.")
public class JumpReset extends Module {
    private final SliderValue exitRange = new SliderValue("Exit range", 3, 1, 6, 0.01F, this);
    private final SliderValue selfPredictionTicks = new SliderValue("Prediction ticks", 2, 0, 5, 1, this);
    private final BoolValue log = new BoolValue("Log", false, this);

    private boolean jump;
    private int simHurtTime;

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        EntityLivingBase target = PlayerUtils.getTarget(6);

        if (target != null && jump && shouldJump(target)) {
            if (log.get()) {
                ChatUtils.sendMessageClient("jumped");
            }

            e.setJumping(true);
        }

        jump = false;
    }

    private boolean shouldJump(EntityLivingBase target) {
        SimulatedPlayer simPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simHurtTime = mc.thePlayer.hurtTime;

        for (int i = 0; i < selfPredictionTicks.getInt(); i++) {
            simPlayer.tick();

            if (simHurtTime > 0) {
                simHurtTime--;
            }
        }

        if (simHurtTime <= 0) {
            simulateKB(simPlayer, target);
        }

        Vec3 predictedTarget = target.getPositionVector().add(target.getMoveDeltaVector());
        double dist = PlayerUtils.getDistToTargetFromMouseOver(
                simPlayer.getPos().add(0, simPlayer.getEyeHeight(), 0),
                mc.thePlayer.getLook(1),
                target,
                target.getHitbox().offset(predictedTarget.subtract(target.getPositionVector()))
        );

        return mc.thePlayer.isSprinting() && dist > exitRange.get() && PlayerUtils.getDistanceToEntityBox(target) <= exitRange.get();
    }

    private void simulateKB(SimulatedPlayer simPlayer, EntityLivingBase attacker) {
        int i = 0;
        i = i + EnchantmentHelper.getKnockbackModifier(attacker);

        if (attacker.isSprinting()) {
            ++i;
        }

        simPlayer.motionX += -MathHelper.sin(attacker.rotationYawHead * (float) Math.PI / 180.0F) * (float) i * 0.5F;
        simPlayer.motionY += 0.1D;
        simPlayer.motionZ += MathHelper.cos(attacker.rotationYawHead * (float) Math.PI / 180.0F) * (float) i * 0.5F;

        simHurtTime = 10;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING && e.getPacket() instanceof S12PacketEntityVelocity s12) {
            if (mc.thePlayer.onGround && s12.getEntityID() == mc.thePlayer.getEntityId() && s12.motionY > 0 && mc.currentScreen == null) {
                jump = true;
            }
        }
    }
}