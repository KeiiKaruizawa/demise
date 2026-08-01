package gr.larryngton.demise.features.anticheat.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.modules.impl.misc.AntiCheat;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.sound.SoundUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public abstract class Check implements InstanceAccess {
    public final PlayerData data;
    public final EntityOtherPlayerMP player;

    public Check(PlayerData data) {
        this.data = data;
        player = data.getPlayer();
    }

    public TimerUtil flagTimer = new TimerUtil();

    public abstract String getName();

    public void onPacket(Packet<?> packet) {
    }

    public boolean isMove(Packet<?> packet) {
        return packet instanceof S14PacketEntity.S15PacketEntityRelMove
                || packet instanceof S14PacketEntity.S17PacketEntityLookMove
                || packet instanceof S14PacketEntity.S16PacketEntityLook;
    }

    public void flag(String description) {
        flag(description, "");
    }

    public void flag(String description, String verbose) {
        AntiCheat antiCheat = Demise.INSTANCE.getModuleManager().getModule(AntiCheat.class);

        if (flagTimer.hasTimeElapsed(antiCheat.alertCoolDown.get())) {
            if (!antiCheat.alertInChat.get()) {
                Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, player.getName() + EnumChatFormatting.WHITE + " has failed " + EnumChatFormatting.GRAY + getName() + EnumChatFormatting.WHITE, description, 5, new ScaledResolution(mc));
            } else {
                String prefix = EnumChatFormatting.DARK_GRAY + "demise" + EnumChatFormatting.GRAY + " » " + EnumChatFormatting.WHITE;
                ChatComponentText text = new ChatComponentText(prefix + player.getName() + EnumChatFormatting.WHITE + " has failed " + EnumChatFormatting.GRAY + getName() + EnumChatFormatting.DARK_GRAY + " (" + description + ")");

                if (!verbose.isBlank()) {
                    ChatStyle style = new ChatStyle();
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(verbose)));
                    text.setChatStyle(style);
                    mc.ingameGUI.getChatGUI().printChatMessage(text);
                } else {
                    mc.ingameGUI.getChatGUI().printChatMessage(text);
                }

                SoundUtil.notifSound(NotificationType.INFO);
            }

            antiCheat.mark(player);
            flagTimer.reset();
        }
    }
}