package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ColorValue;
import gr.larryngton.demise.features.values.impl.ModeValue;

import java.awt.*;

@ModuleInfo(name = "Cape", description = "Renders a cape on your player.", legit = true)
public class Cape extends Module {
    public final ModeValue capeMode = new ModeValue("Cape mode", new String[]{"Minecraft", "Rise", "Raven", "Other"}, "Minecraft", this);
    public final ModeValue riseMode = new ModeValue("Rise cape mode", new String[]{"Normal", "Red", "Green", "Blue", "Dogshit"}, "Normal", this, () -> capeMode.is("Rise"));
    public final ModeValue ravenMode = new ModeValue("Raven cape mode", new String[]{"Anime", "Aqua", "Green", "Purple", "Red", "White", "Yellow"}, "Purple", this, () -> capeMode.is("Raven"));
    public final ModeValue mcMode = new ModeValue("Minecraft cape mode",
            new String[]{
                    "15thAnniversary",
                    "Birthday",
                    "CherryBlossom",
                    "Cobalt",
                    "dB",
                    "Followers",
                    "MCC15Year",
                    "MillionthCustomer",
                    "MineCon2011",
                    "MineCon2012",
                    "MineCon2013",
                    "MineCon2015",
                    "MineCon2016",
                    "MinecraftExperience",
                    "Mojang",
                    "MojangClassic",
                    "MojangOffice",
                    "MojangStudios",
                    "MojiraMod",
                    "Prismarine",
                    "RealmsMapMaker",
                    "Scrolls",
                    "Snowman",
                    "Spade",
                    "test",
                    "Translator",
                    "TranslatorCN",
                    "TranslatorJP",
                    "Turtle",
                    "Valentine",
                    "Vanilla"
            }, "Mojang", this, () -> capeMode.is("Minecraft"));
    public final ModeValue otherMode = new ModeValue("Other mode", new String[]{"Aesthetic drive", "Polar", "Firefox", "Larp", "Novoline", "Tenacity", "BEST CLIENT", "KKE", "cp", "loli", "Grim", "Karuizawa", "Ichinose"}, "BEST CLIENT", this, () -> capeMode.is("Other"));

    public final BoolValue modernPhysics = new BoolValue("Modern cape physics", false, this);
    public final BoolValue tint = new BoolValue("Cape tint", false, this);
    public final ColorValue color = new ColorValue("Tint color", Color.white, this, tint::get);
}
