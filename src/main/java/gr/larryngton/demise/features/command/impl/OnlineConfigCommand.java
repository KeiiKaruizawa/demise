package gr.larryngton.demise.features.command.impl;

import com.google.gson.JsonObject;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.utils.api.HttpUtils;
import gr.larryngton.demise.utils.misc.ChatUtils;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public class OnlineConfigCommand extends Command {

    @Override
    public String getUsage() {
        return "onlineconfig/ocf <load> <config>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"onlineconfig", "ocf"};
    }

    private boolean loading = false;

    @Override
    public void execute(String[] args) {
        if (!validateArgs(args)) {
            ChatUtils.sendMessageClient("Usage: " + getUsage());
            return;
        }

        if (args[1].equalsIgnoreCase("load")) {
            if (loading) {
                ChatUtils.sendMessageClient("Already loading!");
                return;
            }

            loading = true;
            loadConfig(args[2]);
        }
    }

    private boolean validateArgs(String[] args) {
        return args.length >= 3 && args[1].equalsIgnoreCase("load");
    }

    private void loadConfig(String configName) {
        String normalizedConfigName = configName.toLowerCase(Locale.getDefault());
        HttpUtils.send(it -> it.url(Demise.INSTANCE.getCloud() + "/configs/" + normalizedConfigName + ".json"))
                .thenApply(HttpUtils.bodyGson(JsonObject.class))
                .thenAcceptAsync(jsonConfig -> {
                    if (Demise.INSTANCE.getConfigManager().loadOnlineConfig(
                            Demise.INSTANCE.getConfigManager().getSetting(), jsonConfig)) {
                        ChatUtils.sendMessageClient("Loaded config: " + configName);
                    } else {
                        ChatUtils.sendMessageClient("Invalid config: " + configName);
                    }
                    loading = false;
                }, Minecraft.getMinecraft())
                .exceptionallyAsync(t -> {
                    Demise.LOGGER.error("Failed to load config: {}", configName, t);
                    ChatUtils.sendMessageClient("Failed to load config: " + t.getMessage());
                    loading = false;
                    return null;
                }, Minecraft.getMinecraft());
    }

}