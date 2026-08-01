package gr.larryngton.demise.features.modules.impl.misc;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.misc.chat.IRCClient;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.packet.UserUtils;
import moe.lasoleil.axochat4j.packet.AxochatPacket;
import moe.lasoleil.axochat4j.packet.s2c.*;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(name = "IRC", description = "LiquidChat inside demise.", legit = true)
public class IRC extends Module {
    public final BoolValue jwt = new BoolValue("JWT", false, this);

    private boolean lastJwt;
    public static String jwtToken = "";

    public static final IRCClient client = new IRCClient() {
        private final Supplier<IRC> irc = Suppliers.memoize(() -> Demise.INSTANCE.getModuleManager().getModule(IRC.class));

        @Override
        public void onConnect() {
            chat("§7Connecting to chat server...");
        }

        @Override
        public void onConnected() {
            chat("§7Connected to chat server!");

            if (irc.get().jwt.get()) {
                loginJWT(jwtToken);
            } else if (UserUtils.isValidTokenOffline(mc.session.getToken())) {
                loginMojang();
            }
        }

        @Override
        public void onDisconnect() {
            chat("§cDisconnected from chat server!");
        }

        @Override
        public void onLogon() {
            chat("§7Logging in...");
        }

        @Override
        public void onPacket(AxochatPacket.S2C packet) {
            if (packet instanceof S2CMessagePacket messagePacket) {
                if (mc.thePlayer == null) {
                    Demise.LOGGER.info("[IRC] {}: {}", messagePacket.getAuthorInfo().getName(), messagePacket.getContent());
                    return;
                }

                IChatComponent chatComponent = new ChatComponentText(getPrefix() + "§7" + messagePacket.getAuthorInfo().getName() + ": ");
                IChatComponent messageComponent = toChatComponent(messagePacket.getContent());
                chatComponent.appendSibling(messageComponent);
                mc.thePlayer.addChatMessage(chatComponent);

            } else if (packet instanceof S2CPrivateMessagePacket privateMessage) {
                chat("§c(P)§7 " + privateMessage.getAuthorInfo().getName() + ": §7" + privateMessage.getContent());
            } else if (packet instanceof S2CErrorPacket errorPacket) {
                String message = switch (errorPacket.getMessage()) {
                    case "NotSupported" -> "This method is not supported!";
                    case "LoginFailed" -> "Login Failed!";
                    case "NotLoggedIn" -> "You must be logged in to use the chat!";
                    case "AlreadyLoggedIn" -> "You are already logged in!";
                    case "MojangRequestMissing" -> "Mojang request missing!";
                    case "NotPermitted" -> "You are missing the required permissions!";
                    case "NotBanned" -> "You are not banned!";
                    case "Banned" -> "You are banned!";
                    case "RateLimited" -> "You have been rate limited. Please try again later.";
                    case "PrivateMessageNotAccepted" -> "Private message not accepted!";
                    case "EmptyMessage" -> "You are trying to send an empty message!";
                    case "MessageTooLong" -> "Message is too long!";
                    case "InvalidCharacter" -> "Message contains a non-ASCII character!";
                    case "InvalidId" -> "The given ID is invalid!";
                    case "Internal" -> "An internal server error occurred!";
                    default -> errorPacket.getMessage();
                };
                chat("§cError: §7" + message);
            } else if (packet instanceof S2CSuccessPacket successPacket) {
                switch (successPacket.getReason()) {
                    case "Login":
                        chat("§7Logged in!");
                        chat("§7Send a message: .irc <message>");
                        loggedIn = true;
                        break;
                    case "Ban":
                        chat("§7Successfully banned user!");
                        break;
                    case "Unban":
                        chat("§7Successfully unbanned user!");
                        break;
                }
            } else if (packet instanceof S2CNewJWTPacket newJWTPacket) {
                jwtToken = newJWTPacket.getToken();
                jwt = true;
                irc.get().setEnabled(false);
                irc.get().setEnabled(true);
            }
        }

        @Override
        public void onError(Throwable cause) {
            chat("§c§lError: §7" + cause.getClass().getName() + ": " + cause.getMessage());
        }
    };

    private static final AtomicLong lastConnectTimestamp = new AtomicLong(0);
    private static final long CONNECT_DELAY_MILLIS = 5000L;

    private void tryConnect() {
        long now = System.currentTimeMillis();
        long lastTime = lastConnectTimestamp.get();

        if (now - lastTime >= CONNECT_DELAY_MILLIS) {
            if (lastConnectTimestamp.compareAndSet(lastTime, now)) {
                connect();
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        tryConnect();

        if (lastJwt != jwt.get()) {
            if (this.isEnabled()) {
                this.setEnabled(false);
                this.setEnabled(true);
            }
        }

        lastJwt = jwt.get();
    }

    @Override
    public void onDisable() {
        client.disconnect();
    }

    private void connect() {
        if (client.isConnected()) return;

        if (jwt.get() && jwtToken.isEmpty()) {
            chat("§cError: §7No token provided!");
            setEnabled(false);
            return;
        }

        try {
            client.connect();
        } catch (Exception e) {
            Demise.LOGGER.error("IRC error", e);
            chat("§cError: §7" + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("all")
    private static final Pattern urlPattern = Pattern.compile(
            "((?:[a-z0-9]{2,}://)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_.]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"§ \n]|$))",
            Pattern.CASE_INSENSITIVE
    );

    private static IChatComponent toChatComponent(String string) {
        IChatComponent component = null;
        Matcher matcher = urlPattern.matcher(string);
        int lastEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String part = string.substring(lastEnd, start);

            if (!part.isEmpty()) {
                if (component == null) {
                    component = new ChatComponentText(part);
                    component.getChatStyle().setColor(EnumChatFormatting.GRAY);
                } else {
                    component.appendText(part);
                }
            }

            String url = string.substring(start, end);
            lastEnd = end;

            try {
                if (new URI(url).getScheme() != null) {
                    IChatComponent link = new ChatComponentText(url);
                    link.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    link.getChatStyle().setUnderlined(true);
                    link.getChatStyle().setColor(EnumChatFormatting.GRAY);

                    if (component == null)
                        component = link;
                    else
                        component.appendSibling(link);

                    continue;
                }
            } catch (URISyntaxException ignored) {
            }

            if (component == null) {
                component = new ChatComponentText(url);
                component.getChatStyle().setColor(EnumChatFormatting.GRAY);
            } else {
                component.appendText(url);
            }
        }

        String endText = string.substring(lastEnd);
        if (component == null) {
            component = new ChatComponentText(endText);
            component.getChatStyle().setColor(EnumChatFormatting.GRAY);
        } else if (!endText.isEmpty()) {
            component.appendText(endText);
        }

        return component;
    }

    private static void chat(String message) {
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(getPrefix() + message));
    }

    private static String getPrefix() {
        return EnumChatFormatting.DARK_GRAY + "[IRC]" + EnumChatFormatting.GRAY + " » " + EnumChatFormatting.WHITE;
    }
}
