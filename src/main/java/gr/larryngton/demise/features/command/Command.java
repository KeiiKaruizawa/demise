package gr.larryngton.demise.features.command;

import java.util.Collections;
import java.util.List;

public abstract class Command {
    public abstract String[] getAliases();

    public abstract void execute(final String[] p0) throws CommandExecutionException;

    public abstract String getUsage();

    public List<String> getArgumentSuggestions(String[] args) {
        return Collections.emptyList();
    }
}
