package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.render.RenderUtils;

import java.util.Arrays;
import java.util.List;

public class KeystrokeWidget extends Widget {
    private final Key w = new Key(new float[]{20, 0}, mc.gameSettings.keyBindForward.getKeyCode());
    private final Key a = new Key(new float[]{0, 20}, mc.gameSettings.keyBindLeft.getKeyCode());
    private final Key s = new Key(new float[]{20, 20}, mc.gameSettings.keyBindBack.getKeyCode());
    private final Key d = new Key(new float[]{40, 20}, mc.gameSettings.keyBindRight.getKeyCode());
    private final Key jump = new Key(new float[]{0, 40}, 57, mc.gameSettings.keyBindJump.getKeyCode());

    private final List<Key> keyList = Arrays.asList(w, a, s, d, jump);

    public KeystrokeWidget() {
        super("Keystrokes");
        this.width = 57;
        this.height = 57;
        this.x = 0.9342179f;
        this.y = 0.84583354f;
    }

    @Override
    public void render() {
        drawKeystrokes(false);
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> drawKeystrokes(true));
    }

    private void drawKeystrokes(boolean shader) {
        w.setPressed(mc.thePlayer.movementInput.moveForward > 0);
        a.setPressed(mc.thePlayer.movementInput.moveStrafe > 0);
        s.setPressed(mc.thePlayer.movementInput.moveForward < 0);
        d.setPressed(mc.thePlayer.movementInput.moveStrafe < 0);
        jump.setPressed(mc.thePlayer.movementInput.jump);

        RenderUtils.renderKeys(renderX, renderY, keyList, shader, 3);
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Keystrokes");
    }
}