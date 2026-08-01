package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(name = "KnockbackDisplacement", description = "Fucks with your opponent's kb.")
public class KnockbackDisplacement extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Packet"}, "Packet", this);
    private final SliderValue delay = new SliderValue("Delay", 250, 0, 1000, 50, this, () -> mode.is("Packet"));

    private final TimerUtil timer = new TimerUtil();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (timer.hasTimeElapsed(delay.getInt())) {
            sendPacket(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw + (rand.nextBoolean() ? 90 : -90), mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
            timer.reset();
        }
    }
}
