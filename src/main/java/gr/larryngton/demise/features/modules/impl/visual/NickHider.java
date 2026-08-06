package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.PrintChatMessageEvent;
import gr.larryngton.demise.events.impl.misc.SendMessageEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.TextValue;
import net.minecraft.util.ChatComponentText;

import java.util.Locale;
import java.util.regex.Pattern;

@ModuleInfo(name = "NickHider", description = "Hides your username", legit = true)
public class NickHider extends Module {
    public static NickHider INSTANCE;

    public final TextValue customNick = new TextValue("Custom nick", "", this);

    @Override
    public void onEnable() {
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        INSTANCE = null;
    }

    private String cachedRealName;
    private Pattern cachedRealNamePattern;

    private Pattern getRealNamePattern(String realName) {
        if (!realName.equals(cachedRealName)) {
            cachedRealName = realName;
            cachedRealNamePattern = Pattern.compile(Pattern.quote(realName), Pattern.CASE_INSENSITIVE);
        }
        return cachedRealNamePattern;
    }

    private String getRealName() {
        if (mc.session == null) {
            return null;
        }
        return mc.session.getUsername();
    }

    @EventTarget
    public void onSendMessage(SendMessageEvent event) {
        String realName = getRealName();
        String fakeName = customNick.get();
        if (realName == null || fakeName == null || fakeName.isEmpty()) {
            return;
        }

        String message = event.getMessage();
        if (message.toLowerCase(Locale.ROOT).contains(fakeName.toLowerCase(Locale.ROOT))) {
            Pattern fakeNamePattern = Pattern.compile(Pattern.quote(fakeName), Pattern.CASE_INSENSITIVE);
            event.setMessage(fakeNamePattern.matcher(message).replaceAll(realName));
        }
    }

    @EventTarget
    public void onPrintChatMessage(PrintChatMessageEvent event) {
        String realName = getRealName();
        String fakeName = customNick.get();
        if (!isEnabled() || realName == null || fakeName == null || fakeName.isEmpty()) {
            return;
        }

        String formatted = event.getChatComponent().getFormattedText();
        if (formatted.toLowerCase(Locale.ROOT).contains(realName.toLowerCase(Locale.ROOT))) {
            String masked = getRealNamePattern(realName).matcher(formatted).replaceAll(fakeName);
            event.setChatComponent(new ChatComponentText(masked));
        }
    }

    public String apply(String input) {
        String realName = getRealName();
        String fakeName = customNick.get();
        if (!isEnabled() || input == null || realName == null || fakeName == null || fakeName.isEmpty()) {
            return input;
        }

        if (input.toLowerCase(Locale.ROOT).contains(realName.toLowerCase(Locale.ROOT))) {
            return getRealNamePattern(realName).matcher(input).replaceAll(fakeName);
        }

        return input;
    }
}