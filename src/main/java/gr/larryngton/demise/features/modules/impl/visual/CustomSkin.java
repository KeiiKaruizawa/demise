package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.TextValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

@ModuleInfo(name = "CustomSkin", description = "Show a skin clientside", legit = true)
public class CustomSkin extends Module {
    public final BoolValue slimSkin = new BoolValue("Slim", true, this);
    public final BoolValue customUrl = new BoolValue("Custom URL", false, this);
    public final ModeValue skinMode = new ModeValue("Skin", new String[]{"❤", "larryngton", "Michael Ernst", "izuna", "GommeHD", "Ye", "Karuizawa"}, "larryngton", this, () -> !customUrl.get());
    public final TextValue customSkinUrl = new TextValue("URL", "https://s.namemc.com/i/afd98273bb9a8b69.png", this, customUrl::get);

    private String lastAppliedState = "";

    @Override
    public void onEnable() {
        lastAppliedState = "";
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getGameProfile().getId());
        if (info != null) {
            info.resetPlayerTextures();
        }
    }

    @EventTarget
    public void onGame(GameEvent event) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        String currentState = getURL() + "|" + slimSkin.get();
        if (currentState.equals(lastAppliedState)) {
            return;
        }

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getGameProfile().getId());
        if (info != null) {
            info.resetPlayerTextures();
            lastAppliedState = currentState;
        }
    }

    public String getURL() {
        if (!customUrl.get()) {
            return switch (skinMode.get()) {
                case "❤" -> "https://s.namemc.com/i/f684b65b81072fb3.png";
                case "larryngton" -> "https://s.namemc.com/i/afd98273bb9a8b69.png";
                case "Michael Ernst" -> "https://s.namemc.com/i/c6e0ab215c046d63.png";
                case "izuna" -> "https://s.namemc.com/i/b95cabd61250a94d.png";
                case "GommeHD" -> "https://s.namemc.com/i/0f1ee89922626929.png";
                case "Ye" -> "https://s.namemc.com/i/d4099d0f94e33b28.png";
                case "Karuizawa" -> "https://s.namemc.com/i/1d71e2c3fc382eac.png";
                default -> "";
            };
        } else {
            return customSkinUrl.get();
        }
    }
}