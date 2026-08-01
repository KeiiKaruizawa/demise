package gr.larryngton.demise.features.modules.impl.misc;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.KeyPressEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.TextValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.*;
import net.minecraft.network.play.client.*;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Keyboard;

import java.awt.*;

@ModuleInfo(name = "ViaMCPPlus", description = "Improves ViaMCP by implementing 1.9+ mechanics into 1.8.", legit = true)
public class ViaMCPPlus extends Module {
    public final TextValue offHandBind = new TextValue("Offhand bind", "V", this);
    public final BoolValue attackCooldown = new BoolValue("Attack cooldown", false, this);

    private float interpolatedWidthPercentage;
    private float interpolatedScale;
    public static float attackStrengthTicker;
    private int lastSlot;
    private final TimerUtil packetTimer = new TimerUtil();
    private final TimerUtil actionTimer = new TimerUtil();

    public ViaMCPPlus() {
        offHandBind.setDescription("Type the key's name in all caps.");
    }

    @EventTarget
    public void onPostMotion(MotionEvent e) {
        if (e.isPost() && attackCooldown.get()) {
            attackStrengthTicker++;

            if (lastSlot != mc.thePlayer.inventory.currentItem) {
                attackStrengthTicker = 0;
            }

            lastSlot = mc.thePlayer.inventory.currentItem;
        }
    }

    public float getCurrentItemAttackStrengthDelay() {
        return (float) (1.0 / (getAttackSpeed() + 4) * 20.0);
    }

    private float getAttackSpeed() {
        ItemStack s = mc.thePlayer.getCurrentEquippedItem();

        if (s == null) {
            return 0;
        }

        Item i = s.getItem();

        if (i == null) {
            return 0;
        }

        if (i instanceof ItemSword) {
            return -2.4F;
        }

        if (i instanceof ItemPickaxe) {
            return -2.8F;
        }

        if (i instanceof ItemSpade) {
            return -3F;
        }

        if (i instanceof ItemAxe axe) {
            return switch (axe.getToolMaterial()) {
                case WOOD, STONE -> -3.2F;
                case EMERALD, GOLD -> -3F;
                case IRON -> -3.1F;
            };
        }

        if (i instanceof ItemHoe hoe) {
            // dear mojang why the actual fuck can i not do hoe.getToolMaterial()
            return switch (hoe.getMaterialName()) {
                case "WOOD", "GOLD" -> -3F;
                case "STONE" -> -2F;
                case "IRON" -> -1F;
                default -> 0F;
            };
        }

        return 0;
    }

    public float getAttackStrengthScale() {
        return MathHelper.clamp_float((float) ((attackStrengthTicker + 0.5) / getCurrentItemAttackStrengthDelay()), 0.0F, 1.0F);
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (attackCooldown.get()) {
            float widthPercentage = getAttackStrengthScale() != 0 ? 1 - getAttackStrengthScale() : 0;

            interpolatedWidthPercentage = MathUtils.interpolate(interpolatedWidthPercentage, widthPercentage != 0 ? widthPercentage : 1);
            float a = widthPercentage != 0 ? interpolatedWidthPercentage : 0;

            interpolatedScale = MathUtils.interpolate(interpolatedScale, widthPercentage != 0 ? 1 : 0, 0.15f);

            float baseWidth = 50;
            float width = baseWidth * a;

            ScaledResolution sr = new ScaledResolution(mc);

            float x = sr.getScaledWidth() / 2f - baseWidth / 2f;
            float y = sr.getScaledHeight() / 2f + 20;

            RenderUtils.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f + 20, interpolatedScale);
            RectUtils.drawRound(x, y, baseWidth, 2, 1, new Color(0, 0, 0, 150));
            RectUtils.drawGradientHorizontal(x, y, width, 2, 1, new Color(getModule(Interface.class).color()), new Color(getModule(Interface.class).color((int) widthPercentage)));
            RenderUtils.scaleEnd();
        }

        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
            if (attackCooldown.get()) {
                float baseWidth = 50;

                ScaledResolution sr = new ScaledResolution(mc);

                float x = sr.getScaledWidth() / 2f - baseWidth / 2f;
                float y = sr.getScaledHeight() / 2f + 20;

                RenderUtils.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f + 20, interpolatedScale);
                RectUtils.drawShaderRound(x, y, baseWidth, 2, 1, Color.black);
                RenderUtils.scaleEnd();
            }
        });
    }

    @EventTarget
    public void onKey(KeyPressEvent e) {
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
            if (e.getKey() == Keyboard.getKeyIndex(offHandBind.get())) {
                // you can't directly access via version's offhand handler shit, so this has to do
                ChatUtils.sendMessageServer("/offhand");
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_13)) {
            if (e.getPacket() instanceof C0APacketAnimation || e.getPacket() instanceof C08PacketPlayerBlockPlacement || e.getPacket() instanceof C0BPacketEntityAction || e.getPacket() instanceof C02PacketUseEntity) {
                actionTimer.reset();
            }

            if (actionTimer.hasTimeElapsed(100) && e.getPacket() instanceof C03PacketPlayer c03 && !c03.isMoving() && !c03.getRotating() && c03.isOnGround() && !packetTimer.hasTimeElapsed(1000)) {
                e.setCancelled(true);
            } else {
                packetTimer.reset();
            }
        }
    }
}