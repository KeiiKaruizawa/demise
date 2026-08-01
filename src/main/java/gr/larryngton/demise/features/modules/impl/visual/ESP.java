package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.events.impl.render.RenderNameTagEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.AntiBot;
import gr.larryngton.demise.features.modules.impl.misc.AntiCheat;
import gr.larryngton.demise.features.modules.impl.misc.MurderMystery;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ColorValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.GLUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "ESP", description = "The CS")
public class ESP extends Module {
    public final BoolValue noSelf = new BoolValue("No Self", true, this);
    public final BoolValue noDead = new BoolValue("No Dead", true, this);
    public final BoolValue tags = new BoolValue("Tags", true, this);
    public final SliderValue tagsSize = new SliderValue("Tags Size", 0.5f, 0.1f, 2, 0.05f, this, tags::get);
    public final BoolValue tagsHealth = new BoolValue("Tags Health", true, this, tags::get);
    public final BoolValue tagsBackground = new BoolValue("Tags Background", true, this, tags::get);
    public final BoolValue item = new BoolValue("Item", true, this, tags::get);
    public final BoolValue esp2d = new BoolValue("2D ESP", true, this);
    public final BoolValue box = new BoolValue("Box", true, this, esp2d::get);
    public final ModeValue boxMode = new ModeValue("Box Mode", new String[]{"Outline", "Corners"}, "Outline", this, box::get);
    public final BoolValue boxSyncColor = new BoolValue("Box Sync Color", false, this, () -> esp2d.get() && box.get());
    public final ColorValue boxColor = new ColorValue("Box Color", Color.RED, this, () -> esp2d.get() && box.get() && !boxSyncColor.get());
    public final BoolValue healthBar = new BoolValue("Health Bar", true, this, esp2d::get);
    public final BoolValue healthBarSyncColor = new BoolValue("Health Bar Sync Color", false, this, () -> esp2d.get() && healthBar.get());
    public final ColorValue absorptionColor = new ColorValue("Absorption Color", new Color(255, 255, 50), this, () -> esp2d.get() && healthBar.get() && !healthBarSyncColor.get());
    public final BoolValue armorBar = new BoolValue("Armor Bar", true, this, esp2d::get);
    public final ColorValue armorBarColor = new ColorValue("Armor Bar Color", new Color(50, 255, 255), this, () -> esp2d.get() && armorBar.get());

    private final Map<EntityPlayer, float[]> entityPosMap = new HashMap<>();

    @Override
    public void onDisable() {
        entityPosMap.clear();
    }

    @EventTarget
    public void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (tags.get() && entityPosMap.containsKey(entity))
            event.setCancelled(true);
    }

    @EventTarget
    public void onWorld(WorldChangeEvent event) {
        entityPosMap.clear();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        for (EntityPlayer player : entityPosMap.keySet()) {
            if ((player.getDistanceToEntity(mc.thePlayer) < 1.0F && mc.gameSettings.thirdPersonView == 0) ||
                    !RenderUtils.isBBInFrustum(player.getEntityBoundingBox()) || (noSelf.get() && player.equals(mc.thePlayer)) ||
                    (Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isEnabled() && Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isBot(player)) ||
                    (noDead.get() && player.isDead))
                continue;

            final float[] positions = entityPosMap.get(player);
            final float x = positions[0];
            final float y = positions[1];
            final float x2 = positions[2];
            final float y2 = positions[3];

            final float health = PlayerUtils.getActualHealth(player);
            final float maxHealth = player.getMaxHealth();
            final float healthPercentage = health / maxHealth;

            if (!esp2d.get() && !tags.get()) {
                return;
            }

            if (tags.get()) {
                final FontRenderer fontRenderer = mc.fontRendererObj;

                final String healthString = tagsHealth.get() ? (player.getDisplayName().getFormattedText().endsWith(" ") ? "" : " ") + MathUtils.roundToHalf(health) : "";
                String name = player.getDisplayName().getFormattedText();

                if (Demise.INSTANCE.getFriendManager().isFriend(player)) {
                    name += EnumChatFormatting.GREEN + " [Friend]";
                }

                if (Demise.INSTANCE.getModuleManager().getModule(AntiCheat.class).isCheater(player)) {
                    name += EnumChatFormatting.RED + " [Cheater]";
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

                String name2 = name + healthString;
                float halfWidth = (float) fontRenderer.getStringWidth(name2) / 2 * tagsSize.get();
                final float xDif = x2 - x;
                final float middle = x + (xDif / 2);
                final float textHeight = fontRenderer.FONT_HEIGHT * tagsSize.get();
                float renderY = y - textHeight - 2;

                final float left = middle - halfWidth - 1;
                final float right = middle + halfWidth + 1;

                if (tagsBackground.get()) {
                    Gui.drawRect(left, renderY - 1, right, renderY + textHeight + 1, 0x96000000);
                }

                fontRenderer.drawScaledString(name, middle - halfWidth, renderY + 0.5F, tagsSize.get(), -1);
                fontRenderer.drawScaledString(healthString, middle - halfWidth + fontRenderer.getStringWidth(name) * tagsSize.get(), renderY + 0.5F, tagsSize.get(), ColorUtils.getHealthColor(player));

                if (item.get()) {
                    List<ItemStack> items = new ArrayList<>();
                    if (player.getHeldItem() != null) {
                        items.add(player.getHeldItem());
                    }
                    for (int index = 3; index >= 0; index--) {
                        ItemStack stack = player.inventory.armorInventory[index];
                        if (stack != null) {
                            items.add(stack);
                        }
                    }
                    float armorX = middle - ((float) (items.size() * 16) / 2) * tagsSize.get();

                    for (ItemStack stack : items) {
                        RenderUtils.renderItemStack(stack, armorX, renderY - 19 * tagsSize.get(), tagsSize.get(), true, false, tagsSize.get() + tagsSize.get() / 2);
                        armorX += 16 * tagsSize.get();
                    }
                }
            }

            if (esp2d.get()) {
                glDisable(GL_TEXTURE_2D);
                GLUtils.startBlend();

                if (armorBar.get()) {
                    final float armorPercentage = player.getTotalArmorValue() / 20.0F;
                    final float armorBarWidth = (x2 - x) * armorPercentage;

                    glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);
                    glBegin(GL_QUADS);

                    // Background
                    {
                        glVertex2f(x, y2 + 0.5F);
                        glVertex2f(x, y2 + 2.5F);

                        glVertex2f(x2, y2 + 2.5F);
                        glVertex2f(x2, y2 + 0.5F);
                    }

                    if (armorPercentage > 0) {
                        RenderUtils.color(armorBarColor.get().getRGB());

                        // Bar
                        {
                            glVertex2f(x + 0.5F, y2 + 1);
                            glVertex2f(x + 0.5F, y2 + 2);

                            glVertex2f(x + armorBarWidth - 0.5F, y2 + 2);
                            glVertex2f(x + armorBarWidth - 0.5F, y2 + 1);
                        }
                        RenderUtils.resetColor();
                    }

                    if (!healthBar.get())
                        glEnd();
                }

                if (healthBar.get()) {
                    float healthBarLeft = x - 2.5F;
                    float healthBarRight = x - 0.5F;

                    glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);

                    if (!armorBar.get())
                        glBegin(GL_QUADS);

                    // Background
                    {
                        glVertex2f(healthBarLeft, y);
                        glVertex2f(healthBarLeft, y2);

                        glVertex2f(healthBarRight, y2);
                        glVertex2f(healthBarRight, y);
                    }

                    healthBarLeft += 0.5F;
                    healthBarRight -= 0.5F;

                    final float heightDif = y - y2;
                    final float healthBarHeight = heightDif * healthPercentage;

                    final float topOfHealthBar = y2 + 0.5F + healthBarHeight;

                    if (healthBarSyncColor.get()) {
                        final int syncedcolor = getModule(Interface.class).color();

                        RenderUtils.color(syncedcolor);
                    } else {
                        final int color = ColorUtils.getColorFromPercentage(healthPercentage);

                        RenderUtils.color(color);
                    }
                    // Bar
                    {
                        glVertex2f(healthBarLeft, topOfHealthBar);
                        glVertex2f(healthBarLeft, y2 - 0.5F);

                        glVertex2f(healthBarRight, y2 - 0.5F);
                        glVertex2f(healthBarRight, topOfHealthBar);
                    }

                    RenderUtils.resetColor();


                    final float absorption = player.getAbsorptionAmount();

                    final float absorptionPercentage = Math.min(1.0F, absorption / 20.0F);

                    final int absorptionColor = this.absorptionColor.get().getRGB();

                    final float absorptionHeight = heightDif * absorptionPercentage;

                    final float topOfAbsorptionBar = y2 + 0.5F + absorptionHeight;

                    if (healthBarSyncColor.get()) {
                        RenderUtils.color(getModule(Interface.class).color());
                    } else {
                        RenderUtils.color(absorptionColor);
                    }

                    // Absorption Bar
                    {
                        glVertex2f(healthBarLeft, topOfAbsorptionBar);
                        glVertex2f(healthBarLeft, y2 - 0.5F);

                        glVertex2f(healthBarRight, y2 - 0.5F);
                        glVertex2f(healthBarRight, topOfAbsorptionBar);
                    }

                    RenderUtils.resetColor();

                    if (!box.get())
                        glEnd();
                }

                if (box.get()) {
                    switch (boxMode.get()) {
                        case "Outline":
                            glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);

                            if (!healthBar.get())
                                glBegin(GL_QUADS);

                            // Background
                        {
                            // Left
                            glVertex2f(x, y);
                            glVertex2f(x, y2);
                            glVertex2f(x + 1.5F, y2);
                            glVertex2f(x + 1.5F, y);

                            // Right
                            glVertex2f(x2 - 1.5F, y);
                            glVertex2f(x2 - 1.5F, y2);
                            glVertex2f(x2, y2);
                            glVertex2f(x2, y);

                            // Top
                            glVertex2f(x + 1.5F, y);
                            glVertex2f(x + 1.5F, y + 1.5F);
                            glVertex2f(x2 - 1.5F, y + 1.5F);
                            glVertex2f(x2 - 1.5F, y);

                            // Bottom
                            glVertex2f(x + 1.5F, y2 - 1.5F);
                            glVertex2f(x + 1.5F, y2);
                            glVertex2f(x2 - 1.5F, y2);
                            glVertex2f(x2 - 1.5F, y2 - 1.5F);
                        }

                        if (boxSyncColor.get()) {
                            RenderUtils.color(getModule(Interface.class).color());
                        } else {
                            RenderUtils.color(boxColor.get().getRGB());
                        }

                        // Box
                        {
                            // Left
                            glVertex2f(x + 0.5F, y + 0.5F);
                            glVertex2f(x + 0.5F, y2 - 0.5F);
                            glVertex2f(x + 1, y2 - 0.5F);
                            glVertex2f(x + 1, y + 0.5F);

                            // Right
                            glVertex2f(x2 - 1, y + 0.5F);
                            glVertex2f(x2 - 1, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y + 0.5F);

                            // Top
                            glVertex2f(x + 0.5F, y + 0.5F);
                            glVertex2f(x + 0.5F, y + 1);
                            glVertex2f(x2 - 0.5F, y + 1);
                            glVertex2f(x2 - 0.5F, y + 0.5F);

                            // Bottom
                            glVertex2f(x + 0.5F, y2 - 1);
                            glVertex2f(x + 0.5F, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y2 - 1);
                        }

                        RenderUtils.resetColor();

                        glEnd();
                            break;
                        case "Corners":
                            Color c;
                            if (boxSyncColor.get()) {
                                c = new Color(getModule(Interface.class).color());
                            } else {
                                c = boxColor.get();
                            }
                            drawCornerBox(x,y,x2-x,y2-y,c);
                            break;
                    }
                }

                glEnable(GL_TEXTURE_2D);
                GLUtils.endBlend();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        final boolean project2D = esp2d.get() || tags.get();
        if (project2D && !entityPosMap.isEmpty())
            entityPosMap.clear();

        final float partialTicks = event.partialTicks();

        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (project2D) {

                final double posX = (MathUtils.interpolate(player.prevPosX, player.posX, partialTicks) -
                        mc.getRenderManager().viewerPosX);
                final double posY = (MathUtils.interpolate(player.prevPosY, player.posY, partialTicks) -
                        mc.getRenderManager().viewerPosY);
                final double posZ = (MathUtils.interpolate(player.prevPosZ, player.posZ, partialTicks) -
                        mc.getRenderManager().viewerPosZ);

                final double halfWidth = player.width / 2.0D;
                final AxisAlignedBB bb = new AxisAlignedBB(posX - halfWidth, posY, posZ - halfWidth,
                        posX + halfWidth, posY + player.height + (player.isSneaking() ? -0.2D : 0.1D), posZ + halfWidth).expand(0.1, 0.1, 0.1);

                final double[][] vectors = {{bb.minX, bb.minY, bb.minZ},
                        {bb.minX, bb.maxY, bb.minZ},
                        {bb.minX, bb.maxY, bb.maxZ},
                        {bb.minX, bb.minY, bb.maxZ},
                        {bb.maxX, bb.minY, bb.minZ},
                        {bb.maxX, bb.maxY, bb.minZ},
                        {bb.maxX, bb.maxY, bb.maxZ},
                        {bb.maxX, bb.minY, bb.maxZ}};

                float[] projection;
                final float[] position = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, -1.0F, -1.0F};

                for (final double[] vec : vectors) {
                    projection = GLUtils.project2D((float) vec[0], (float) vec[1], (float) vec[2], event.scaledResolution().getScaleFactor());
                    if (projection != null && projection[2] >= 0.0F && projection[2] < 1.0F) {
                        final float pX = projection[0];
                        final float pY = projection[1];
                        position[0] = Math.min(position[0], pX);
                        position[1] = Math.min(position[1], pY);
                        position[2] = Math.max(position[2], pX);
                        position[3] = Math.max(position[3], pY);
                    }
                }

                entityPosMap.put(player, position);
            }
        }
    }

    public static void drawCornerBox(float x, float y, float width, float height, Color color) {
        Color color1 = new Color(0,0,0,150);
        float halfWidth = width / 4f;
        float halfHeight = height / 4f;

        //background
        {
            //top left
            drawRect(x + 1.5f, y, halfWidth, 1.5f, color1);
            drawRect(x, y, 1.5f, halfHeight + 0.5f, color1);

            //top right
            drawRect(x + width - halfWidth - 1.5f, y, halfWidth, 1.5f, color1);
            drawRect(x + width - 1.5f, y, 1.5f, halfHeight + 0.5f, color1);

            //bottom left
            drawRect(x + 1.5f, y + height - 1.5f, halfWidth, 1.5f, color1);
            drawRect(x, y + height - halfHeight - 0.5f, 1.5f, halfHeight + 0.5f, color1);

            //bottom right
            drawRect(x + width - halfWidth - 1f, y + height - 1.5f, halfWidth, 1.5f, color1);
            drawRect(x + width - 1.5f, y + height - halfHeight - 0.5f, 1.5f, halfHeight + 0.5f, color1);
        }

        //lines
        {
            //top left
            drawRect(x + 1f, y + 0.5f, halfWidth, 0.5f, color);
            drawRect(x + 0.5f, y + 0.5f, 0.5f, halfHeight - 0.5f, color);

            //top right
            drawRect(x + width - halfWidth - 1f, y + 0.5f, halfWidth, 0.5f, color);
            drawRect(x + width - 1f, y + 0.5f, 0.5f, halfHeight - 0.5f, color);

            //bottom left
            drawRect(x + 1f, y + height - 1f, halfWidth, 0.5f, color);
            drawRect(x + 0.5f, y + height - halfHeight, 0.5f, halfHeight - 0.5f, color);

            //bottom right
            drawRect(x + width - halfWidth - 0.5f, y + height - 1f, halfWidth, 0.5f, color);
            drawRect(x + width - 1f, y + height - halfHeight, 0.5f, halfHeight - 0.5f, color);
        }
    }


    public static void drawRect(float x, float y, float width, float height, Color color) {
        glPushMatrix();
        glBegin(GL_QUADS);
        GL11.glColor4f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f);
        GL11.glVertex2f(x+width,y);
        GL11.glVertex2f(x,y);
        GL11.glVertex2f(x,y+height);
        GL11.glVertex2f(x+width,y+height);
        glEnd();
        glPopMatrix();
    }
}