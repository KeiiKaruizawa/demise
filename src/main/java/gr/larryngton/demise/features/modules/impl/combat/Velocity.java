package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.BlockAABBEvent;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.TickEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.HitSlowDownEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.packet.PacketUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.MovementCorrectionMode;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.*;
import org.apache.commons.lang3.Range;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "Velocity", description = "Minimises or removes knockback.")
public class Velocity extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Cancel", "Reduce", "Legit", "GrimC07", "Intave", "Collision", "ReStrafe", "Ground", "Delay", "Freeze", "Grim spoof", "Polar", "Glitch"}, "Normal", this);

    public final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Tick Reduce", "Reduce"}, "Reduce", this, () -> mode.is("Intave"));

    private final SliderValue horizontal = new SliderValue("Horizontal", 0, 0, 100, 1, this, () -> mode.is("Normal"));
    private final SliderValue vertical = new SliderValue("Vertical", 100, 0, 100, 1, this, () -> mode.is("Normal") || mode.is("ReStrafe"));

    private final BoolValue directionReduce = new BoolValue("Direction reduce", false, this, () -> mode.is("Legit"));
    private final BoolValue patternFill = new BoolValue("Click pattern fill", false, this, () -> mode.is("Legit"));
    private final SliderValue maxQueuedClicks = new SliderValue("Max queued clicks", 5, 1, 10, 1, this, () -> mode.is("Legit") && patternFill.get());
    private final SliderValue clickAdditionRate = new SliderValue("Click addition rate", 90, 1, 100, 1, this, () -> mode.is("Legit") && patternFill.get());
    private final BoolValue singleTickAttack = new BoolValue("Single tick attack", false, this, () -> mode.is("Legit"));

    private final DoubleSliderValue hurtTime = new DoubleSliderValue("HurtTime", 9, 10, 1, 10, 1, this, () -> (mode.is("Intave") && intaveMode.is("Tick Reduce")) || (mode.is("Legit") && singleTickAttack.get()));
    private final DoubleSliderValue factor = new DoubleSliderValue("Factor", 0.6f, 0.6f, 0, 1, 0.01f, this, () -> mode.is("Reduce") || (mode.is("Intave") && !intaveMode.is("Test")));

    private final DoubleSliderValue attacks = new DoubleSliderValue("Attacks", 3, 5, 1, 20, 1, this, () -> mode.is("Legit") && singleTickAttack.get());

    private final SliderValue ticks = new SliderValue("Ticks", 0, 0, 6, 1, this, () -> mode.is("ReStrafe"));

    private final BoolValue motionGround = new BoolValue("Motion ground", false, this, () -> mode.is("Ground"));

    private final SliderValue freezeTime = new SliderValue("Freeze time", 250, 1, 500, 1, this, () -> mode.is("Freeze"));

    private final SliderValue maxDelayTicks = new SliderValue("Max delay ticks", 3, 1, 20, 1, this, () -> mode.is("Delay"));

    private final ModeValue polarMode = new ModeValue("Polar mode", new String[]{"Reduce", "Cancel"}, "Reduce", this, () -> mode.is("Polar"));

    public Velocity() {
        motionGround.setDescription("Also sets the server's ground state to true when taking knockback.");
        directionReduce.setDescription("Only works with KillAura.");
        polarMode.setDescription("Cancel works best when polar's cloud checks are down.");
    }

    private boolean velocity;
    private int queuedClicks;
    private Vec3 realPos = Vec3.ZERO;
    private boolean setPos;
    private final TimerUtil freezeTimer = new TimerUtil();
    private double motionX;
    private double motionY;
    private double motionZ;
    private double x;
    private double y;
    private double z;
    private boolean released = true;
    private float velocityYaw;
    private final ArrayList<Packet<?>> packets = new ArrayList<>();
    private boolean processing;
    private int delayingTicks;
    private final Queue<Short> transactionQueue = new ConcurrentLinkedQueue<>();
    private boolean grimPacket;
    private double violations;
    private int nextHurtTime;
    private int nextAttacks;

    @Override
    public void onEnable() {
        released = true;
        x = mc.thePlayer.posX;
        y = mc.thePlayer.posY;
        z = mc.thePlayer.posZ;
        motionX = mc.thePlayer.motionX;
        motionY = mc.thePlayer.motionY;
        motionZ = mc.thePlayer.motionZ;
        delayingTicks = 0;
        realPos = Vec3.ZERO;
    }

    @Override
    public void onDisable() {
        realPos = null;
        packets.clear();
        transactionQueue.clear();
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(mode.get());

        if (e.isPost())
            return;

        switch (mode.get()) {
            case "GrimC07":
                if (velocity) {
                    PacketUtils.sendPacket(
                            new C07PacketPlayerDigging((mc.objectMouseOver != null && mc.thePlayer.isSwingInProgress && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? C07PacketPlayerDigging.Action.START_DESTROY_BLOCK : C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK), new BlockPos(mc.thePlayer), EnumFacing.UP)
                    );

                    velocity = false;
                }
                break;
            case "ReStrafe":
                if (mc.thePlayer.hurtTime == 9 - ticks.get()) {
                    if (MoveUtils.isMoving()) {
                        MoveUtils.strafe();
                    } else {
                        mc.thePlayer.motionZ *= -1;
                        mc.thePlayer.motionX *= -1;
                    }

                    if (vertical.get() != 100.0D) {
                        mc.thePlayer.motionY *= vertical.get() / 100.0D;
                    }
                }
                break;
            case "Ground":
                if (velocity) {
                    mc.thePlayer.onGround = true;
                    if (motionGround.get()) {
                        e.setOnGround(true);
                    }
                    velocity = false;
                }
                break;
        }
    }

    @EventTarget
    public void onPostMotion(MotionEvent e) {
        if (mode.is("Delay") && e.isPost()) {
            if (velocity) {
                if (mc.thePlayer.onGround || mc.thePlayer.offGroundTicks > 12 || delayingTicks >= maxDelayTicks.get()) {
                    processing = true;
                    packets.forEach(packet -> mc.getNetHandler().addToReceiveQueue(packet));
                    packets.clear();
                    processing = false;
                    velocity = false;
                    setPos = false;
                    delayingTicks = 0;
                } else {
                    if (!setPos) {
                        realPos = mc.thePlayer.getPositionVector();
                        setPos = true;
                    }

                    delayingTicks++;
                }
            } else {
                setPos = false;
            }
        }
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (mode.is("Freeze")) {
            if (!freezeTimer.hasTimeElapsed(freezeTime.getInt())) {
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;
                mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                released = false;
            } else {
                if (!released) {
                    mc.thePlayer.motionX = motionX;
                    mc.thePlayer.motionY = motionY;
                    mc.thePlayer.motionZ = motionZ;
                    mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                    released = true;
                }

                x = mc.thePlayer.posX;
                y = mc.thePlayer.posY;
                z = mc.thePlayer.posZ;
                motionX = mc.thePlayer.motionX;
                motionY = mc.thePlayer.motionY;
                motionZ = mc.thePlayer.motionZ;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (mode.is("Delay") && setPos && mc.gameSettings.thirdPersonView != 0) {
            double x = realPos.xCoord - mc.getRenderManager().viewerPosX;
            double y = realPos.yCoord - mc.getRenderManager().viewerPosY;
            double z = realPos.zCoord - mc.getRenderManager().viewerPosZ;

            GlStateManager.pushMatrix();
            GL11.glPushAttrib(GL_ALL_ATTRIB_BITS);
            float lightLevel = mc.theWorld.getLight(new BlockPos(mc.thePlayer.getPositionVector()));
            GlStateManager.color(lightLevel, lightLevel, lightLevel);
            mc.getRenderManager().doRenderEntity(mc.thePlayer, x, y, z, mc.thePlayer.rotationYawHead, e.partialTicks(), true, true);
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    }

    @EventTarget
    public void onTick(TickEvent e) {
        switch (mode.get()) {
            case "Legit":
                if (patternFill.get()) {
                    if (velocity && queuedClicks < maxQueuedClicks.get()) {
                        for (int i = 0; i < Math.min(10, ClickHandler.getClickPattern().size()); i++) {
                            if (queuedClicks >= maxQueuedClicks.get()) {
                                break;
                            }

                            Integer click = ClickHandler.getClickPattern().get(i);

                            if (click == 0 && rand.nextInt(100) <= clickAdditionRate.getInt()) {
                                ClickHandler.getClickPattern().set(i, 1);
                                queuedClicks++;
                            }
                        }

                        if (queuedClicks != 0) {
                            ChatUtils.sendMessageClient("Queued " + queuedClicks + " " + (queuedClicks == 1 ? "click" : "clicks"));
                        }
                    } else {
                        queuedClicks = 0;
                    }
                }

                if (singleTickAttack.get()) {
                    if (mc.thePlayer.hurtTime == nextHurtTime && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && !mc.thePlayer.isUsingItem()) {
                        for (int i = 0; i < nextAttacks; i++) {
                            mc.clickMouse();
                        }

                        ChatUtils.sendMessageClient("Reduced (" + nextAttacks + " attacks)");
                    }
                }

                velocity = false;
                break;
            case "Grim spoof":
                if (transactionQueue.isEmpty() && grimPacket) grimPacket = false;
                break;
        }
    }

    @EventTarget
    @EventPriority(-100)
    public void onHighPriorityMoveInput(MoveInputEvent e) {
        if (mode.is("Legit") && directionReduce.get() && mc.thePlayer.hurtTime != 0 && RotationHandler.shouldRotate() && PlayerUtils.getTarget(5) != null) {
            if (RotationHandler.correction == MovementCorrectionMode.Silent) {
                RotationHandler.directionYaw = velocityYaw;
            } else if (RotationHandler.correction == MovementCorrectionMode.None) {
                MoveUtils.fixMovement(e, mc.thePlayer.rotationYaw, velocityYaw);
            }
        }
    }

    @EventTarget
    public void onBlockAABB(BlockAABBEvent e) {
        if (mode.is("Collision")) {
            if (e.getBlock() instanceof BlockAir && mc.thePlayer.hurtTime >= 1) {
                double x = e.getBlockPos().getX(), y = e.getBlockPos().getY(), z = e.getBlockPos().getZ();

                if (y == Math.floor(mc.thePlayer.posY) + 1) {
                    e.setBoundingBox(AxisAlignedBB.fromBounds(0, 0, 0, 1, 0, 1).offset(x, y, z));
                }
            }
        }
    }

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING && e.getPacket() instanceof S12PacketEntityVelocity s12 && s12.getEntityID() == mc.thePlayer.getEntityId()) {
            switch (mode.get()) {
                case "Normal":
                    if (horizontal.get() != 100) {
                        if (horizontal.get() != 0) {
                            s12.motionX *= (int) (horizontal.get() / 100);
                            s12.motionZ *= (int) (horizontal.get() / 100);
                        } else {
                            s12.motionX = (int) (mc.thePlayer.motionX * 8000);
                            s12.motionZ = (int) (mc.thePlayer.motionZ * 8000);
                        }
                    }

                    if (vertical.get() != 100) {
                        if (vertical.get() != 0) {
                            s12.motionY *= (int) (vertical.get() / 100);
                        } else {
                            s12.motionY = (int) (mc.thePlayer.motionY * 8000);
                        }
                    }
                    break;
                case "Cancel":
                    e.setCancelled(true);
                    break;
                case "GrimC07":
                    e.setCancelled(true);
                case "Ground":
                    velocity = true;
                    break;
                case "Delay":
                    if (!processing) {
                        velocity = true;
                        e.setCancelled(true);
                        packets.add(s12);
                    }
                    break;
                case "Freeze":
                    freezeTimer.reset();
                    break;
                case "Legit":
                    double velX = s12.getMotionX() / 8000.0D;
                    double velZ = s12.getMotionZ() / 8000.0D;
                    velocityYaw = (float) MathHelper.wrapAngleTo180_double(Math.toDegrees(Math.atan2(velZ, velX)) + 90);
                    velocity = true;

                    if (singleTickAttack.get()) {
                        nextHurtTime = (int) hurtTime.random();
                        nextAttacks = (int) attacks.random();
                    }
                    break;
                case "Glitch":
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY) - (Math.floor(mc.thePlayer.posY) - MoveUtils.findGround(mc.thePlayer)) + MathUtils.randomizeDouble(0, 0.1), mc.thePlayer.posZ);
                    }
                    break;
                case "Polar":
                    if (polarMode.is("Cancel")) {
                        EntityLivingBase target = PlayerUtils.getTarget(6);

                        if (target != null &&
                                PlayerUtils.getDistToTargetFromMouseOver(
                                        MoveUtils.getPredictedHurtPos(mc.thePlayer, target, 1),
                                        mc.thePlayer.getLook(1),
                                        target,
                                        target.getHitbox().offset(target.getMoveDeltaVector())
                                ) > 2.9F &&
                                PlayerUtils.getDistanceToEntityBox(target) > 1 &&
                                violations < 1
                        ) {
                            e.setCancelled(true);
                            violations++;
                        } else {
                            violations = Math.max(0, violations - 0.1);
                        }
                    }
                    break;
            }
        }

        switch (mode.get()) {
            case "Delay":
                if (!processing && e.getState() == PacketEvent.State.INCOMING) {
                    if (e.getPacket() instanceof S32PacketConfirmTransaction transaction) {
                        if (velocity) {
                            packets.add(transaction);
                            e.setCancelled(true);
                        }
                    }
                    if (e.getPacket() instanceof S00PacketKeepAlive keepAlive) {
                        if (velocity) {
                            if (mc.thePlayer.ticksExisted % 3 == 0) {
                                packets.add(keepAlive);
                            }

                            e.setCancelled(true);
                        }
                    }
                }
                break;
            case "Freeze":
                if (e.getState() == PacketEvent.State.OUTGOING && mode.is("Freeze") && !freezeTimer.hasTimeElapsed(freezeTime.get())) {
                    if (e.getPacket() instanceof C03PacketPlayer || e.getPacket() instanceof C0APacketAnimation || e.getPacket() instanceof C02PacketUseEntity) {
                        e.setCancelled(true);
                    }
                }
                break;
            case "Grim spoof":
                if (mc.thePlayer.ticksExisted > 20) {
                    if (e.getState() == PacketEvent.State.INCOMING) {
                        if (e.getPacket() instanceof S12PacketEntityVelocity packetEntityVelocity && packetEntityVelocity.getEntityID() == mc.thePlayer.getEntityId()) {
                            e.setCancelled(true);
                            grimPacket = true;
                        } else if (e.getPacket() instanceof S32PacketConfirmTransaction packetConfirmTransaction) {
                            if (!grimPacket) return;
                            e.setCancelled(true);
                            transactionQueue.add(packetConfirmTransaction.getActionNumber());
                        }
                    } else {
                        if (e.getPacket() instanceof C0FPacketConfirmTransaction packetConfirmTransaction) {
                            if (!grimPacket || transactionQueue.isEmpty()) return;
                            if (transactionQueue.remove(packetConfirmTransaction.getUid()))
                                e.setCancelled(true);
                        }
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        switch (mode.get()) {
            case "Intave":
                if (intaveMode.is("Tick Reduce")) {
                    if (Range.between((int) hurtTime.getStart(), (int) hurtTime.getEnd()).contains(mc.thePlayer.hurtTime)) {
                        double factor = this.factor.random();
                        mc.thePlayer.motionX *= factor;
                        mc.thePlayer.motionZ *= factor;
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onHitSlowDown(HitSlowDownEvent e) {
        if (mc.thePlayer.hurtTime != 0) {
            switch (mode.get()) {
                case "Reduce" -> e.setSlowDown(this.factor.random());
                case "Polar" -> {
                    if (polarMode.is("Reduce")) {
                        e.setSlowDown(0.59928D);
                    }
                }
            }
        }
    }
}
