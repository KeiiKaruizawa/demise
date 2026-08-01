package gr.larryngton.demise.utils;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.utils.math.PerlinNoise;
import net.minecraft.client.Minecraft;

import java.util.Random;

public interface InstanceAccess {
    Minecraft mc = Minecraft.getMinecraft();
    Demise INSTANCE = Demise.INSTANCE;
    Random rand = new Random();
    PerlinNoise perlin = new PerlinNoise();
}