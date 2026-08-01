package gr.larryngton.demise.gui.click.components.setting;

import gr.larryngton.demise.features.values.impl.TextValue;
import gr.larryngton.demise.gui.click.Component;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.animations.Animation;
import gr.larryngton.demise.utils.animations.Direction;
import gr.larryngton.demise.utils.animations.impl.DecelerateAnimation;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.apache.commons.lang3.math.NumberUtils;
import org.lwjglx.input.Keyboard;

import java.awt.*;

public class StringComponent extends Component {
    private final TextValue setting;
    private final Animation input = new DecelerateAnimation(250, 1);
    private boolean inputting;
    private String text = "";

    public StringComponent(TextValue setting) {
        this.setting = setting;
        setHeight(Fonts.interRegular.get(14).getHeight() * 2 + 4);
        input.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        setDescription(setting.getDescription());

        input.setDirection(inputting ? Direction.FORWARDS : Direction.BACKWARDS);
        text = setting.get();
        if (setting.isOnlyNumber() && !NumberUtils.isNumber(text)) {
            text = text.replaceAll("[a-zA-Z]", "");
        }
        String textToDraw = setting.get().isEmpty() && !inputting ? "Empty..." : setting.getText();
        Fonts.interRegular.get(14).drawString(setting.getName(), getX() + 4, getY(), -1);
        drawTextWithLineBreaks(textToDraw + (inputting && text.length() < 59 && System.currentTimeMillis() % 1000 > 500 ? "|" : ""), getX() + 6, getY() + Fonts.interRegular.get(14).getHeight() + 2);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(getX() + 6, getY() + Fonts.interRegular.get(14).getHeight(), getWidth(), Fonts.interRegular.get(15).getHeight() + 4, mouseX, mouseY) && mouseButton == 0) {
            inputting = !inputting;
        } else {
            inputting = false;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (inputting && GuiScreen.isKeyComboCtrlV(keyCode)) {
            String clipboard = GuiScreen.getClipboardString();
            if (clipboard != null && !clipboard.isEmpty()) {
                StringBuilder filtered = new StringBuilder();
                for (char c : clipboard.toCharArray()) {
                    if (setting.isOnlyNumber() && !Character.isDigit(c)) {
                        continue;
                    }
                    if (ChatAllowedCharacters.isAllowedCharacter(c) || c == ' ') {
                        filtered.append(c);
                    }
                }
                text += filtered;
                setting.setText(text);
                SoundUtil.playSound("demise.tick");
            }
            super.keyTyped(typedChar, keyCode);
            return;
        }

        if (setting.isOnlyNumber() && !NumberUtils.isNumber(String.valueOf(typedChar))) {
            return;
        }
        if (inputting) {
            if (keyCode == Keyboard.KEY_BACK) {
                deleteLastCharacter();
            }

            if (keyCode == Keyboard.KEY_SPACE) {
                text += ' ';
                setting.setText(text);
            } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                text += typedChar;
                setting.setText(text);
            }

            SoundUtil.playSound("demise.tick");
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void drawTextWithLineBreaks(String text, float x, float y) {
        String[] lines = text.split("\n");
        float currentY = y;

        for (String line : lines) {
            Fonts.interRegular.get(15).drawString(line, x, currentY, ColorUtils.interpolateColor2(new Color(-1).darker(), new Color(-1), (float) input.getOutput()));
            currentY += Fonts.interRegular.get(15).getHeight();
        }
    }

    private void deleteLastCharacter() {
        if (!text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
            setting.setText(text);
        }
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}