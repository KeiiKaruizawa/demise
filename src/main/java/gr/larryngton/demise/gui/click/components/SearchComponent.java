package gr.larryngton.demise.gui.click.components;

import gr.larryngton.demise.gui.click.IComponent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;
import lombok.Getter;
import lombok.Setter;
import org.lwjglx.input.Keyboard;

import java.awt.*;

// todo friend category using this
@Getter
@Setter
public class SearchComponent implements IComponent {
    private float x;
    private float y;
    private float width;
    private String filter = "";
    private boolean inputting;

    public SearchComponent(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return MouseUtils.isHovered(x, y, width, 20, mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        String thing = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");
        String text = !filter.isEmpty() ? filter : "Search...";
        if (inputting) {
            text += thing;
        }

        RectUtils.drawRound(x, y, width, 20, 7, new Color(0, 0, 0, 100));
        drawText(text);
    }

    private void drawText(String text) {
        int color = inputting ? new Color(193, 193, 193).getRGB() : new Color(119, 119, 119, 255).getRGB();
        Fonts.interRegular.get(18).drawString(text, x + 5, y + Fonts.interRegular.get(15).getHeight() - 2, color);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        inputting = isHovered(mouseX, mouseY) && mouseButton == 0;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (inputting) {
            String lastFilter = filter;

            if (keyCode == Keyboard.KEY_BACK) {
                if (!filter.isEmpty()) {
                    filter = filter.substring(0, filter.length() - 1);
                }
            }

            if (Character.isLetterOrDigit(typedChar) || keyCode == Keyboard.KEY_SPACE) {
                filter += typedChar;
            }

            if (!lastFilter.equals(filter)) {
                SoundUtil.playSound("demise.tick");
            }
        }
    }
}
