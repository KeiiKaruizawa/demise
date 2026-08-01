package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.render.RenderItemEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Animations", description = "Modifies hand animations.", legit = true)
public class Animations extends Module {
    public final BoolValue modifySwingSpeed = new BoolValue("Modify swing speed", false, this);
    public final SliderValue swingSpeed = new SliderValue("Swing speed", 0, -10, 10, 1, this, modifySwingSpeed::get);
    public final BoolValue noOverride = new BoolValue("Don't override swing reset time", true, this, modifySwingSpeed::get);
    private final BoolValue blockOnSwing = new BoolValue("Block on swing", true, this);
    public final BoolValue old = new BoolValue("Old", true, this);
    public final BoolValue noBlock = new BoolValue("Not While Blocking", false, this, old::get);
    public final BoolValue digOnly = new BoolValue("Digging only", false, this, () -> old.get() && !noBlock.get());
    public final BoolValue noReset = new BoolValue("No equip reset", false, this);
    private final ModeValue blockAnimation = new ModeValue("Block animation", new String[]{"None", "Default", "Exhibition", "Spin", "Shove", "Tap", "Xiv", "Gay"}, "Default", this);

    private final SliderValue translatedX = new SliderValue("Translated X", 0, -0.2f, 0.2f, 0.01f, this);
    private final SliderValue translatedY = new SliderValue("Translated Y", 0, -0.2f, 0.2f, 0.01f, this);
    private final SliderValue translatedZ = new SliderValue("Translated Z", 0, -0.2f, 0.2f, 0.01f, this);

    @EventTarget
    public void onGame(GameEvent e) {
        if (old.get() && mc.thePlayer.getItemInUse().getItem().getItemUseAction(mc.thePlayer.getItemInUse()) != EnumAction.NONE && mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt >= mc.thePlayer.getArmSwingAnimationEnd(noOverride.get()) / 2 || mc.thePlayer.swingProgressInt < 0) {
                mc.thePlayer.isSwingInProgress = true;
                mc.thePlayer.swingProgressInt = -1;
            }
        }
    }

    @EventTarget
    public void onRenderItem(RenderItemEvent e) {
        if (e.getItemToRender().getItem() instanceof ItemMap) {
            return;
        }

        if (blockOnSwing.get() && mc.thePlayer.isSwingInProgress && SpoofSlotUtils.getSpoofedStack() != null && SpoofSlotUtils.getSpoofedStack().getItem() instanceof ItemSword) {
            e.setUseItem(true);
        }

        if (e.isUseItem() && e.getEnumAction() == EnumAction.BLOCK) {
            GlStateManager.translate(translatedX.get(), translatedY.get(), translatedZ.get());
            float var = MathHelper.sin((float) (MathHelper.sqrt_float(e.getSwingProgress()) * Math.PI));

            if (blockAnimation.is("Default")) {
                return;
            }

            switch (blockAnimation.get()) {
                case "Exhibition": {
                    float funny = MathHelper.sin(MathHelper.sqrt_float(e.getSwingProgress()) * (float) Math.PI);
                    ItemRenderer.transformFirstPersonItem(e.getProgress() / 2.0F, 0.0F);
                    GlStateManager.translate(0.0F, 0.3F, -0.0F);
                    GlStateManager.rotate(-funny * 31.0F, 1, 0, 2.0F);
                    GlStateManager.rotate(-funny * 33.0F, 1.5F, (funny / 1.1F), 0F);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
                case "Spin": {
                    GlStateManager.translate(0.1f, -0.125f, -0.1f);
                    ItemRenderer.transformFirstPersonItem(e.getProgress() / 2f, 0.0f);
                    GlStateManager.rotate(45f, -0.0f, 0.0f, -0.1f);
                    GlStateManager.rotate(System.currentTimeMillis() % 360, -0.1f, 0.1f, -0.1f);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
                case "Xiv": {
                    ItemRenderer.transformFirstPersonItem(e.getProgress(), 0.0f);
                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                    GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                    final float var16 = MathHelper.sin((float) (e.getSwingProgress() * e.getSwingProgress() * Math.PI));
                    GlStateManager.rotate(-var16 * 20.0f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.rotate(-var * 20.0f, 0.0f, 0.0f, 1.0f);
                    GlStateManager.rotate(-var * 80.0f, 1.0f, 0.0f, 0.0f);
                    break;
                }
                case "Shove": {
                    GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
                    GlStateManager.translate(0.0F, 0, 0.0F);
                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(MathHelper.sin(e.getSwingProgress() * e.getSwingProgress() * (float) Math.PI) * -20.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(var * -20.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.rotate(var * -40.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.scale(0.4F, 0.4F, 0.4F);
                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                    GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                    break;
                }
                case "Tap": {
                    GL11.glTranslated(0.0D, 0.0D, 0.0D);
                    ItemRenderer.transformFirstPersonItem(0.0F, 0.0F);
                    GlStateManager.translate(-0.12F, 0.14f, 0.02F);
                    GlStateManager.rotate(-var * 75.0F / 2.0F, var / 2.0F, 1.0F, 4.0F);
                    GlStateManager.rotate(-var * 50.0F, 1.0F, var / 3.0F, -0.0F);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
                case "None": {
                    ItemRenderer.transformFirstPersonItem(e.getProgress(), 1.0F);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
                case "Gay": {
                    ItemRenderer.transformFirstPersonItem(e.getProgress(), e.getSwingProgress());
                    GL11.glTranslatef(-0.35F, 0.1F, 0.0F);
                    GL11.glTranslatef(-0.05F, -0.1F, 0.1F);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
            }

            e.setCancelled(true);
        }
    }
}