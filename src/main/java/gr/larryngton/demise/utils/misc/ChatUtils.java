package gr.larryngton.demise.utils.misc;

import gr.larryngton.demise.utils.InstanceAccess;
import lombok.experimental.UtilityClass;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.Serializable;

@UtilityClass
public class ChatUtils implements InstanceAccess {
    public void sendMessageClient(String message) {
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "demise" + EnumChatFormatting.GRAY + " » " + EnumChatFormatting.WHITE + message));
    }

    public void sendMessageClient(Serializable... messages) {
        StringBuilder builder = new StringBuilder();

        int i = 0;
        for (Serializable message : messages) {
            i++;
            builder.append(message).append(i != messages.length ? " | " : "");
        }

        sendMessageClient(builder.toString());
    }

    public void sendMessageServer(String message) {
        mc.thePlayer.sendChatMessage(message);
    }
}