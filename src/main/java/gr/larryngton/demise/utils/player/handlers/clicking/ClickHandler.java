package gr.larryngton.demise.utils.player.handlers.clicking;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.fixes.AttackOrder;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.MouseStateUpdateEvent;
import gr.larryngton.demise.events.impl.misc.StaticTickEvent;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.modules.impl.misc.ViaMCPPlus;
import gr.larryngton.demise.features.modules.impl.player.Scaffold;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.packet.PacketUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClickHandler implements InstanceAccess {
    private static boolean rayTrace;
    private static float cps;
    private static float attackRange;
    private static boolean ignoreItemUse;
    private static EntityLivingBase target;
    @Getter
    private static boolean initialized;
    private static boolean failSwing;
    @Setter
    private static ClickMode clickMode = ClickMode.PlayerController;
    @Getter
    private static List<Integer> clickPattern = new LinkedList<>();
    private static float swingRange;
    public static boolean clickingNow;
    private int cachedClicks;
    private static boolean respectHitDelay;
    private static int maxDelay;
    private static boolean doubleClick;
    private static int maxSkipsInARow;
    private double lastConcentration;
    private static int maxConcentrationDiff;
    private static int lastCPS;
    private static float consistency;
    private static float maxClickAmount;
    private static int leftClickCounter;
    private int cycles;
    private static double perlinRandAdd;

    public enum ClickMode {
        Legit,
        Packet,
        PlayerController
    }

    public static void initHandler(float cps,
                                   double perlinRandAdd,
                                   boolean rayTrace,
                                   boolean ignoreItemUse,
                                   boolean failSwing,
                                   boolean respectHitDelay,
                                   int maxDelay,
                                   float attackRange,
                                   float swingRange,
                                   boolean doubleClick,
                                   float maxClickAmount,
                                   int maxSkipsInARow,
                                   int maxConcentrationDiff,
                                   float consistency,
                                   ClickMode clickMode,
                                   EntityLivingBase target) {
        ClickHandler.cps = cps;
        ClickHandler.rayTrace = rayTrace;
        ClickHandler.attackRange = attackRange;
        ClickHandler.target = target;
        ClickHandler.ignoreItemUse = ignoreItemUse;
        ClickHandler.failSwing = failSwing;
        ClickHandler.clickMode = clickMode;
        ClickHandler.swingRange = swingRange;
        ClickHandler.respectHitDelay = respectHitDelay;
        ClickHandler.maxDelay = maxDelay;
        ClickHandler.doubleClick = doubleClick;
        ClickHandler.maxClickAmount = maxClickAmount;
        ClickHandler.maxSkipsInARow = maxSkipsInARow;
        ClickHandler.maxConcentrationDiff = maxConcentrationDiff;
        ClickHandler.consistency = consistency;
        ClickHandler.perlinRandAdd = perlinRandAdd;

        initialized = true;
    }

    private void generateBasePattern(double clicks, int ticks) {
        clickPattern.clear();

        for (int i = 0; i < ticks; i++) {
            clickPattern.add(0);
        }

        if (cycles == 10) {
            perlin.setSeed(rand.nextGaussian() * 255);
            cycles = 0;
        }

        clicks += perlin.noise(mc.thePlayer.ticksExisted) * perlinRandAdd;

        cycles++;

        int cycles = (int) Math.ceil(clicks / ticks);

        for (int j = 0; j < cycles; j++) {
            List<Integer> cyclePattern = new ArrayList<>();

            for (int i = 0; i < ticks; i++) {
                cyclePattern.add(0);
            }

            List<Integer> randomSlots = new ArrayList<>();
            double normClicks = Math.min(clicks, 20);
            double clicksToDistribute = normClicks;

            for (int i = 0; i < ticks && clicksToDistribute > 0; i++) {
                double probability = clicksToDistribute / (ticks - i);
                if (ThreadLocalRandom.current().nextDouble() < probability) {
                    randomSlots.add(i);
                    clicksToDistribute--;
                }
            }

            double interval = (double) ticks / normClicks;

            for (int i = 0; i < randomSlots.size(); i++) {
                double blended = randomSlots.get(i) * (1.0 - consistency) + (int) Math.round(i * interval) * consistency;

                int index = (int) Math.round(blended);
                index = Math.max(0, Math.min(ticks - 1, index));

                int attempts = 0;
                while (cyclePattern.get(index) > 0 && attempts < ticks) {
                    index = (index + 1) % ticks;
                    attempts++;
                }

                cyclePattern.set(index, cyclePattern.get(index) + 1);
            }

            for (int i = 0; i < ticks; i++) {
                clickPattern.set(i, clickPattern.get(i) + cyclePattern.get(i));
            }

            clicks -= 20;
        }

        if (doubleClick) {
            for (int j = 0; j < maxClickAmount; j++) {
                int from = ThreadLocalRandom.current().nextInt(ticks);
                if (clickPattern.get(from) == 0) continue;

                int to = (int) Math.min(ticks - 1, Math.max(0, from + MathUtils.randomizeAround(2)));
                if (from == to) continue;

                clickPattern.set(from, clickPattern.get(from) - 1);
                clickPattern.set(to, clickPattern.get(to) + 1);
            }
        }
    }

    private void generateClickPattern() {
        generateBasePattern(cps, 20);

        int skipsInARow = 0;

        for (int i = 0; i < clickPattern.size(); i++) {
            int click = clickPattern.get(i);

            if (click == 0) {
                skipsInARow++;
            } else {
                skipsInARow = 0;
            }

            if (skipsInARow > maxSkipsInARow) {
                if (i + 1 < clickPattern.size() - 1) {
                    int nextClick = clickPattern.get(i + 1);

                    if (nextClick != 0) {
                        clickPattern.set(i, nextClick);
                        clickPattern.set(i + 1, 0);
                    }
                }
            }
        }

        int concentration = maxRunLength();

        if (cps == lastCPS) {
            int i = 0;
            List<Integer> originalPattern = clickPattern;

            while (Math.abs(concentration - lastConcentration) > maxConcentrationDiff && i < 10) {
                ChatUtils.sendMessageClient("regenerating pattern (concentration delta is " + Math.abs(concentration - lastConcentration) + ")");

                generateBasePattern(cps, 20);
                concentration = maxRunLength();
                i++;
            }
            if (i >= 10) {
                clickPattern = originalPattern;
                ChatUtils.sendMessageClient("average concentration handler timed out");
            }
        }

        lastConcentration = concentration;
        lastCPS = (int) cps;
    }

    private double standardDeviation(List<Integer> sd) {
        double sum = 0, newSum = 0;
        for (Number v : sd) {
            sum = sum + v.doubleValue();
        }
        double mean = sum / sd.size();
        for (Number v : sd) {
            newSum = newSum + (v.doubleValue() - mean) * (v.doubleValue() - mean);
        }
        return Math.sqrt(newSum / sd.size());
    }

    private int maxRunLength() {
        int maxRun = 1;
        int currentRun = 1;

        for (int i = 1; i < clickPattern.size(); i++) {
            if (clickPattern.get(i).equals(clickPattern.get(i - 1))) {
                currentRun++;
                maxRun = Math.max(maxRun, currentRun);
            } else {
                currentRun = 1;
            }
        }
        return maxRun;
    }

    @EventTarget
    public void onStaticTick(StaticTickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (target != null && initialized && !Demise.INSTANCE.getModuleManager().getModule(Scaffold.class).isEnabled() && mc.currentScreen == null) {
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
    public void onMouseStateUpdate(MouseStateUpdateEvent e) {
        leftClickCounter = Math.max(0, leftClickCounter - 1);
        clickingNow = false;
        finalizeHandler();
    }

    private void finalizeHandler() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (target != null && initialized) {
            ViaMCPPlus viaMCPPlus = Demise.INSTANCE.getModuleManager().getModule(ViaMCPPlus.class);
            if (viaMCPPlus.isEnabled() && viaMCPPlus.attackCooldown.get()) {
                if (ViaMCPPlus.attackStrengthTicker < PlayerUtils.getItemCoolDown()) {
                    cachedClicks = 0;
                    initialized = false;
                    return;
                }
            }

            double distance = PlayerUtils.getDistanceToEntityBox(target);
            double predDistance = PlayerUtils.getCustomDistanceToEntityBox(mc.thePlayer.getPositionEyes(1).add(mc.thePlayer.getMoveDeltaVector()), target.getPositionVector().add(target.getMoveDeltaVector()));

            boolean rayTraceFailed = rayTraceFailed();
            boolean shouldAttack = distance <= attackRange && !rayTraceFailed;
            boolean willAttack = hasHitDelayPassed() && shouldAttack && cachedClicks > 0;

            KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);
            if (killAura.isEnabled()) {
                willAttack = killAura.alwaysPreAttack(willAttack);
            }

            if (cachedClicks > 0 && willAttack) {
                boolean shouldFailSwing = failSwing && predDistance <= swingRange && (distance > attackRange || rayTraceFailed);

                if (killAura.isEnabled()) {
                    killAura.preAttack();
                }

                if (hasHitDelayPassed()) {
                    for (int i = 0; i < cachedClicks; i++) {
                        if (shouldAttack) {
                            attack();
                        } else if (shouldFailSwing) {
                            handleFailSwing();
                        }
                    }

                    clickingNow = shouldAttack || shouldFailSwing;
                }

                if (killAura.isEnabled()) {
                    killAura.postAttack();
                }
            } else if (mc.currentScreen == null && mc.inGameHasFocus) {
                leftClickCounter = 0;
            }

            cachedClicks = 0;

            initialized = false;
        } else {
            clickingNow = false;
        }
    }

    private static boolean rayTraceFailed() {
        if (!rayTrace) {
            return false;
        }

        return mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY;
    }

    private static boolean hasHitDelayPassed() {
        return !respectHitDelay || leftClickCounter <= 0;
    }

    private void handleFailSwing() {
        if (itemUseAttack() && mc.currentScreen == null) {
            switch (clickMode) {
                case Packet, PlayerController:
                    AttackOrder.sendConditionalSwing(mc.objectMouseOver);
                    break;
                case Legit:
                    mc.clickMouse();
                    break;
            }
        }

        leftClickCounter = maxDelay;
    }

    public static void attack() {
        if (itemUseAttack() && mc.currentScreen == null) {
            switch (clickMode) {
                case Legit:
                    mc.clickMouse();
                    break;
                case Packet:
                    if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        mc.thePlayer.swingItem();
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                    } else {
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                        mc.thePlayer.swingItem();
                    }
                    Demise.INSTANCE.getEventManager().call(new AttackEvent(target));
                    break;
                case PlayerController:
                    AttackOrder.sendFixedAttack(mc.thePlayer, target);
                    break;
            }
        }
    }

    private static boolean lastUsingItem;
    private static int releaseTicks = 0;

    private static boolean itemUseAttack() {
        if (ignoreItemUse) {
            return true;
        }

        KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);
        boolean staysBlockedThroughAttack = killAura.isEnabled() && (killAura.autoBlockMode.is("Force") || killAura.autoBlockMode.is("NCP"));

        boolean usingItem = mc.thePlayer.isUsingItem() || (KillAura.isRealBlocking && !staysBlockedThroughAttack);
        boolean released = !usingItem && lastUsingItem;
        lastUsingItem = usingItem;

        if (released) {
            releaseTicks = 2;
        }

        if (releaseTicks > 0) {
            releaseTicks--;
            return false;
        }

        return !usingItem;
    }
}