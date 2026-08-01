package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.features.values.impl.TextValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import net.minecraft.network.play.server.S02PacketChat;

@ModuleInfo(name = "AutoAccount", description = "Automatically does account-related shit.")
public class AutoAccount extends Module {
    private final TextValue password = new TextValue("Password", "FoxesAreCute", this);
    private final SliderValue delay = new SliderValue("Delay", 5, 0, 20, 1, this);

    private final TimerUtil timer = new TimerUtil();
    private boolean received1;
    private boolean received2;
    private boolean reg;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (timer.hasTimeElapsed(delay.get() * 50)) {
            if (received1) {
                ChatUtils.sendMessageServer((reg ? "/reg " : "/register ") + password.get() + " " + password.get());
                received1 = false;
            } else if (received2) {
                ChatUtils.sendMessageServer("/login " + password.get());
                received2 = false;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (e.getPacket() instanceof S02PacketChat s02PacketChat) {
                String str = s02PacketChat.getChatComponent().getUnformattedText();

                if (str.contains("/reg")) {
                    reg = true;
                    received1 = true;
                    timer.reset();
                } else if (str.contains("/register")) {
                    reg = false;
                    received1 = true;
                    timer.reset();
                }

                // "Please login to proceed!"
                if (str.contains("/login") || str.contains("login to proceed")) {
                    received2 = true;
                    timer.reset();
                }

                // "You have already registered!"
                if (received1 && received2 && str.contains("already registered")) {
                    received1 = false;
                    timer.reset();
                }
            }
        }
    }
}