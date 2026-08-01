package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.packet.PacketReleaseEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(name = "Balance", description = "Displays the tick difference between the client and the server.", legit = true)
public class Balance extends Module {
    private int balance;
    private long lastC03;

    @Override
    public void onEnable() {
        lastC03 = System.currentTimeMillis();
        balance = 0;
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        lastC03 = System.currentTimeMillis();
        balance = 0;
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (SpoofSlotUtils.isSpoofing() && getModule(CustomWidgetsModule.class).isEnabled() && getModule(CustomWidgetsModule.class).hotbar.get() && getModule(CustomWidgetsModule.class).hotbarMode.is("Default")) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        Fonts.interRegular.get(18).drawCenteredString("Balance: " + balance, sr.getScaledWidth() / 2f, sr.getScaledHeight() - 80, -1);
    }

    @EventTarget
    public void onPacketRelease(PacketReleaseEvent e) {
        if (e.getTimedPacket().getPacket() instanceof C03PacketPlayer) {
            balance += 50;
        }
    }

    @EventTarget
    @EventPriority(100)
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.OUTGOING && e.getPacket() instanceof C03PacketPlayer) {
            if (!e.isCancelled()) {
                balance += 50;
            }

            balance -= (int) (System.currentTimeMillis() - lastC03);
            lastC03 = System.currentTimeMillis();
        }
    }
}
