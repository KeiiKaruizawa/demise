package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.utils.misc.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class VClipCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"vclip", "clip"};
    }

    @Override
    public void execute(final String[] arguments) {
        if (arguments.length == 2) {
            EntityPlayerSP pl = Minecraft.getMinecraft().thePlayer;

            pl.setPosition(pl.posX, pl.posY + Double.parseDouble(arguments[1]), pl.posZ);
            ChatUtils.sendMessageClient("Clipped " + arguments[1] + " blocks vertically.");
        } else {
            ChatUtils.sendMessageClient("Usage: " + getUsage());
        }
    }

    @Override
    public String getUsage() {
        return "vclip/clip <amount>";
    }
}
