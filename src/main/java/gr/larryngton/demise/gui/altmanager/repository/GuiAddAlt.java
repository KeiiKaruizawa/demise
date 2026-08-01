package gr.larryngton.demise.gui.altmanager.repository;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.gui.altmanager.mslogin.MicrosoftAuthCallback;
import gr.larryngton.demise.gui.altmanager.repository.credential.AltCredential;
import gr.larryngton.demise.gui.altmanager.repository.credential.MicrosoftAltCredential;
import gr.larryngton.demise.gui.altmanager.utils.Checks;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.misc.UsernameGenerator;
import gr.larryngton.demise.utils.render.shader.impl.MenuBackground;
import lombok.Setter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjglx.Sys;
import org.lwjglx.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static net.minecraft.util.EnumChatFormatting.RED;

public final class GuiAddAlt extends GuiScreen {
    private final AltRepositoryGUI gui;
    private final BiConsumer<GuiAddAlt, ? super AltCredential> consumer;

    private final String addAltButtonName;
    private final String title;

    private GuiTextField usernameField;
    @Setter
    public String status = EnumChatFormatting.GRAY + "Idling...";

    public GuiAddAlt(@NotNull AltRepositoryGUI gui, @NotNull String addAltButtonName, @NotNull String title, @NotNull BiConsumer<GuiAddAlt, ? super AltCredential> consumer) {
        this.gui = gui;
        this.addAltButtonName = addAltButtonName;
        this.title = title;
        this.consumer = consumer;
    }

    @Override
    public void initGui() {
        int height = GuiScreen.height / 2 - 30;

        List<GuiButton> buttonList = this.buttonList;
        buttonList.add(new GuiButton(4, width / 2f - 100, height + 72 + 12 - 72, "Microsoft"));
        buttonList.add(new GuiButton(3, width / 2f - 100, height + 72 + 12 - 48, "Generate"));
        buttonList.add(new GuiButton(0, width / 2f - 100, height + 72 - 12, addAltButtonName));
        buttonList.add(new GuiButton(1, width / 2f - 100, height + 72 + 12, "Back"));

        this.usernameField = new GuiTextField(0, mc.fontRendererObj, width / 2 - 100, height - 30, 200, 20);
        usernameField.setFocused(true);

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MenuBackground.draw(Demise.INSTANCE.getStartTimeLong());

        GuiTextField usernameField = this.usernameField;

        usernameField.drawTextBox();

        Fonts.interMedium.get(20).drawString(status,
                (width - Fonts.interSemiBold.get(20).getStringWidth(status)) / 2.0F,
                15 + Fonts.interMedium.get(20).getHeight() + 11,
                new Color(198, 198, 198).getRGB());

        Fonts.interSemiBold.get(22).drawString(this.title, (width - Fonts.interSemiBold.get(22).getStringWidth(this.title)) / 2.0F, 15 + 5, -1);

        if (StringUtils.isBlank(usernameField.getText()) && !usernameField.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Username", width / 2F - 96, height / 2f - 54, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])$"
    );

    static boolean isEmail(@NotNull String email) {
        Checks.notBlank(email, "email");
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void add_login() {
        try {
            if (usernameField.getText().trim().isEmpty()) return;

            AltCredential altCredential = new AltCredential(usernameField.getText());
            String login = altCredential.getUsername();

            if (!login.matches("^[a-zA-Z0-9_]+$")) {
                setStatus(RED + "Invalid characters in username");
                return;
            }

            if (login.length() > 16) {
                setStatus(RED + "Username is too long");
                return;
            }

            consumer.accept(this, altCredential);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                add_login();
                break;
            case 1:
                mc.displayGuiScreen(gui);
                break;
            case 4:
                MicrosoftAuthCallback callback = new MicrosoftAuthCallback();

                CompletableFuture<MicrosoftAltCredential> future = callback.start((s, o) -> setStatus(String.format(s, o[0])));

                if (!Sys.openURL(MicrosoftAuthCallback.url)) {
                    System.out.println("Failed to open the login page. URL: " + MicrosoftAuthCallback.url);
                }

                future.whenCompleteAsync((o, t) -> {
                    if (t != null) {
                        setStatus(t.getClass().getName() + ':' + t.getMessage());
                        t.printStackTrace();
                    } else {
                        setStatus("Done");
                        consumer.accept(this, o);
                    }
                });
                break;
            case 3:
                usernameField.setText(UsernameGenerator.generate());
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
            add_login();
            return;
        }

        if (character == '\r') {
            mc.displayGuiScreen(gui);
        }

        usernameField.textboxKeyTyped(character, key);
    }

    @Override
    public void mouseClicked(int x2, int y2, int button) {
        try {
            super.mouseClicked(x2, y2, button);
        } catch (Throwable t) {
            Demise.LOGGER.warn(t);
        }

        usernameField.mouseClicked(x2, y2, button);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}