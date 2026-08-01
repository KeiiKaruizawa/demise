package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.misc.StringUtils;
import gr.larryngton.demise.utils.misc.UsernameGenerator;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@ModuleInfo(name = "Opsec", description = "sudo pacman -S opsec", legit = true)
public class Opsec extends Module {
    public static String name = "";

    @Override
    public void onEnable() {
        name = UsernameGenerator.generate();
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (e.getPacket() instanceof S02PacketChat s02) {
                IChatComponent iChatComponent = s02.getChatComponent();

                if (iChatComponent instanceof ChatComponentText) {
                    String newMessage = iChatComponent.getFormattedText().replace(mc.thePlayer.getGameProfile().getName(), name);
                    ChatComponentText newChatComponentText = new ChatComponentText(newMessage);
                    s02.setChatComponent(newChatComponentText);
                }

                e.setPacket(s02);
            }
        }
    }

    @EventTarget
    @EventPriority(-100)
    public void onRender2D(Render2DEvent e) {
        for (NetworkPlayerInfo player : mc.getNetHandler().getPlayerInfoMap()) {
            if (player.getGameProfile().getName().length() < 3 || player.getDisplayName() == null) continue;
            player.setDisplayName(new ChatComponentText(player.getDisplayName().getFormattedText().replaceFirst(mc.thePlayer.getGameProfile().getName(), name)));
        }
    }
}
