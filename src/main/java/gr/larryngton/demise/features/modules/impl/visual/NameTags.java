package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.events.impl.render.RenderNameTagEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.AntiBot;
import gr.larryngton.demise.features.modules.impl.misc.AntiCheat;
import gr.larryngton.demise.features.modules.impl.misc.MurderMystery;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.Range;
import org.lwjglx.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleInfo(name = "NameTags", description = "Renders player nametags.")
public class NameTags extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Modern"}, "Modern", this);
    private final SliderValue offsetY = new SliderValue("Offset Y", 0, -20, 20, 1, this);
    private final DoubleSliderValue scale = new DoubleSliderValue("Scale", 0.01F, 1, 0.05F, 2, 0.05F, this);
    private final BoolValue tagsHealth = new BoolValue("Tags health", true, this);
    private final BoolValue tagsBackground = new BoolValue("Tags background", true, this);

    private final Map<UUID, Vector2f> posMap = new HashMap<>();
    private final List<EntityPlayer> playerList = new ArrayList<>();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.ticksExisted % 2 == 0) {
            playerList.clear();

            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (isValid(player)) {
                    playerList.add(player);
                }
            }
        }

//        if (mc.thePlayer.ticksExisted % 10 == 0) {
//            playerList.sort(Comparator.comparingDouble(player -> -mc.thePlayer.getDistanceSqToEntity(player)));
//        }
    }

    @EventTarget
    public void onRenderNameTag(RenderNameTagEvent e) {
        if (isValid(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        for (EntityPlayer player : playerList) {
            if (!RenderUtils.isBBInFrustum(player.getEntityBoundingBox())) {
                continue;
            }

            float playerX = (float) MathUtils.interpolate(player.prevPosX, player.posX);
            float playerY = (float) MathUtils.interpolate(player.prevPosY, player.posY) + player.height + 25f / 100;
            float playerZ = (float) MathUtils.interpolate(player.prevPosZ, player.posZ);

            Vector2f pos = RenderUtils.worldToScreen(playerX, playerY, playerZ, new ScaledResolution(mc), true);

            if (pos == null) {
                pos = posMap.getOrDefault(player.getUniqueID(), new Vector2f(0, 0));
            }

            posMap.put(player.getUniqueID(), pos);

            float x = pos.x;
            float y = pos.y;

            if (mc.currentScreen instanceof PanelGui) {
                if (Range.between(PanelGui.posX, PanelGui.posX + 450).contains(x) && Range.between(PanelGui.posY, PanelGui.posY + 300).contains(y)) {
                    continue;
                }
            }

            FontRenderer modernFont = Fonts.interRegular.get(17);
            String healthString = tagsHealth.get() ? " " + (MathUtils.roundToHalf(PlayerUtils.getActualHealth(player) + player.getAbsorptionAmount())) + EnumChatFormatting.RED + "❤" : "";
            String name = player.getDisplayName().getFormattedText() + healthString;

            if (Demise.INSTANCE.getFriendManager().isFriend(player)) {
                name += EnumChatFormatting.GREEN + " [Friend]" + EnumChatFormatting.RESET;
            }

            if (Demise.INSTANCE.getModuleManager().getModule(AntiCheat.class).isCheater(player)) {
                name += EnumChatFormatting.RED + " [Cheater]" + EnumChatFormatting.RESET;
            }

            MurderMystery murderMystery = getModule(MurderMystery.class);

            if (murderMystery.isEnabled()) {
                if (murderMystery.murderer != null && player.getName().equals(murderMystery.murderer.getName())) {
                    name += EnumChatFormatting.DARK_RED + " [Murderer]" + EnumChatFormatting.RESET;
                }

                if (murderMystery.detective != null && player.getName().equals(murderMystery.detective.getName())) {
                    name += EnumChatFormatting.BLUE + " [Detective]" + EnumChatFormatting.RESET;
                }
            }

            float halfWidth = mode.is("Vanilla") ? (float) mc.fontRendererObj.getStringWidth(name) / 2 : (float) modernFont.getStringWidth(name) / 2;
            float renderX = x - halfWidth;
            float textHeight = mode.is("Vanilla") ? mc.fontRendererObj.FONT_HEIGHT : (float) modernFont.getHeight();
            float renderY = y - textHeight - offsetY.get();

            GlStateManager.pushMatrix();
            GlStateManager.translate(renderX + (modernFont.getStringWidth(name) + 6) / 2F, renderY - 0.5f - 3 + (textHeight + 3) / 2F, 0);

            double scale = MathUtils.interpolateNoUpdateCheck(this.scale.getEnd(), this.scale.getStart(), MathHelper.clamp_double(mc.thePlayer.getDistanceToEntity(player) / 200, 0, 1));
            GlStateManager.scale(scale, scale, scale);

            GlStateManager.translate(-(renderX + (modernFont.getStringWidth(name) + 6) / 2F), -(renderY - 0.5f - 3 + (textHeight + 3) / 2F), 0);

            if (mode.is("Vanilla")) {
                if (tagsBackground.get()) {
                    RenderUtils.drawRect(renderX, renderY - 1, mc.fontRendererObj.getStringWidth(name) + 2, textHeight + 2, 0x96000000);
                }

                mc.fontRendererObj.drawScaledString(name, renderX + 1, renderY + 0.5f, 1, -1);
            } else {
                if (tagsBackground.get()) {
                    RectUtils.drawRound(renderX, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, new Color(getModule(Interface.class).bgColor(), true));
                }

                modernFont.drawStringWithShadow(name, renderX + 3, renderY + 0.5f, -1);
            }

            GlStateManager.popMatrix();

            if (mode.is("Modern") && tagsBackground.get()) {
                String finalName = name;
                Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(renderX + (modernFont.getStringWidth(finalName) + 6) / 2F, renderY - 0.5f - 3 + (textHeight + 3) / 2F, 0);
                    GlStateManager.scale(scale, scale, scale);
                    GlStateManager.translate(-(renderX + (modernFont.getStringWidth(finalName) + 6) / 2F), -(renderY - 0.5f - 3 + (textHeight + 3) / 2F), 0);
                    RectUtils.drawShaderRound(renderX, renderY - 0.5f - 3, modernFont.getStringWidth(finalName) + 6, textHeight + 3, 4, Color.black);
                    GlStateManager.popMatrix();
                });
            }
        }
    }

    public boolean isValid(Entity entity) {
        if (entity instanceof EntityPlayer player) {
            if (!player.isEntityAlive()) {
                return false;
            }

            if (entity == mc.thePlayer) {
                return false;
            }

            if (getModule(AntiBot.class).isBot(player)) {
                return false;
            }

            return mc.theWorld.playerEntities.contains(player);
        }

        return false;
    }
}
