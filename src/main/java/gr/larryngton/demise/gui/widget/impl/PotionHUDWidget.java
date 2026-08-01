package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import static net.minecraft.client.gui.Gui.drawTexturedModalRect;

public class PotionHUDWidget extends Widget {
    public PotionHUDWidget() {
        super("Potion hud");
    }

    @Override
    public void render() {
        if (mc.thePlayer.getActivePotionEffects().isEmpty() || setting.potionMode.is("Adjust")) {
            return;
        }

        float yPos = 0;
        float width = 0;
        for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            String amplifier = "";

            if (effect.getAmplifier() > 0) {
                if (effect.getAmplifier() < 10) {
                    amplifier = I18n.format("enchantment.level." + (effect.getAmplifier() + 1));
                } else {
                    amplifier = intToRoman(effect.getAmplifier() + 1);
                }
            }

            String name = I18n.format(potion.getName()) + " " + amplifier;
            float stringWidth = mc.fontRendererObj.getStringWidth(name) + mc.fontRendererObj.getStringWidth("§f" + Potion.getDurationString(effect));

            if (width < stringWidth)
                width = stringWidth;

            float finalY = yPos;
            mc.fontRendererObj.drawStringWithShadow(name, (int) (renderX + 23.75f), (int) (renderY + finalY + 3.75f), -1);
            mc.fontRendererObj.drawStringWithShadow("§f" + Potion.getDurationString(effect), (int) (renderX + 23.75f), (int) (renderY + finalY + mc.fontRendererObj.FONT_HEIGHT + 3.75f), -1);

            if (potion.hasStatusIcon()) {
                int statusIconIndex = potion.getStatusIconIndex();
                mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
                drawTexturedModalRect((int) (renderX + 3.75f), (int) (renderY + finalY + 3f), statusIconIndex % 8 * 18, 198 + statusIconIndex / 8 * 18, 18, 18);
            }

            yPos += (mc.fontRendererObj.FONT_HEIGHT * 2) + 3;
        }

        this.width = width + 7.5f;
        this.height = yPos;
    }

    private String intToRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        while (i < values.length && num >= 0) {
            while (values[i] <= num) {
                num -= values[i];
                stringBuilder.append(symbols[i]);
            }
            i++;
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Potion hud") && setting.potionMode.is("Default");
    }
}