package gr.larryngton.demise.features.modules.impl.client;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.impl.combat.AntiBot;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.ColorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.*;
import java.util.Arrays;

public class ClientSettings extends Module {
    public final TextValue username = new TextValue("Default username", "Me", this);
    public final TextValue clientUsername = new TextValue("Client username", "demise", this);
    public final BoolValue notificationSounds = new BoolValue("Notification sounds", true, this);
    public final SliderValue pingUpdateDelay = new SliderValue("Ping update delay (seconds)", 10, 1, 60, 1, this);
    private final SliderValue fadeSpeed = new SliderValue("Fade speed", 1, 1, 10, 1, this);
    private final SliderValue timePerIndex = new SliderValue("Time per index", 75L, -350L, 350L, 1L, this);
    public final ModeValue colorMode = new ModeValue("Color mode", new String[]{"Custom", "Winter", "Blend", "Mango", "Snowy sky", "Miko", "Satin", "Gothic", "Cotton candy", "Breath", "Silver", "Tuff", "Yin & Yang", "Karuizawa", "Ichinose"}, "Winter", this);
    public final ColorValue mainColor = new ColorValue("Main Color", new Color(0, 170, 255), this, () -> colorMode.is("Breath") || colorMode.is("Custom"));
    public final ColorValue secondaryColor = new ColorValue("Secondary Color", new Color(0, 170, 255), this, () -> colorMode.is("Custom"));
    public final ModeValue bgStyle = new ModeValue("Background style", new String[]{"Transparent", "Opaque", "Custom"}, "Transparent", this);
    public final ColorValue bgColor = new ColorValue("Background color", new Color(21, 21, 21), this, () -> bgStyle.is("Custom"));
    public final BoolValue chatCombine = new BoolValue("Chat combine", true, this);
    public final ModeValue moduleCase = new ModeValue("Casing mode", new String[]{"PascalCase", "camelCase()", "snake_case", "lowercase", "UPPERCASE", "aLtErNaTe"}, "PascalCase", this);

    public final BoolValue chatTabs = new BoolValue("Chat tabs", false, this);
    public final TextValue messageFilter = new TextValue("Filter", "[IRC]", this, chatTabs::get);
    public final TextValue messagePrefixInTab = new TextValue("Message prefix in tab", ".chat", this, chatTabs::get);

    public final ModeValue buttonStyle = new ModeValue("Menu button style", new String[]{"Custom", "Vanilla"}, "Custom", this);
    public final ModeValue background = new ModeValue("Background mode", new String[]{"Minecraft", "Static", "Shader", "Video", "Rise 5"}, "Minecraft", this);
    public final BoolValue fullTint = new BoolValue("Full tint", false, this, () -> background.is("Rise 5"));

    private final MultiBoolValue allowedTargets = new MultiBoolValue("Allowed targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Non players", true),
            new BoolValue("Teams", true),
            new BoolValue("Invisibles", false),
            new BoolValue("Dead", false)
    ), this);

    public final BoolValue funy = new BoolValue("funy", false, this);

    public boolean isValidTarget(Entity entity) {
        if (!(entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager || entity instanceof EntityPlayer || entity instanceof EntitySlime || entity instanceof EntityIronGolem)) {
            return false;
        }

        if (entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager || entity instanceof EntitySlime || entity instanceof EntityIronGolem) {
            if (!allowedTargets.isEnabled("Non players")) return false;
        }

        if (entity.isInvisible() && !allowedTargets.isEnabled("Invisibles")) return false;
        if (entity.isDead && !allowedTargets.isEnabled("Dead")) return false;

        if (entity instanceof EntityPlayer) {
            if (!allowedTargets.isEnabled("Players")) return false;
            if (getModule(AntiBot.class).isBot((EntityPlayer) entity)) return false;
            if (PlayerUtils.isInTeam(entity) && !allowedTargets.isEnabled("Teams")) return false;
        }

        return true;
    }

    public Color[] getColors() {
        Color color1;
        Color color2;

        color2 = switch (colorMode.get()) {
            case "Custom" -> {
                color1 = mainColor.get();
                yield secondaryColor.get();
            }
            case "Blend" -> {
                color1 = new Color(71, 148, 253);
                yield new Color(71, 253, 160);
            }
            case "Mango" -> {
                color1 = new Color(255, 200, 75);
                yield new Color(255, 83, 73);
            }
            case "Snowy sky" -> {
                color1 = new Color(1, 171, 179);
                yield new Color(234, 234, 234);
            }
            // ofc I had to add her
            // and ik that Miko is a real name
            // i love Yae Miko
            case "Miko" -> {
                color1 = new Color(255, 100, 100);
                yield new Color(255, 255, 255);
            }
            case "Satin" -> {
                color1 = new Color(215, 60, 67);
                yield new Color(140, 23, 39);
            }
            case "Gothic" -> {
                color1 = new Color(31, 30, 30);
                yield new Color(196, 190, 190);
            }
            case "Cotton candy" -> {
                color1 = new Color(202, 153, 221);
                yield new Color(103, 244, 253);
            }
            case "Breath" -> {
                color1 = mainColor.get();
                yield ColorUtils.darker(color1, 0.45f);
            }
            case "Silver" -> {
                color1 = new Color(255, 255, 255);
                yield new Color(145, 145, 145);
            }
            case "Tuff" -> {
                color1 = new Color(179, 140, 255);
                yield new Color(255, 179, 153);
            }
            case "Yin & Yang" -> {
                color1 = new Color(255, 255, 255);
                yield new Color(0, 0, 0);
            }
            // Its fair enough to add!
            case "Karuizawa" -> {
                color1 = new Color(248, 223, 183);
                yield new Color(212, 151, 54);
            }
            case "Ichinose" -> {
                color1 = new Color(223, 120, 163, 255);
                yield new Color(246, 211, 202);
            }
            default -> {
                color1 = Color.white;
                yield Color.white;
            }
        };

        return new Color[]{color1, color2};
    }

    public int color(int counter, int alpha) {
        Color[] colors = getColors();
        return ColorUtils.swapAlpha(ColorUtils.colorSwitch(colors[0], colors[1], 2000.0F, counter, timePerIndex.getInt(), fadeSpeed.get()).getRGB(), alpha);
    }

    public int bgColor() {
        return switch (bgStyle.get()) {
            case "Transparent" -> new Color(0, 0, 0, 120).getRGB();
            case "Opaque" -> new Color(21, 21, 21, 204).getRGB();
            case "Custom" -> new Color(bgColor.get().getRGB(), true).getRGB();
            default -> -1;
        };
    }

    public String getUsername() {
        return Demise.INSTANCE.getDiscordInfo().getName().equals("null") ? username.get() : Demise.INSTANCE.getDiscordInfo().getName();
    }

    public String applyCasing(String name) {
        return applyCasing(name, moduleCase.get());
    }

    public String applyCasing(String name, String mode) {
        return switch (mode) {
            case "camelCase()" -> toCamelCase(name);
            case "snake_case" -> toSnakeCase(name);
            case "lowercase" -> name.toLowerCase();
            case "UPPERCASE" -> name.toUpperCase();
            case "aLtErNaTe" -> toAlternatingCase(name);
            default -> name;
        };
    }

    private String toCamelCase(String name) {
        return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String toSnakeCase(String name) {
        return name.trim().replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("\\s+", "_").toLowerCase();
    }

    private String toAlternatingCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : name.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = !upper;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}