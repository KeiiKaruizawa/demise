package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.misc.Opsec;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

@ModuleInfo(name = "Interface", description = "The client's interface.", legit = true)
public class Interface extends Module {
    public final MultiBoolValue elements = new MultiBoolValue("Elements", Arrays.asList(
            new BoolValue("Watermark", true),
            new BoolValue("Module list", true),
            new BoolValue("Armor", true),
            new BoolValue("Notifications", true),
            new BoolValue("Keystrokes", true),
            new BoolValue("Info", true),
            new BoolValue("Motion graph", false),
            new BoolValue("Radar", false),
            new BoolValue("Potion hud", false),
            new BoolValue("Mouse strokes", false),
            new BoolValue("Target hud", false),
            new BoolValue("Teams hud", false),
            new BoolValue("Spotify", false)
    ), this);

    public final ModeValue watermarkMode = new ModeValue("Watermark mode", new String[]{"Text", "Blue archive", "Old", "Miko", "Icarus", "Modern", "Adjust", "Rise 5", "Sigma", "Karuizawa"}, "Text", this, () -> elements.isEnabled("Watermark"));
    public final ModeValue moduleListMode = new ModeValue("Module list mode", new String[]{"New", "Old", "Icarus", "Adjust", "Rise 5", "Sigma", "Custom"}, "New", this, () -> elements.isEnabled("Module list"));
    public final BoolValue background = new BoolValue("Background", true, this, () -> moduleListMode.is("Adjust") && moduleListMode.canDisplay());
    public final BoolValue customBackground = new BoolValue("Custom background", true, this, () -> moduleListMode.is("Custom") && moduleListMode.canDisplay());
    public final ModeValue lineMode = new ModeValue("Line mode", new String[]{"None", "Line", "Outline"}, "None", this, () -> (moduleListMode.is("Custom") || (moduleListMode.is("Adjust") && background.get())) && moduleListMode.canDisplay());
    public final ColorValue backgroundColor = new ColorValue("Background Colour", new Color(21, 21, 21, 204), this, () -> moduleListMode.is("Custom") && customBackground.get() && moduleListMode.canDisplay());

    public final SliderValue customTextSize = new SliderValue("Custom text size", 15, 6, 40, 1, this, () -> moduleListMode.is("Custom") && moduleListMode.canDisplay());
    public final ModeValue customFont = new ModeValue("Custom font", new String[]{"Vanilla", "Inter Bold", "Inter Medium", "Inter Regular", "Inter Semi Bold", "Inter Light", "Urbanist", "Exhi", "Genshin", "Tahoma", "Tahoma Bold", "Comfortaa", "Helvetica Neue", "why the fuck would you use this", "Rubik Bold"}, "Inter Regular", this, () -> moduleListMode.is("Custom") && moduleListMode.canDisplay());
    public final BoolValue customShowSuffix = new BoolValue("Custom suffix", true, this, () -> moduleListMode.is("Custom") && moduleListMode.canDisplay());
    public final ColorValue customSuffixColor = new ColorValue("Suffix color", Color.lightGray, this, () -> moduleListMode.is("Custom") && customShowSuffix.get() && moduleListMode.canDisplay());
    public final ModeValue customSuffixFont = new ModeValue("Suffix font", new String[]{"Vanilla", "Inter Bold", "Inter Medium", "Inter Regular", "Inter Semi Bold", "Inter Light", "Urbanist", "Exhi", "Genshin", "Tahoma", "Tahoma Bold", "Comfortaa", "Helvetica Neue", "why the fuck would you use this", "Rubik Bold"}, "Inter Regular", this, () -> moduleListMode.is("Custom") && customShowSuffix.get() && moduleListMode.canDisplay());
    public final ModeValue customSuffixCasing = new ModeValue("Suffix casing", new String[]{"Default", "UPPERCASE", "lowercase", "aLtErNaTe"}, "Default", this, () -> moduleListMode.is("Custom") && customShowSuffix.get() && moduleListMode.canDisplay());

    public final ModeValue potionMode = new ModeValue("Potion Mode", new String[]{"Default", "Adjust"}, "Default", this, () -> elements.isEnabled("Potion hud"));
    public final ModeValue infoMode = new ModeValue("Info Mode", new String[]{"Default", "Adjust"}, "Default", this, () -> elements.isEnabled("Info"));

    public final ModeValue targetHudMode = new ModeValue("Target hud mode", new String[]{"New", "Old", "Novoline", "Exhibition", "Adjust", "Rise 5"}, "New", this, () -> elements.isEnabled("Target hud"));
    public final BoolValue trackTarget = new BoolValue("Track target", false, this, () -> elements.isEnabled("Target hud"));
    public final SliderValue interpolation = new SliderValue("Interpolation", 0.5F, 0.01F, 1, 0.01F, this, () -> elements.isEnabled("Target hud") && trackTarget.get());
    public final SliderValue offsetX = new SliderValue("Offset X", 0, 0, 250, this, () -> elements.isEnabled("Target hud") && trackTarget.get());
    public final SliderValue offsetY = new SliderValue("Offset Y", 100, -25, 250, this, () -> elements.isEnabled("Target hud") && trackTarget.get());

    private String displayName() {
        String customName = Demise.INSTANCE.getClientSettings().clientUsername.get();
        return customName.isEmpty() ? Demise.INSTANCE.getClientName() : customName;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (elements.isEnabled("Watermark")) {
            switch (watermarkMode.get()) {
                case "Text":
                    Fonts.urbanist.get(38).drawString(displayName(), 10, 10, new Color(255, 255, 255, 208).getRGB());
                    Fonts.urbanist.get(27.4F).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(displayName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27.4F).getHeight() * 1.1F, new Color(245, 245, 245, 208).getRGB());

                    Demise.INSTANCE.getShaderManager().addToShadowQueue(() -> {
                        Fonts.urbanist.get(38).drawString(displayName(), 10, 10, new Color(255, 255, 255, 208).getRGB());
                        Fonts.urbanist.get(27.4F).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(displayName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27.4F).getHeight() * 1.1F, new Color(245, 245, 245, 208).getRGB());
                    });
                    break;
                case "Blue archive":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-base.png"), 0, 0, 127, 75);
                    RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-colored.png"), 0, 0, 127, 75, color());

                    Demise.INSTANCE.getShaderManager().addToShadowQueue(() -> {
                        RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-base.png"), 0, 0, 127, 75, Color.black.getRGB());
                        RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-colored.png"), 0, 0, 127, 75, Color.black.getRGB());
                    });
                    break;
                case "Old":
                    String clientUsername = displayName();
                    String firstLetter = clientUsername.isEmpty() ? "" : clientUsername.substring(0, 1);
                    String rest = clientUsername.isEmpty() ? "" : clientUsername.substring(1);

                    mc.fontRendererObj.drawStringWithShadow(firstLetter, 5, 5, color());
                    mc.fontRendererObj.drawStringWithShadow(
                            rest + " | " +
                                    Minecraft.getDebugFPS() + "fps | " +
                                    (mc.isSingleplayer() ? "singleplayer" : PlayerUtils.getCurrServer()),
                            5 + (mc.fontRendererObj.getStringWidth(firstLetter)), 5, Color.WHITE.getRGB()
                    );
                    break;
                case "Miko":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/mikos/Miko-writing.png"), 10, 10, 90, 90);
                    break;
                case "Icarus":
                    Fonts.interBold.get(80).drawStringWithShadow(displayName(), 8, 0, -1);
                    break;
                case "Modern":
                    String srv;
                    if (!Demise.INSTANCE.getModuleManager().getModule(Opsec.class).isEnabled()) {
                        if (mc.isSingleplayer()) {
                            srv = "singleplayer";
                        } else if (mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy.net")) {
                            srv = "liquidproxy.net";
                        } else {
                            srv = mc.getCurrentServerData().serverIP;
                        }
                    } else {
                        // OPSEC METHOD
                        srv = "https://www.pixiv.net/en/users/110918912";
                    }

                    String name = displayName().toLowerCase() + EnumChatFormatting.WHITE +
                            " | " + Demise.INSTANCE.getDiscordInfo().getName() +
                            " | " + srv;

                    int x = 7;
                    int y = 7;
                    int width = Fonts.interSemiBold.get(17).getStringWidth("") + Fonts.interSemiBold.get(17).getStringWidth(name) + 5;
                    int height = Fonts.interSemiBold.get(17).getHeight() + 3;

                    RectUtils.drawRound(x, y, width, height, 4, new Color(getModule(Interface.class).bgColor(), true));
                    Fonts.interSemiBold.get(17).drawStringWithShadow(name, Fonts.interBold.get(17).getStringWidth("") + x + 2, y + 4.5f, new Color(color(1)).getRGB());
                    Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RectUtils.drawShaderRound(x, y, width, height, 4, Color.black));
                    break;
                case "Adjust":
                    String adjustName = displayName();
                    String adjustFirst = adjustName.isEmpty() ? "" : adjustName.substring(0, 1);
                    String adjustRest = adjustName.isEmpty() ? "" : adjustName.substring(1);

                    Fonts.tahomaBold.get(18).drawStringWithShadow(adjustFirst, 2.5f, 3f, getColors()[0].getRGB());
                    Fonts.tahoma.get(18).drawStringWithShadow(adjustRest, 3f + Fonts.tahomaBold.get(18).getStringWidth(adjustFirst), 3f, Color.WHITE.getRGB());
                    break;
                case "Rise 5":
                    Fonts.comfortaa.get(32).drawGradientWithShadow(displayName(), 1, 5, (i) -> new Color(color(i)));
                    Fonts.comfortaa.get(18).drawStringWithShadow("5.100", 1 + Fonts.comfortaa.get(32).getStringWidth(displayName()), 4, color());
                    break;
                case "Sigma":
                    Fonts.helveticaNeue.get(50).drawGradientWithShadow(displayName(), 1, 5, (i) -> new Color(color(i)));
                    Fonts.helveticaNeue.get(25).drawStringWithShadow("Jello", 1, 5 + Fonts.helveticaNeue.get(60).getHeight() - 8, color());
                    break;
                case "Karuizawa":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/karuizawa/karuizawa-nendoroid.png"), 10, 10, 90, 90);
                    break;
            }
        }

        //adjust
        {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            FontRenderer normalFont = Fonts.tahoma.get(15);
            FontRenderer smallFont = Fonts.tahoma.get(12);

            if (elements.isEnabled("Info") && infoMode.is("Adjust")) {
                String month = LocalDate.now().getMonth().toString().toLowerCase();
                month = month.substring(0, 1).toUpperCase() + month.substring(1);
                String username = Demise.INSTANCE.getClientSettings().getUsername();
                normalFont.drawGradientWithShadow(username, 2.5f, sr.getScaledHeight() - normalFont.getHeight() + 1f, (index) -> new Color(color(index)));
                smallFont.drawStringWithShadow(" " + month + " " + getDayWithSuffix(LocalDate.now().getDayOfMonth()), 2.5f + normalFont.getStringWidth(username), sr.getScaledHeight() - smallFont.getHeight() + 0.5f, new Color(230, 230, 230).getRGB());
                smallFont.drawStringWithShadow(String.format("%.2f", MoveUtils.getBPS()), 3f, sr.getScaledHeight() - normalFont.getHeight() - smallFont.getHeight() + 1.5f, new Color(230, 230, 230).getRGB());
                smallFont.drawStringWithShadow("B/s", 3f + smallFont.getStringWidth(String.format("%.2f", MoveUtils.getBPS())) + 1.5f, sr.getScaledHeight() - normalFont.getHeight() - smallFont.getHeight() + 0.5f, Color.LIGHT_GRAY.getRGB());
            }

            if (elements.isEnabled("Potion hud") && potionMode.is("Adjust")) {
                float offset = 0;

                for (PotionEffect effect : mc.thePlayer.getActivePotionEffects().stream().sorted(Comparator.comparingDouble(effect -> {
                            Potion potion = Potion.potionTypes[effect.getPotionID()];
                            String amplifier = "";

                            if (effect.getAmplifier() > 0) {
                                if (effect.getAmplifier() < 10) {
                                    amplifier = " " + I18n.format("enchantment.level." + (effect.getAmplifier() + 1));
                                } else {
                                    amplifier = " " + intToRoman(effect.getAmplifier() + 1);
                                }
                            }

                            return -normalFont.getStringWidth(I18n.format(potion.getName()) + amplifier + Potion.getDurationString(effect));
                        }
                )).toList()) {
                    Potion potionType = Potion.potionTypes[effect.getPotionID()];
                    String amplifier = "";

                    if (effect.getAmplifier() > 0) {
                        if (effect.getAmplifier() < 10) {
                            amplifier = " " + I18n.format("enchantment.level." + (effect.getAmplifier() + 1));
                        } else {
                            amplifier = " " + intToRoman(effect.getAmplifier() + 1);
                        }
                    }

                    String name = I18n.format(potionType.getName()) + amplifier;
                    String durationString = Potion.getDurationString(effect);
                    normalFont.drawStringWithShadow(durationString, sr.getScaledWidth() - normalFont.getStringWidth(durationString) - 3f, sr.getScaledHeight() - normalFont.getHeight() + 0.5f - offset, effect.getDuration()/20 > 12 ? Color.LIGHT_GRAY.getRGB() : new Color(255, 100, 100).getRGB());
                    normalFont.drawStringWithShadow(name, sr.getScaledWidth() - normalFont.getStringWidth(durationString) - normalFont.getStringWidth(name) - 3f - 2.5f, sr.getScaledHeight() - normalFont.getHeight() + 0.5f - offset, ColorUtils.swapAlpha(potionType.getLiquidColor(), 200));
                    offset += 8;
                }
            }
        }

        if (elements.isEnabled("Armor")) {
            ArrayList<ItemStack> stuff = new ArrayList<>();
            boolean onWater = mc.thePlayer.isEntityAlive() && mc.thePlayer.isInsideOfMaterial(Material.water);
            int split = -3;
            for (int index = 3; index >= 0; --index) {
                ItemStack armor = mc.thePlayer.inventory.armorInventory[index];
                if (armor == null) continue;
                stuff.add(armor);
            }

            if (SpoofSlotUtils.getSpoofedStack() != null) {
                stuff.add(SpoofSlotUtils.getSpoofedStack());
            }

            for (ItemStack everything : stuff) {
                split += 16;
                RenderUtils.renderItemStack(everything, split + (double) event.scaledResolution().getScaledWidth() / 2 - 4, event.scaledResolution().getScaledHeight() - (onWater ? 67 : 57) + (mc.thePlayer.capabilities.isCreativeMode ? 10 : 0), 1, true, 0.5f);
            }
        }

        if (elements.isEnabled("Notifications")) {
            Demise.INSTANCE.getNotificationManager().publish();
        }
    }

    public Color[] getColors() {
        return Demise.INSTANCE.getClientSettings().getColors();
    }

    public int color() {
        return color(0);
    }

    public int color(int counter) {
        return color(counter, 255);
    }

    public int color(int counter, int alpha) {
        return Demise.INSTANCE.getClientSettings().color(counter, alpha);
    }

    public int bgColor() {
        return Demise.INSTANCE.getClientSettings().bgColor();
    }

    public String getDayWithSuffix(int day) {
        String dayStr = String.valueOf(day);

        if (dayStr.startsWith("1") && (dayStr.endsWith("1") || dayStr.endsWith("2") || dayStr.endsWith("3")))
            return day + "th";

        return switch (dayStr.charAt(dayStr.length() - 1)) {
            case '1' -> day + "st";
            case '2' -> day + "nd";
            case '3' -> day + "rd";
            default -> day + "th";
        };
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
}