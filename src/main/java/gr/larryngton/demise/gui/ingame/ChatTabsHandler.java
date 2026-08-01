package gr.larryngton.demise.gui.ingame;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.SendMessageEvent;
import gr.larryngton.demise.features.modules.impl.visual.CustomWidgetsModule;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.InstanceAccess;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Mouse;

import java.awt.*;
import java.util.List;

public class ChatTabsHandler implements InstanceAccess {
    private static boolean globalChat = true;
    private boolean lastMouseDown;
    private final GuiButton globalButton = new GuiButton(0, 0, 0, 0, 0, "Global");
    private final GuiButton filteredButton = new GuiButton(0, 0, 0, 0, 0, "Filtered");

    @EventTarget
    @EventPriority(-100)
    public void onSendMessage(SendMessageEvent e) {
        if (!globalChat) {
            if (!e.getMessage().startsWith("/")) {
                String prefix = Demise.INSTANCE.getClientSettings().messagePrefixInTab.get();
                String add = " ";

                if (prefix.equals("/") || prefix.equals(".")) {
                    add = "";
                }

                String str = prefix + add + e.getMessage();
                e.setMessage(str);
            }
        }
    }

    public void render() {
        if (!Demise.INSTANCE.getClientSettings().chatTabs.get()) {
            return;
        }

        CustomWidgetsModule customWidgets = Demise.INSTANCE.getModuleManager().getModule(CustomWidgetsModule.class);
        ScaledResolution sr = new ScaledResolution(mc);

        float x;
        float y;
        float width = MathHelper.ceiling_float_int((float) GuiNewChat.getChatWidth() / GuiNewChat.getChatScale()) + 4;
        float height = 12;

        if (!customWidgets.isEnabled() || !customWidgets.chat.get()) {
            x = 8;
            y = sr.getScaledHeight() - height - x - 12;
        } else {
            x = 10;
            y = sr.getScaledHeight() - 43;
        }

        x += width + 4;

        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        FontRenderer fr = Fonts.interRegular.get(15);

        float globalWidth = fr.getStringWidth("Global") + 8;
        float filteredWidth = fr.getStringWidth("Filtered") + 8;

        if (Demise.INSTANCE.getClientSettings().buttonStyle.is("Vanilla")) {
            height += 8;
            y -= 8;
            globalWidth = mc.fontRendererObj.getStringWidth("Global") + 8;
            filteredWidth = mc.fontRendererObj.getStringWidth("Filtered") + 8;
        }

        globalButton.xPosition = x;
        globalButton.yPosition = y;
        globalButton.width = globalWidth;
        globalButton.height = height;
        globalButton.radius = 5;

        filteredButton.xPosition = x + globalWidth + 4;
        filteredButton.yPosition = y;
        filteredButton.width = filteredWidth;
        filteredButton.height = height;
        filteredButton.radius = 5;

        GuiButton.shader = false;
        globalButton.drawButton(mc, mouseX, mouseY);
        filteredButton.drawButton(mc, mouseX, mouseY);

        GuiButton.shader = true;
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
            globalButton.drawButton(mc, mouseX, mouseY);
            filteredButton.drawButton(mc, mouseX, mouseY);
        });

        boolean down = Mouse.isButtonDown(0);

        if (!lastMouseDown && down) {
            if (globalButton.mousePressed(mc, mouseX, mouseY)) {
                if (!globalChat) {
                    globalChat = true;
                    globalButton.playPressSound(mc.getSoundHandler());
                }
            }

            if (filteredButton.mousePressed(mc, mouseX, mouseY)) {
                if (globalChat) {
                    globalChat = false;
                    filteredButton.playPressSound(mc.getSoundHandler());
                }
            }
        }

        lastMouseDown = down;

        globalButton.enabled = !globalChat;
        filteredButton.enabled = globalChat;
    }

    public List<ChatLine> overrideChatLines(List<ChatLine> initChatLines) {
        if (Demise.INSTANCE.getClientSettings().chatTabs.get()) {
            if (globalChat) {
                return initChatLines.stream().filter(line -> !matches(line)).toList();
            } else {
                return initChatLines.stream().filter(this::matches).toList();
            }
        }
        return initChatLines;
    }

    private boolean matches(ChatLine line) {
        String text = line.getChatComponent().getUnformattedText();
        text = text.replaceAll("§.", "");

        return text.startsWith(Demise.INSTANCE.getClientSettings().messageFilter.get());
    }
}
