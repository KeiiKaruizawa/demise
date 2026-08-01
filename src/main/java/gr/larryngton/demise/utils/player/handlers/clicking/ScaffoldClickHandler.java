package gr.larryngton.demise.utils.player.handlers.clicking;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.StaticTickEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.impl.player.Scaffold;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import lombok.Getter;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ScaffoldClickHandler implements InstanceAccess {
    private static float minCPS;
    private static float maxCPS;
    private static boolean initialized;
    @Getter
    private static final List<Integer> clickPattern = new LinkedList<>();
    private int cachedClicks;
    private static RandomisationMode randomisationMode;
    private static boolean clickOnMiss;
    private static boolean allowWrongPlace;

    public enum RandomisationMode {
        Stabilised,
        Even,
        Jitter
    }

    public static void initHandler(float minCPS, float maxCPS, RandomisationMode randomisationMode, boolean allowWrongPlace, boolean clickOnMiss) {
        ScaffoldClickHandler.minCPS = minCPS;
        ScaffoldClickHandler.maxCPS = maxCPS;
        ScaffoldClickHandler.randomisationMode = randomisationMode;
        ScaffoldClickHandler.clickOnMiss = clickOnMiss;
        ScaffoldClickHandler.allowWrongPlace = allowWrongPlace;

        initialized = true;
    }

    private void generateClickPattern() {
        clickPattern.clear();
        int totalTicks = 20;

        double clicks = MathUtils.randomizeDouble(minCPS, maxCPS);

        for (int i = 0; i < totalTicks; i++) {
            clickPattern.add(0);
        }

        int cycles = clicks / 20 <= 1 ? 1 : 2;

        for (int j = 0; j < cycles; j++) {
            double normClicks = Math.min(clicks, 20);

            switch (randomisationMode) {
                case Stabilised -> {
                    double interval = (double) totalTicks / normClicks;
                    int remainder = (int) (totalTicks % normClicks);

                    int currentIndex = 0;

                    for (int i = 0; i < normClicks; i++) {
                        int index = currentIndex % totalTicks;
                        clickPattern.set(index, clickPattern.get(index) + 1);
                        currentIndex += (int) Math.max(interval, 1);
                        if (remainder > 0) {
                            currentIndex++;
                            remainder--;
                        }
                    }
                }
                case Even -> {
                    double interval = (double) totalTicks / normClicks;
                    for (int i = 0; i < clicks; i++) {
                        int index = (int) Math.round(i * interval) % totalTicks;
                        clickPattern.set(index, clickPattern.get(index) + 1);
                    }
                }
                case Jitter -> {
                    double clicksToDistribute = normClicks;

                    for (int i = 0; i < totalTicks; i++) {
                        double probability = clicksToDistribute / (totalTicks - i);
                        if (ThreadLocalRandom.current().nextDouble() < probability) {
                            clickPattern.set(i, clickPattern.get(i) + 1);
                            clicksToDistribute--;
                        }
                    }
                }
            }

            clicks -= 20;
        }
    }

    @EventTarget
    public void onStaticTick(StaticTickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (initialized) {
            if (clickPattern.isEmpty()) {
                generateClickPattern();
            }

            Integer next = clickPattern.get(0);
            clickPattern.remove(0);

            if (next != null) {
                cachedClicks += next;
            }
        } else {
            cachedClicks = 0;
        }
    }

    @EventTarget
    public void onTick(UpdateEvent e) {
        finalizeHandler();
    }

    private void finalizeHandler() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (initialized && mc.currentScreen == null) {
            Scaffold scaffold = Demise.INSTANCE.getModuleManager().getModule(Scaffold.class);

            boolean a = testForRightClick(scaffold.data.blockPos, scaffold.data.sideHit);
            boolean b = allowWrongPlace && MoveUtils.getSpeed() != 0;

            if (a || b) {
                for (int i = 0; i < cachedClicks; i++) {
                    mc.rightClickMouse(false);
                }

                if (a && cachedClicks != 0) {
                    Scaffold.blocksPlaced++;
                }
            } else {
                if (!testForRightClickBasic(mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit) && clickOnMiss && MoveUtils.getSpeed() != 0) {
                    for (int i = 0; i < cachedClicks; i++) {
                        mc.rightClickMouse(false);
                    }
                }
            }
        }

        cachedClicks = 0;
        initialized = false;
    }

    public static boolean testForRightClick(BlockPos hitPos, EnumFacing side) {
        ItemStack heldStack = mc.thePlayer.getHeldItem();

        if (!mc.theWorld.getWorldBorder().contains(hitPos)) {
            return false;
        } else {
            if (heldStack != null && heldStack.getItem() instanceof ItemBlock itemblock) {
                if (!itemblock.canPlaceBlockOnSide(mc.theWorld, hitPos, side, mc.thePlayer, heldStack)) {
                    return false;
                }
            }

            MovingObjectPosition ray = mc.objectMouseOver;
            return ray.typeOfHit.equals(MovingObjectPosition.MovingObjectType.BLOCK) && ray.getBlockPos().equalsBlockPos(hitPos) && ray.sideHit.equals(side);
        }
    }

    public static boolean testForRightClickBasic(BlockPos hitPos, EnumFacing side) {
        ItemStack heldStack = mc.thePlayer.getHeldItem();

        if (!mc.theWorld.getWorldBorder().contains(hitPos)) {
            return false;
        } else {
            if (heldStack != null && heldStack.getItem() instanceof ItemBlock itemblock) {
                return itemblock.canPlaceBlockOnSide(mc.theWorld, hitPos, side, mc.thePlayer, heldStack);
            }

            return true;
        }
    }
}