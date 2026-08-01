package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.gui.notification.NotificationType;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

@ModuleInfo(name = "FlagDetector", description = "Alerts you about anticheat flags.", legit = true)
public class FlagDetector extends Module {
    private int totalFlags;

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING && mc.thePlayer.ticksExisted > 20) {
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                totalFlags++;
                alert("Lagback");
            }
        }

        if (e.getPacket() instanceof S01PacketJoinGame || e.getPacket() instanceof S00PacketDisconnect) {
            totalFlags = 0;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!mc.thePlayer.isDead) {
            if (mc.thePlayer.getHealth() < 0) {
                totalFlags++;
                alert("Invalid health");
            }

            if (mc.thePlayer.getFoodStats().getFoodLevel() < 0) {
                totalFlags++;
                alert("Invalid hunger");
            }
        }
    }

    private void alert(String flagType) {
        Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, "FlagDetector", "Detected " + flagType + " (" + totalFlags + "x)", new ScaledResolution(mc));
    }
}