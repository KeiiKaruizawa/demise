package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.features.command.Command;
import net.minecraft.client.Minecraft;

public class JumpCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"jump", "j"};
    }

    @Override
    public void execute(final String[] arguments) {
        Minecraft.getMinecraft().thePlayer.jump();
    }

    @Override
    public String getUsage() {
        return "jump/j";
    }
}