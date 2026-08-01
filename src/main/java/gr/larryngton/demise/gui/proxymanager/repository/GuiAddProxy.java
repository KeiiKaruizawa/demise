package gr.larryngton.demise.gui.proxymanager.repository;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.render.shader.impl.MenuBackground;
import lombok.Setter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjglx.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

import static net.minecraft.util.EnumChatFormatting.RED;

public final class GuiAddProxy extends GuiScreen {
    private final ProxyRepositoryGUI gui;
    private final BiConsumer<GuiAddProxy, Proxy> consumer;

    private final String addProxyButtonName;
    private final String title;

    private GuiTextField hostField;
    private GuiTextField portField;
    private GuiTextField usernameField;
    private GuiTextField passwordField;

    @Setter
    public String status = EnumChatFormatting.GRAY + "Idling...";

    public GuiAddProxy(@NotNull ProxyRepositoryGUI gui, @NotNull String addProxyButtonName, @NotNull String title, @NotNull BiConsumer<GuiAddProxy, Proxy> consumer) {
        this.gui = gui;
        this.addProxyButtonName = addProxyButtonName;
        this.title = title;
        this.consumer = consumer;
    }

    @Override
    public void initGui() {
        int height = GuiScreen.height / 2 - 68;

        List<GuiButton> buttonList = this.buttonList;
        buttonList.add(new GuiButton(0, width / 2f - 100, height + 108, addProxyButtonName));
        buttonList.add(new GuiButton(1, width / 2f - 100, height + 132, "Back"));

        this.hostField = new GuiTextField(0, mc.fontRendererObj, width / 2 - 100, height, 200, 20);
        this.portField = new GuiTextField(1, mc.fontRendererObj, width / 2 - 100, height + 24, 200, 20);
        this.usernameField = new GuiTextField(2, mc.fontRendererObj, width / 2 - 100, height + 48, 200, 20);
        this.passwordField = new GuiTextField(3, mc.fontRendererObj, width / 2 - 100, height + 72, 200, 20);

        this.hostField.setMaxStringLength(256);
        this.portField.setMaxStringLength(5);
        this.usernameField.setMaxStringLength(256);
        this.passwordField.setMaxStringLength(256);

        this.hostField.setFocused(true);

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MenuBackground.draw(Demise.INSTANCE.getStartTimeLong());

        hostField.drawTextBox();
        portField.drawTextBox();
        usernameField.drawTextBox();
        passwordField.drawTextBox();

        Fonts.interMedium.get(20).drawString(status,
                (width - Fonts.interSemiBold.get(20).getStringWidth(status)) / 2.0F,
                15 + Fonts.interMedium.get(20).getHeight() + 11,
                new Color(198, 198, 198).getRGB());

        Fonts.interSemiBold.get(22).drawString(this.title, (width - Fonts.interSemiBold.get(22).getStringWidth(this.title)) / 2.0F, 15 + 5, -1);

        if (StringUtils.isBlank(hostField.getText()) && !hostField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Host", width / 2F - 96, hostField.yPosition + 6, 0xFF888888);
        }

        if (StringUtils.isBlank(portField.getText()) && !portField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Port", width / 2F - 96, portField.yPosition + 6, 0xFF888888);
        }

        if (StringUtils.isBlank(usernameField.getText()) && !usernameField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Username", width / 2F - 96, usernameField.yPosition + 6, 0xFF888888);
        }

        if (StringUtils.isBlank(passwordField.getText()) && !passwordField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Password", width / 2F - 96, passwordField.yPosition + 6, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void add_proxy() {
        try {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (host.isEmpty() || portText.isEmpty()) {
                setStatus(RED + "Host and port are required");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                setStatus(RED + "Invalid port");
                return;
            }

            if (port < 1 || port > 65535) {
                setStatus(RED + "Port out of range");
                return;
            }

            consumer.accept(this, new Proxy(gui, host, port, username, password));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                add_proxy();
                break;
            case 1:
                mc.displayGuiScreen(gui);
                break;
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == 1) {
            mc.displayGuiScreen(gui);
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            add_proxy();
            return;
        }

        hostField.textboxKeyTyped(character, key);
        portField.textboxKeyTyped(character, key);
        usernameField.textboxKeyTyped(character, key);
        passwordField.textboxKeyTyped(character, key);
    }

    @Override
    public void mouseClicked(int x2, int y2, int button) {
        try {
            super.mouseClicked(x2, y2, button);
        } catch (Throwable t) {
            Demise.LOGGER.warn(t);
        }

        hostField.mouseClicked(x2, y2, button);
        portField.mouseClicked(x2, y2, button);
        usernameField.mouseClicked(x2, y2, button);
        passwordField.mouseClicked(x2, y2, button);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}