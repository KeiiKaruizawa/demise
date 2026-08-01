package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.utils.misc.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.lwjglx.input.Keyboard;

import java.awt.*;

@Getter
@Setter
public class Key {
    private boolean isPressed;
    private int keyCode;
    private String name;
    private float[] pos;
    private float width;
    private float height;
    private float interpolatedWidth;
    private float interpolatedHeight;
    private float interpolatedX;
    private float interpolatedY;
    private float renderX;
    private float renderY;
    private Color color = new Color(0, 0, 0);

    public Key(float[] pos, int keyCode) {
        this.pos = pos;
        this.keyCode = keyCode;
        this.name = StringUtils.capitalizeWords(Keyboard.getKeyName(keyCode));
        this.width = 17;
        this.height = 17;
    }

    public Key(float[] pos, float width, int keyCode) {
        this.pos = pos;
        this.keyCode = keyCode;
        this.name = StringUtils.capitalizeWords(Keyboard.getKeyName(keyCode));
        this.width = width;
        this.height = 17;
    }
}