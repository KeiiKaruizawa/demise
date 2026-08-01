package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.LClickMouseEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.SimulatedPlayer;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

@ModuleInfo(name = "SmartClicking", description = "\"1.8 pvp is just spam\"")
public class SmartClicking extends Module {
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 3, 6, 0.1F, this);
    private final SliderValue searchRange = new SliderValue("Search range", 3.5F, 3, 6, 0.1F, this);
    private final SliderValue selfPredictionTicks = new SliderValue("Self prediction ticks", 2, 0, 10, 1, this);
    private final SliderValue targetPredictionTicks = new SliderValue("Target prediction ticks", 2, 0, 10, 1, this);
    private final BoolValue baseHurtTimeOnPing = new BoolValue("Base hurtTime on ping", false, this);
    private final DoubleSliderValue validHurtTimeRange = new DoubleSliderValue("Valid hurtTime range", 3, 3, 0, 10, 1, this);
    private final SliderValue tradeTimeoutTicks = new SliderValue("Trade timeout ticks", 5, 0, 10, 1, this);
    private final DoubleSliderValue midTradeHurtTimeRange = new DoubleSliderValue("Mid trade hurtTime range", 2, 3, -5, 10, 1, this);
    private final BoolValue forceAttackOnBackTrack = new BoolValue("Force attack on BackTrack", false, this);
    private final BoolValue counterHitSelect = new BoolValue("Counter hit select", true, this);

    private EntityLivingBase target;
    private boolean shouldClick;
    private final TimerUtil hurtTimer = new TimerUtil();
    private int simHurtTime;
    private int extraHurtTime;
    private double lastDamage;
    private int calcHurtTime;

    @EventTarget
    public void onDisable() {
        target = null;
    }

    public SmartClicking() {
        midTradeHurtTimeRange.setDescription("Negative hurtTime values will just block clicking for longer.");
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (baseHurtTimeOnPing.get()) {
            calcHurtTime = Demise.INSTANCE.getPingTracker().getTickPing();
        } else {
            calcHurtTime = 0;
        }

        if (getModule(KillAura.class).isEnabled()) {
            target = KillAura.target;
        }

        if (target != null) {
            if (PlayerUtils.getDistanceToEntityBox(target) > searchRange.get()) {
                target = null;
            }
        }

        if (target != null) {
            if (target.hurtTime == 10) {
//                if (extraHurtTime < 10) {
//                    ChatUtils.sendMessageClient("last hurtTime was " + extraHurtTime);
//                }
            }

            if (target.hurtTime == 0) {
                extraHurtTime--;
            } else {
                extraHurtTime = target.hurtTime;
            }

            shouldClick = shouldClick();
        } else {
            shouldClick = true;
        }

        setTag(String.valueOf(shouldClick));
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        target = (EntityLivingBase) e.getTargetEntity();
    }

    @EventTarget
    public void onLClickMouse(LClickMouseEvent e) {
        float f = (float) mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        float f1 = EnchantmentHelper.getModifierForCreature(mc.thePlayer.getHeldItem(), target.getCreatureAttribute());
        boolean flag = mc.thePlayer.fallDistance > 0.0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null;

        if (flag && f > 0.0F) {
            f *= 1.5F;
        }

        float damage = f + f1;

        if (!shouldClick) {
            if (damage > lastDamage) {
                setTag("true");
                lastDamage = damage;
            } else {
                e.setCancelled(true);
            }
        } else {
            lastDamage = damage;
        }
    }

    private boolean shouldClick() {
        SimulatedPlayer simPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simHurtTime = mc.thePlayer.hurtTime;

        for (int i = 0; i < selfPredictionTicks.getInt(); i++) {
            simPlayer.tick();

            if (simHurtTime > 0) {
                simHurtTime--;
            }
        }

        boolean targetHittable = target.hurtTime <= getOptimalHurtTime();

        boolean onGround = target instanceof EntityPlayer player && MoveUtils.findGround(player) < targetPredictionTicks.getInt();
//        boolean selfGround = mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer) < 1.5F;
        boolean selfHittable = PlayerUtils.hittableBy(target) && PlayerUtils.getCustomDistanceToEntityBox(target.getPositionEyes(1), mc.thePlayer) < 3 && onGround;

        if (simHurtTime <= 0) {
            simulateKB(simPlayer, target);
        }

//        double predictedTargetDist = PlayerUtils.getCustomDistanceToEntityBox(target.getPositionEyes(1), predictedSelf);

        if (counterHitSelect.get()) {
            Vec3 predictedTarget = target.getPositionVector().add(target.getMoveDeltaVector());
            double predictedSelfDist = PlayerUtils.getCustomDistanceToEntityBox(mc.thePlayer.getPositionEyes(1), predictedTarget);
            boolean falling = falling();

            if (predictedSelfDist < attackRange.get() && PlayerUtils.getDistanceToEntityBox(target) > 1 && !target.isSwingInProgress && isMovingTowards(target, mc.thePlayer) && rotDiff(target) < 45 && !falling && onGround && target.hurtTime == 0) {
                return false;
            }
        }

//        if ((predictedSelfDist <= attackRange.get() && predictedTargetDist > 3) || (falling && targetHittable)) {
//            return targetHittable;
//        }

        boolean forceOnBacktrack = forceAttackOnBackTrack.get() && BackTrack.shouldLag && getModule(BackTrack.class).isEnabled();

        if (mc.thePlayer.hurtTime != 0) {
            hurtTimer.reset();
        }

        boolean shouldMidTrade = PlayerUtils.getDistToTargetFromMouseOver(
                simPlayer.getPos().add(0, simPlayer.getEyeHeight(), 0),
                mc.thePlayer.getLook(1),
                target,
                target.getHitbox()
        ) <= attackRange.get() && mc.thePlayer.getHealth() >= PlayerUtils.getActualHealth(target) && !onGround && PlayerUtils.getDistanceToEntityBox(target) < attackRange.get() - 0.4F;

        boolean shouldReduce = !hurtTimer.hasTimeElapsed(tradeTimeoutTicks.getInt() * 50L);

        if (shouldReduce && shouldMidTrade) {
            int ticksForGround = 5;

            SimulatedPlayer simPlayer1 = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
            for (int i = 0; i < 5; i++) {
                simPlayer1.tick();

                if (simPlayer1.onGround) {
                    ticksForGround = i + 1;
                    break;
                }
            }

            int midTradeHurtTime = (int) MathUtils.interpolateNoUpdateCheck(midTradeHurtTimeRange.getStart(), midTradeHurtTimeRange.getEnd(), (float) ticksForGround / 5);

            int extraHurtTime = this.extraHurtTime - calcHurtTime;
            if (extraHurtTime > midTradeHurtTime && extraHurtTime <= validHurtTimeRange.getEnd()) {
                ChatUtils.sendMessageClient("delayed hit");
                return false;
            }
        }

        boolean targetOnFire = !target.isPotionActive(Potion.fireResistance) && target.isBurning();

        return targetHittable || shouldReduce || selfHittable || forceOnBacktrack || targetOnFire;
    }

    private void simulateKB(SimulatedPlayer simPlayer, EntityLivingBase attacker) {
        int i = 0;
        i = i + EnchantmentHelper.getKnockbackModifier(attacker);

        if (attacker.isSprinting()) {
            ++i;
        }

        simPlayer.motionX += -MathHelper.sin(attacker.rotationYawHead * (float) Math.PI / 180.0F) * (float) i * 0.5F;
        simPlayer.motionY += 0.1D;
        simPlayer.motionZ += MathHelper.cos(attacker.rotationYawHead * (float) Math.PI / 180.0F) * (float) i * 0.5F;

        simHurtTime = 10;
    }

    //todo
    private int getOptimalHurtTime() {
        int hurtTime = (int) validHurtTimeRange.getStart() + calcHurtTime;

//        if (mc.objectMouseOver.entityHit == target && mc.thePlayer.hurtTime == 0 && Range.between(attackRange.get() - 0.5, (double) attackRange.get()).contains(PlayerUtils.getDistanceToEntityBox(target))) {
//            if (PlayerUtils.getDistToTargetFromMouseOver(
//                    MoveUtils.predictPositionLinear(mc.thePlayer, 1.6).add(0, mc.thePlayer.getEyeHeight(), 0),
//                    mc.thePlayer.getLook(1),
//                    target,
//                    target.getHitbox().offset(
//                            MoveUtils.getPredictedHurtPos(target,
//                                    mc.thePlayer,
//                                    1).subtract(
//                                    target.getPositionVector()
//                            )
//                    )) > 3F) {
//
//                hurtTime = (int) validHurtTimeRange.getStart();
//            }
//        }

        return hurtTime;
    }

    private float rotDiff(EntityLivingBase target) {
        float[] targetRot = RotationUtils.getRotations(mc.thePlayer.getHitbox().getCenter(), target.getPositionEyes(1));
        return (float) Math.hypot(Math.abs(RotationUtils.getAngleDifference(targetRot[0], target.rotationYawHead)), Math.abs(targetRot[1] - target.rotationPitchHead));
    }

    private boolean falling() {
        return mc.thePlayer.fallDistance > 0 || PlayerUtils.simulatePlayer(1).fallDistance > 0;
    }

    private boolean isMovingTowards(EntityLivingBase source, EntityLivingBase target) {
        Vec3 currentPos = source.getPositionVector();
        Vec3 lastPos = new Vec3(source.lastTickPosX, source.lastTickPosY, source.lastTickPosZ);
        Vec3 targetPos = target.getPositionVector();

        double mx = currentPos.xCoord - lastPos.xCoord;
        double mz = currentPos.zCoord - lastPos.zCoord;
        double movementLength = Math.sqrt(mx * mx + mz * mz);

        if (movementLength == 0.0) {
            return false;
        }

        mx /= movementLength;
        mz /= movementLength;

        double tx = targetPos.xCoord - currentPos.xCoord;
        double tz = targetPos.zCoord - currentPos.zCoord;
        double targetLength = Math.sqrt(tx * tx + tz * tz);

        if (targetLength == 0.0) {
            return false;
        }

        tx /= targetLength;
        tz /= targetLength;

        double dotProduct = mx * tx + mz * tz;
        return dotProduct >= Math.cos(Math.toRadians(60.0));
    }
}
