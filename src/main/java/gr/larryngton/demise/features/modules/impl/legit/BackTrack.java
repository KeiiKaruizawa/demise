package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.PreAttackEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.events.impl.render.RenderEntityEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.packet.LagUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.*;

@ModuleInfo(name = "BackTrack", description = "Delays packets for increased reach.")
public class BackTrack extends Module {
    private final BoolValue onlyWhenNeeded = new BoolValue("Only when needed", false, this);
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 0.1f, 8, 0.1f, this, onlyWhenNeeded::get);
    private final DoubleSliderValue range = new DoubleSliderValue("Range", 3, 6, 1, 8, 0.1f, this, () -> !onlyWhenNeeded.get());
    private final ModeValue rangeBase = new ModeValue("Range base", new String[]{"MouseOver", "HurtTime", "Attack"}, "MouseOver", this, onlyWhenNeeded::get);
    private final BoolValue attackTickFix = new BoolValue("Attack tick fix", false, this, onlyWhenNeeded::get);
    private final SliderValue predictionTicks = new SliderValue("Prediction ticks", 1, 0, 10, 1, this, onlyWhenNeeded::get);
    private final SliderValue hurtTimeToWork = new SliderValue("HurtTime to work", 3, 0, 10, 1, this, onlyWhenNeeded::get);
    private final BoolValue extraCheck = new BoolValue("Extra check", true, this);
    private final SliderValue ms = new SliderValue("Delay ms", 50, 0, 1000, 10, this);
    private final BoolValue extraMS = new BoolValue("Extra ms", false, this);
    private final SliderValue extraRand = new SliderValue("Extra rand", 50, 0, 500, 10, this);
    private final SliderValue delayForNextLag = new SliderValue("Delay for next lag", 0, 0, 1000, 10, this);
    private final ModeValue esp = new ModeValue("Mode", new String[]{"FakePlayer", "Box", "None"}, "Box", this);
    private final ColorValue color = new ColorValue("Color", new Color(0, 0, 0, 40), this, () -> esp.is("Box"));

    private EntityLivingBase target;
    public static Vec3 realPosition = Vec3.ZERO;
    public static Vec3 realLastPos = Vec3.ZERO;
    private Vec3 anim = Vec3.ZERO;
    public static boolean shouldLag;
    private boolean dispatched;
    private boolean outOfRange;
    private boolean attacked;
    private final TimerUtil relagTimer = new TimerUtil();
    private final TimerUtil attackTimer = new TimerUtil();
    private int nextRand;

    @EventTarget
    public void onEnable() {
        realPosition = Vec3.ZERO;
        realLastPos = Vec3.ZERO;
        anim = Vec3.ZERO;
    }

    @Override
    public void onDisable() {
        LagUtils.disable();
        LagUtils.dispatch();
        shouldLag = false;
        realPosition = null;
        realLastPos = null;
        anim = null;
        target = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(ms.getInt() + nextRand + " ms");
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) {
            ms.setMax(extraMS.get() ? 5000 : 1000);
            target = PlayerUtils.getTarget(8);

            if (target == null || mc.thePlayer.isDead) return;

            Vec3 pred = target.getPositionVector().subtract(target.getPrevPositionVector()).multiply(predictionTicks.get());

            double realDistance = PlayerUtils.getCustomDistanceToEntityBox(realPosition.add(0, target.getEyeHeight(), 0).add(target.getMoveDeltaVector().multiply(predictionTicks.get())), mc.thePlayer);
            double realDistanceNoPred = PlayerUtils.getCustomDistanceToEntityBox(realPosition.add(0, target.getEyeHeight(), 0), mc.thePlayer);
            double realDistanceToMouseOverNoPred = PlayerUtils.getDistToTargetFromMouseOver(mc.thePlayer.getPositionEyes(1), mc.thePlayer.getLook(1), target, target.getHitbox().offset(realPosition.add(0, target.getEyeHeight(), 0).subtract(target.getPositionEyes(1))));
            double clientDistance = PlayerUtils.getDistToTargetFromMouseOver(mc.thePlayer.getPositionEyes(1), mc.thePlayer.getLook(1), target, target.getHitbox().offset(target.getPositionVector().inverse()).offset(target.getPositionVector().add(pred)));

            if (clientDistance > attackRange.get()) {
                outOfRange = true;
            }

            if (outOfRange) {
                outOfRange = switch (rangeBase.get()) {
                    case "MouseOver" -> realDistance < 3;
                    case "HurtTime" -> target.hurtTime == 10;
                    default ->
                            attacked && (!attackTickFix.get() || attackTimer.hasTimeElapsed(Math.min(ms.getInt(), 100)));
                };
            }

            boolean distanceCheck = mc.thePlayer.getDistance(target.getPositionVector()) > mc.thePlayer.getDistance(target.getPrevPositionVector());
            boolean extraCheck = distanceCheck || !this.extraCheck.get();
            boolean onlyNeeded = extraCheck && ((realDistance > attackRange.get() || outOfRange) && realDistanceToMouseOverNoPred > attackRange.get()) && realDistance < 4.5 && clientDistance <= 3 && target.hurtTime <= hurtTimeToWork.getInt();
            boolean on = extraCheck && realDistance > range.getStart() && realDistance < range.getEnd();

            shouldLag = onlyWhenNeeded.get() ? onlyNeeded : on;

            if (shouldLag) {
                if (relagTimer.hasTimeElapsed(delayForNextLag.getInt())) {
                    if (realDistanceNoPred > 3 && target.hurtTime == 10) {
                        ChatUtils.sendMessageClient("Attacked from " + realDistanceNoPred + " blocks");
                    }

                    LagUtils.spoof(ms.getInt() + nextRand, true, true, true, true, false, false);
                    dispatched = false;
                }
            } else {
                if (!dispatched) {
                    LagUtils.disable();
                    LagUtils.dispatch();
                    relagTimer.reset();
                    dispatched = true;
                    nextRand = (int) MathUtils.randomizeAround(extraRand.get());
                }
            }

            attacked = false;
        }
    }

    @EventTarget
    public void onAttack(PreAttackEvent e) {
        if (e.getTargetEntity() == target && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            attacked = true;

            if (!attackTimer.hasTimeElapsed(Math.min(ms.getInt(), 100)) && attackTickFix.get() && !dispatched && onlyWhenNeeded.get()) {
                ChatUtils.sendMessageClient("Cancelled attack");
                e.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            realLastPos = realPosition;

            if (e.getPacket() instanceof S14PacketEntity s14PacketEntity) {
                if (s14PacketEntity.getEntityId() == target.getEntityId()) {
                    realPosition = realPosition.addVector(s14PacketEntity.getX() / 32.0D, s14PacketEntity.getY() / 32.0D, s14PacketEntity.getZ() / 32.0D);
                }
            } else if (e.getPacket() instanceof S18PacketEntityTeleport s18PacketEntityTeleport) {
                if (s18PacketEntityTeleport.getEntityId() == target.getEntityId()) {
                    realPosition = new Vec3(s18PacketEntityTeleport.getX() / 32D, s18PacketEntityTeleport.getY() / 32D, s18PacketEntityTeleport.getZ() / 32D);
                }

                if (s18PacketEntityTeleport.getEntityId() == mc.thePlayer.getEntityId()) {
                    dispatched = false;
                    shouldLag = false;
                }
            }
        } else {
            if (e.getPacket() instanceof C02PacketUseEntity c02 && c02.getAction().equals(C02PacketUseEntity.Action.ATTACK) && attackTickFix.get() && !dispatched && onlyWhenNeeded.get()) {
                attackTimer.reset();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && esp.is("Box") && shouldLag) {
            float x = (float) (realPosition.xCoord - mc.getRenderManager().viewerPosX);
            float y = (float) (realPosition.yCoord - mc.getRenderManager().viewerPosY);
            float z = (float) (realPosition.zCoord - mc.getRenderManager().viewerPosZ);
            anim = MathUtils.interpolate(anim, new Vec3(x, y, z));

            RenderUtils.drawAxisAlignedBB(target.getHitbox().offset(anim.subtract(target.getPositionVector())), true, false, color.get().getRGB());
        }
    }

    @EventTarget
    public void onRenderEntity(RenderEntityEvent e) {
        if (target != null && esp.is("FakePlayer") && shouldLag) {
            float x = (float) (realPosition.xCoord - mc.getRenderManager().viewerPosX);
            float y = (float) (realPosition.yCoord - mc.getRenderManager().viewerPosY);
            float z = (float) (realPosition.zCoord - mc.getRenderManager().viewerPosZ);
            anim = MathUtils.interpolate(anim, new Vec3(x, y, z));

            mc.getRenderManager().doRenderEntity(target, anim.xCoord, anim.yCoord, anim.zCoord, target.rotationYawHead, mc.timer.partialTicks, true);
        }
    }
}