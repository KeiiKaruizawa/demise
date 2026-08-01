package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.utils.misc.ChatUtils;

public class HWIDCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"hwid", "id"};
    }

    @Override
    public String getUsage() {
        return "hwid / id";
    }

    @Override
    public void execute(String[] arguments) {
        ChatUtils.sendMessageClient("Your HWID is: " + Demise.HWID.getHWID());
    }
}
