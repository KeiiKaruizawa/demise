package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.command.CommandExecutionException;

public class ReloadCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"reload"};
    }

    @Override
    public void execute(String[] args) throws CommandExecutionException {
        Demise.INSTANCE.hotReload();
    }

    @Override
    public String getUsage() {
        return "reload";
    }
}
