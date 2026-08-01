package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(name = "AntiVoid", description = "Prevents you from falling into the void.")
public class AntiVoid extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Collide", "Packet"}, "Collide", this);
    private final SliderValue fallDistance = new SliderValue("Min fall distance", 3, 0, 10, 1, this);

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) {
            return;
        }

        if (PlayerUtils.overVoid() && mc.thePlayer.fallDistance >= fallDistance.get()) {
            switch (mode.get()) {
                case "Collide":
                    mc.thePlayer.motionY = Math.floor(mc.thePlayer.posY) - mc.thePlayer.posY;
                    if (mc.thePlayer.motionY == 0) {
                        e.setOnGround(true);
                    }
                    break;
                case "Packet":
                    sendPacket(new C03PacketPlayer.C04PacketPlayerPosition());
                    break;
            }
        }
    }
}