package gr.larryngton.demise.features.modules.impl.misc;

import com.google.gson.JsonArray;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.api.HttpUtils;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@ModuleInfo(name = "Translator", description = "Translates your chat.", legit = true)
public class Translator extends Module {

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.INCOMING) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        if (e.getPacket() instanceof S02PacketChat s02) {
            if (!s02.isChat()) {
                return;
            }

            String text = StringUtils.stripControlCodes(s02.getChatComponent().getFormattedText());

            if (text.contains("\n")) {
                return;
            }

            e.setCancelled(true);

            HttpUtils.send(it -> it.url("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)))
                    .thenApply(HttpUtils.bodyGson(JsonArray.class))
                    .thenApply(array -> {
                        String translated = array.get(0).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
                        ChatComponentText translatedComponent = new ChatComponentText(translated);
                        String language = new Locale(array.get(2).getAsString()).getDisplayLanguage(Locale.ENGLISH);

                        if (!translated.equals(text)) {
                            translatedComponent.appendText(" ");
                            ChatComponentText hoverComponent = new ChatComponentText(EnumChatFormatting.DARK_GRAY + "[T]");
                            ChatStyle style = new ChatStyle();
                            style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Translated from " + language + "\n" + text)));
                            hoverComponent.setChatStyle(style);

                            translatedComponent.appendSibling(hoverComponent);
                        }

                        return translatedComponent;
                    })
                    .thenAcceptAsync(mc.thePlayer::addChatMessage, mc);
        }
    }
}