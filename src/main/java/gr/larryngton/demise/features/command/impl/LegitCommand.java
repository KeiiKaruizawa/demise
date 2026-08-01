package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.command.CommandExecutionException;

public class LegitCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"legit"};
    }

    @Override
    public void execute(String[] args) throws CommandExecutionException {
        Demise.INSTANCE.toggleLegit();
    }

    @Override
    public String getUsage() {
        return "legit";
    }
}
