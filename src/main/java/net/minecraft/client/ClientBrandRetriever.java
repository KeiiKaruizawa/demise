package net.minecraft.client;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.exploit.ClientSpoofer;

public class ClientBrandRetriever {
    public static String getClientModName() {
        ClientSpoofer clientSpoofer = Demise.INSTANCE.getModuleManager().getModule(ClientSpoofer.class);

        if (clientSpoofer.isEnabled()) {
            return switch (clientSpoofer.mode.get()) {
                case "Lunar" -> "lunarclient:v2.14.5-2411";
                case "Feather" -> "Feather Forge";
                case "Custom" -> clientSpoofer.customName.get();
                default -> "";
            };
        }

        return "vanilla";
    }
}