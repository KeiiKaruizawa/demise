package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.JumpEvent;
import gr.larryngton.demise.events.impl.player.LClickMouseEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.StrafeEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.legit.AutoClicker;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.packet.BlinkComponent;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.MovementCorrectionMode;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@ModuleInfo(name = "Displace", description = "Redirects target knockback.")
public class Displace extends Module {

    private final ModeValue mode = new ModeValue("Mode", new String[]{"Left", "Right", "Random", "Void"}, "Left", this);
    private final SliderValue angle = new SliderValue("Angle", 100, 10, 180, 5, this);
    private final SliderValue cooldown = new SliderValue("Cooldown", 0.5f, 0.2f, 8.0f, 0.1f, this);
    private final BoolValue hideRotations = new BoolValue("Hide rotations", false, this);
    private final BoolValue onlyKnockbackItems = new BoolValue("Only knockback items", true, this);
    private final BoolValue overrideAttacks = new BoolValue("Override attacks", true, this);
    private final BoolValue playersOnly = new BoolValue("Players only", false, this);
    private final BoolValue renderArrow = new BoolValue("Render arrow", true, this);
    private final BoolValue manual = new BoolValue("Manual", true, this);
    private final BoolValue autoClickerIntegration = new BoolValue("AutoClicker", true, this);
    private final BoolValue killAuraIntegration = new BoolValue("KillAura", true, this);
    private final ModeValue moveCorrection = new ModeValue("Movement correction", new String[]{"Silent", "Strict", "None"}, "Silent", this);
    private static final int HITTABLE_HURT_TIME = 3;
    private static final Random RANDOM = new Random();
    private static Displace instance;

    public static boolean rotate;
    public static boolean queued;
    public static long lastAt;
    public static boolean holding;
    private static int holdTicks;
    private static int blinkTicks;
    public static int lastSide = -1;
    private static int side = -1;
    private static EntityLivingBase lastTarget;
    private static boolean arrow;
    private static long arrowAt;
    private static double arrowX, arrowY, arrowZ, dirX, dirZ;
    private static int forcedAttacks;
    private static boolean selfClick;
    private static float displacedYaw;
    private static boolean yawDisplaced;
    private static boolean auraDisplaced;

    public Displace() {
        instance = this;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (auraDisplaced && !RotationHandler.shouldRotate()) {
            auraDisplaced = false;
        }

        if (blinkTicks > 0 && --blinkTicks == 0) {
            BlinkComponent.dispatch(true);
        }

        if (killAuraIntegration.get()) {
            EntityLivingBase auraTarget = KillAura.target;
            if (auraTarget != null && canDisplace(auraTarget, true, true)) {
                trigger(2);
            }
        }

        if (holdTicks > 0) {
            holding = --holdTicks > 0;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (e.isPre()) {
            if (rotate) {
                rotate = false;
                selfClick = true;
                startBlink();
                clickAttack();
                e.setYaw(yawDisplaced ? displacedYaw : mc.thePlayer.rotationYaw + offset());
                yawDisplaced = false;
            }
        }
    }

    public static float[] displaceAuraRotation(float[] rot) {
        if (!queued) {
            return rot;
        }

        queued = false;

        EntityLivingBase target = KillAura.target;
        KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);

        if (rot != null && target != null && !KillAura.isRealBlocking && killAura != null && killAura.isEnabled()
                && rayHits(target, rot[0], rot[1], killAura.attackRange.get())) {
            rot[0] = rot[0] + offset();
            auraDisplaced = true;
            forcedAttacks++;
            startBlink();
        } else {
            clear();
        }

        return rot;
    }
    public static boolean auraAttackGate() {
        if (!isAuraIntegrationActive()) {
            return false;
        }

        if (queued) {
            return true;
        }

        if (lastAt > 0L && Math.abs(System.currentTimeMillis() - lastAt) < 60L) {
            rearm();
            return false;
        }

        if (forcedAttacks > 0) {
            --forcedAttacks;
            return false;
        }

        if (holding) {
            return true;
        }

        return instance.overrideAttacks.get() && canDisplace(KillAura.target, false, false);
    }

    public static boolean isAuraIntegrationActive() {
        return instance != null && instance.isEnabled() && instance.killAuraIntegration.get();
    }

    @EventTarget
    @EventPriority(-100)
    public void onMoveInput(MoveInputEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null || moveCorrection.is("None")) {
            return;
        }

        if (RotationHandler.shouldRotate()) {
            if (auraDisplaced) {
                RotationHandler.correction = MovementCorrectionMode.valueOf(moveCorrection.get());
            }
            return;
        }

        if (!rotate) {
            return;
        }

        if (!yawDisplaced) {
            displacedYaw = mc.thePlayer.rotationYaw + offset();
            yawDisplaced = true;
        }

        if (moveCorrection.is("Silent")) {
            MoveUtils.fixMovement(e, displacedYaw, mc.thePlayer.rotationYaw);
        }

    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        if (moveCorrection.is("None") || RotationHandler.shouldRotate()) {
            return;
        }

        if (rotate && yawDisplaced) {
            e.setYaw(displacedYaw);
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (moveCorrection.is("None") || RotationHandler.shouldRotate()) {
            return;
        }

        if (rotate && yawDisplaced) {
            e.setYaw(displacedYaw);
        }
    }


    @EventTarget
    public void onLClick(LClickMouseEvent e) {
        if (selfClick) {
            selfClick = false;
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (!autoClickerIntegration.get() && isAutoClickerClicking()) {
            return;
        }

        if (!manual.get()) {
            return;
        }

        Entity entity = null;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            entity = mc.objectMouseOver.entityHit;
        }

        if (entity == null) {
            return;
        }

        if (canDisplace(entity, true, true)) {
            e.setCancelled(true);
            trigger(0);
        } else if (overrideAttacks.get() && canDisplace(entity, false, false)) {
            e.setCancelled(true);
        }
    }
    private static boolean isAutoClickerClicking() {
        AutoClicker autoClicker = Demise.INSTANCE.getModuleManager().getModule(AutoClicker.class);
        return autoClicker != null && autoClicker.isEnabled()
                && GameSettings.isKeyDown(mc.gameSettings.keyBindAttack);
    }

    public static void trigger(int source) {
        if (source == 0) {
            rotate = true;
        } else if (source == 2) {
            queued = true;
        }
        lastAt = System.currentTimeMillis();
    }

    public static void rearm() {
        holdTicks = 3;
    }

    public static void clear() {
        lastAt = 0L;
    }

    public static void startBlink() {
        if (instance.hideRotations.get()) {
            blinkTicks = 2;
            BlinkComponent.blinking = true;
        }
    }

    public static float offset() {
        float f = instance.angle.get();
        if (side != -1) {
            if (side == 0) {
                f = -f;
            }
            side = -1;
        } else if (modeIs("Left") || modeIs("Random") && RANDOM.nextBoolean()) {
            f = -f;
        }
        f += randomFloat(-5.0f, 5.0f);
        if (instance.renderArrow.get() && lastTarget != null) {
            setArrow(lastTarget, f);
        }
        return f;
    }

    public static boolean canDisplace(Entity entity, boolean checkHurtTime, boolean checkCooldown) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }

        if (checkHurtTime && ((EntityLivingBase) entity).hurtTime > HITTABLE_HURT_TIME) {
            return false;
        }

        if (instance.playersOnly.get() && !(entity instanceof EntityPlayer)) {
            return false;
        }

        int n = EnchantmentHelper.getKnockbackModifier(mc.thePlayer);
        if (!mc.thePlayer.isSprinting() && n == 0) {
            return false;
        }
        if (instance.onlyKnockbackItems.get() && n == 0) {
            return false;
        }
        if (checkCooldown && (double) Math.abs(System.currentTimeMillis() - lastAt) < instance.cooldown.get() * 1000.0 - 20.0) {
            return false;
        }
        if (modeIs("Void") && (side = (lastSide = dropSide(entity, lastSide))) == -1) {
            return false;
        }

        lastTarget = (EntityLivingBase) entity;
        return true;
    }

    private static boolean modeIs(String m) {
        return instance.mode.is(m);
    }

    private static float randomFloat(float min, float max) {
        return (float) ((double) min + (double) (max - min) * Math.random());
    }

    private static int dropSide(Entity entity, int previous) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            return -1;
        }
        double px = -(dz /= len);
        double pz = dx /= len;
        double y = entity.posY;
        int minusSide = 0;
        int plusSide = 0;
        int behind = 0;
        for (int n4 : new int[]{1, 2, 3, 4, 6, 8}) {
            int weight = n4 < 6 ? 2 : 1;
            double bx = entity.posX + dx * (double) n4;
            double bz = entity.posZ + dz * (double) n4;
            for (int i = -1; i <= 1; ++i) {
                double x = bx + px * (double) i;
                double z = bz + pz * (double) i;
                if (mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) != null) continue;
                behind += weight;
            }
            double mx = entity.posX - px * (double) n4;
            double mz = entity.posZ - pz * (double) n4;
            for (int i = -1; i <= 1; ++i) {
                double x = mx + dx * (double) i;
                double z = mz + dz * (double) i;
                if (mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) != null) continue;
                minusSide += weight;
            }
            double qx = entity.posX + px * (double) n4;
            double qz = entity.posZ + pz * (double) n4;
            for (int i = -1; i <= 1; ++i) {
                double x = qx + dx * (double) i;
                double z = qz + dz * (double) i;
                if (mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) != null) continue;
                plusSide += weight;
            }
        }
        if (minusSide == 0 && plusSide == 0) {
            return -1;
        }
        if (behind >= minusSide && behind >= plusSide) {
            return -1;
        }
        if (minusSide == plusSide && previous != -1) {
            return previous;
        }
        return minusSide > plusSide ? 0 : 1;
    }

    private static void clickAttack() {
        int keyCode = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(keyCode, true);
        KeyBinding.onTick(keyCode);
        KeyBinding.setKeyBindState(keyCode, false);
    }

    private static boolean rayHits(EntityLivingBase entity, float yaw, float pitch, double range) {
        if (entity == null) {
            return false;
        }
        Vec3 from = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = getVectorForRotation(pitch, yaw);
        Vec3 to = from.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        float f = entity.getCollisionBorderSize();
        AxisAlignedBB box = entity.getEntityBoundingBox().expand(f, f, f);
        return box.calculateIntercept(from, to) != null;
    }

    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(yaw * 0.017453292F);
        float f1 = MathHelper.sin(yaw * 0.017453292F);
        float f2 = MathHelper.sin(pitch * 0.017453292F);
        float f3 = MathHelper.cos(pitch * 0.017453292F);
        return new Vec3((double) (f1 * f3), (double) (-f2), (double) (f * f3));
    }

    @EventTarget
    public void onRenderWorld(Render3DEvent e) {
        if (!arrow || !renderArrow.get()) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        long l = System.currentTimeMillis() - arrowAt;
        if (l > 600L) {
            arrow = false;
            return;
        }
        double d2;
        double d;
        double alpha = 1.0;
        d2 = (double) l / 100.0;
        d2 = d2 < 0.0 ? 0.0 : Math.min(d2, 1.0);
        d = d2 * d2 * (3.0 - 2.0 * d2);
        alpha *= 0.08 + 0.92 * d;
        if (l > 500L) {
            d2 = (double) (l - 500L) / 100.0;
            d2 = d2 < 0.0 ? 0.0 : Math.min(d2, 1.0);
            d = d2 * d2 * (3.0 - 2.0 * d2);
            alpha *= 1.0 - 0.92 * d;
        }
        d2 = (double) l / 450.0;
        d2 = d2 < 0.0 ? 0.0 : Math.min(d2, 1.0);
        d = 1.0 - d2;
        d2 = 1.0 - d * d * d;
        d = Math.sin((double) l * 0.015) * 0.02 * (0.4 + 0.6 * alpha);
        double dist = 0.8 * d2;
        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;
        double x = arrowX - vx + dirX * dist;
        double y = arrowY - vy + d;
        double z = arrowZ - vz + dirZ * dist;
        double yaw = Math.toDegrees(Math.atan2(-dirZ, dirX));
        int fill = (int) (225.0 * alpha) << 24 | 0xFFFFFF;
        int outline = (int) (255.0 * alpha) << 24 | 0xF6F6F6;
        int shadow = (int) (140.0 * alpha) << 24 | 0x5081A;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate((float) yaw, 0.0f, 1.0f, 0.0f);
        GL11.glPushMatrix();
        GL11.glTranslated(0.08 * 0.48, -0.08 * 0.24, -0.02);
        color(shadow);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(-0.2784 * 1.01, -0.0816 * 1.01, 0.0);
        GL11.glVertex3d(0.0 * 1.01, -0.0288 * 1.01, 0.0);
        GL11.glVertex3d(0.0 * 1.01, 0.0288 * 1.01, 0.0);
        GL11.glVertex3d(-0.2784 * 1.01, 0.0816 * 1.01, 0.0);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3d(0.0, -0.144 * 1.01, 0.0);
        GL11.glVertex3d(0.288 * 1.01, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144 * 1.01, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();
        color(fill);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(-0.2784, -0.0816, 0.0);
        GL11.glVertex3d(0.0, -0.0288, 0.0);
        GL11.glVertex3d(0.0, 0.0288, 0.0);
        GL11.glVertex3d(-0.2784, 0.0816, 0.0);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3d(0.0, -0.144, 0.0);
        GL11.glVertex3d(0.288, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144, 0.0);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        color(outline);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(-0.2784, -0.0816, 0.0);
        GL11.glVertex3d(0.0, -0.0288, 0.0);
        GL11.glVertex3d(0.0, 0.0288, 0.0);
        GL11.glVertex3d(-0.2784, 0.0816, 0.0);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, -0.144, 0.0);
        GL11.glVertex3d(0.288, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144, 0.0);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void color(int c) {
        GlStateManager.color((float) (c >> 16 & 0xFF) / 255.0f, (float) (c >> 8 & 0xFF) / 255.0f, (float) (c & 0xFF) / 255.0f, (float) (c >> 24 & 0xFF) / 255.0f);
    }

    private static void setArrow(EntityLivingBase entity, float f) {
        double d = Math.toRadians(mc.thePlayer.rotationYaw + f);
        dirX = -Math.sin(d);
        dirZ = Math.cos(d);
        arrowX = entity.posX + dirX * 0.8;
        arrowY = entity.posY + 1.2;
        arrowZ = entity.posZ + dirZ * 0.8;
        arrowAt = System.currentTimeMillis();
        arrow = true;
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private static void resetState() {
        if (blinkTicks > 0) {
            blinkTicks = 0;
            BlinkComponent.dispatch(true);
        }
        holding = false;
        arrow = false;
        rotate = false;
        queued = false;
        forcedAttacks = 0;
        selfClick = false;
        yawDisplaced = false;
        auraDisplaced = false;
        holdTicks = 0;
        lastSide = -1;
        side = -1;
    }
}
