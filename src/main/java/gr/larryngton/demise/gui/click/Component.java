package gr.larryngton.demise.gui.click;

import gr.larryngton.demise.features.modules.impl.visual.Interface;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public class Component implements IComponent {
    // having the x and y be 0 on default makes you able to click at the top left of the screen and change values LOL
    private float x = -1337, y = -1337, width, height;
    private Color color = new Color(INSTANCE.getModuleManager().getModule(Interface.class).color());
    private int colorRGB = color.getRGB();
    private String description = "";

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return false;
    }

    public boolean isChild() {
        return false;
    }
}
