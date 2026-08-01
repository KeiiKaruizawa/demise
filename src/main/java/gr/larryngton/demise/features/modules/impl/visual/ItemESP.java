package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.ItemUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.Range;
import org.lwjglx.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleInfo(name = "ItemESP", description = "ESP but for items.")
public class ItemESP extends Module {
    private final SliderValue offsetY = new SliderValue("Offset Y", -10, -20, 20, 1, this);
    private final DoubleSliderValue scale = new DoubleSliderValue("Scale", 0.01F, 1, 0.05F, 2, 0.05F, this);
    private final BoolValue tagsBackground = new BoolValue("Tags background", true, this);
    private final BoolValue filterUnneeded = new BoolValue("Filter unneeded", true, this);

    private final Map<UUID, Vector2f> posMap = new HashMap<>();
    private final List<EntityItem> itemList = new ArrayList<>();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.ticksExisted % 2 == 0) {
            itemList.clear();

            for (Entity entity : mc.theWorld.getLoadedEntityList()) {
                if (entity instanceof EntityItem entityItem) {
                    if (ItemUtils.isValidStack(entityItem.getEntityItem()) || !filterUnneeded.get()) {
                        itemList.add(entityItem);
                    }
                }
            }
        }

//        if (mc.thePlayer.ticksExisted % 10 == 0) {
//            itemList.sort(Comparator.comparingDouble(item -> -mc.thePlayer.getDistanceSqToEntity(item)));
//        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        for (EntityItem entityItem : itemList) {
            if (!RenderUtils.isBBInFrustum(entityItem.getEntityBoundingBox())) {
                continue;
            }

            float playerX = (float) MathUtils.interpolate(entityItem.prevPosX, entityItem.posX);
            float playerY = (float) MathUtils.interpolate(entityItem.prevPosY, entityItem.posY) + entityItem.height + 25f / 100;
            float playerZ = (float) MathUtils.interpolate(entityItem.prevPosZ, entityItem.posZ);

            Vector2f pos = RenderUtils.worldToScreen(playerX, playerY, playerZ, new ScaledResolution(mc), true);

            if (pos == null) {
                pos = posMap.getOrDefault(entityItem.getUniqueID(), new Vector2f(0, 0));
            }

            posMap.put(entityItem.getUniqueID(), pos);

            float x = pos.x;
            float y = pos.y;

            if (mc.currentScreen instanceof PanelGui) {
                if (Range.between(PanelGui.posX, PanelGui.posX + 450).contains(x) && Range.between(PanelGui.posY, PanelGui.posY + 300).contains(y)) {
                    return;
                }
            }

            String enhancement = "";

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, entityItem.getEntityItem()) != 0) {
                enhancement = EnumChatFormatting.AQUA + " Protection:" + EnumChatFormatting.RED + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, entityItem.getEntityItem());
            }

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, entityItem.getEntityItem()) != 0) {
                enhancement = EnumChatFormatting.AQUA + " Sharpness:" + EnumChatFormatting.RED + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, entityItem.getEntityItem());
            }

            if (entityItem.getEntityItem().getItem() == Items.golden_apple) {
                if (entityItem.getEntityItem().getItem().hasEffect(entityItem.getEntityItem())) {
                    enhancement = EnumChatFormatting.RED + " Enchanted";
                }
            }

            String var3 = (entityItem.getEntityItem().stackSize > 1) ? (EnumChatFormatting.RESET + " x" + entityItem.getEntityItem().stackSize) : "";

            FontRenderer modernFont = Fonts.interRegular.get(17);
            String name = entityItem.getEntityItem().getDisplayName() + var3 + enhancement;

            float halfWidth = (float) modernFont.getStringWidth(name) / 2;
            float middle = x - halfWidth;
            float textHeight = (float) modernFont.getHeight();
            float renderY = y - textHeight - offsetY.get();

            GlStateManager.pushMatrix();
            GlStateManager.translate(middle + (modernFont.getStringWidth(name) + 6) / 2F, renderY - 0.5f - 3 + (textHeight + 3) / 2F, 0);

            double scale = MathUtils.interpolateNoUpdateCheck(this.scale.getEnd(), this.scale.getStart(), MathHelper.clamp_double(mc.thePlayer.getDistanceToEntity(entityItem) / 200, 0, 1));
            GlStateManager.scale(scale, scale, scale);

            GlStateManager.translate(-(middle + (modernFont.getStringWidth(name) + 6) / 2F), -(renderY - 0.5f - 3 + (textHeight + 3) / 2F), 0);

            if (tagsBackground.get()) {
                RectUtils.drawRound(middle, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, new Color(getModule(Interface.class).bgColor(), true));
            }

            modernFont.drawStringWithShadow(name, middle + 3, renderY + 0.5f, -1);
            GlStateManager.popMatrix();

            if (tagsBackground.get()) {
                Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(middle + (modernFont.getStringWidth(name) + 6) / 2F, renderY - 0.5f - 3 + (textHeight + 3) / 2F, 0);
                    GlStateManager.scale(scale, scale, scale);
                    GlStateManager.translate(-(middle + (modernFont.getStringWidth(name) + 6) / 2F), -(renderY - 0.5f - 3 + (textHeight + 3) / 2F), 0);
                    RectUtils.drawShaderRound(middle, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, Color.black);
                    GlStateManager.popMatrix();
                });
            }
        }
    }
}