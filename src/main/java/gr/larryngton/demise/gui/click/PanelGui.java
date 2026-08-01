package gr.larryngton.demise.gui.click;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.impl.render.RenderGuiEvent;
import gr.larryngton.demise.features.modules.ModuleCategory;
import gr.larryngton.demise.features.modules.impl.visual.ClickGUI;
import gr.larryngton.demise.gui.click.categories.*;
import gr.larryngton.demise.gui.click.components.ModuleComponent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjglx.input.Keyboard;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PanelGui extends GuiScreen {
    private final List<ICategory> categories = new ArrayList<>();
    public static boolean dragging;
    private float dragX, dragY;
    public static float posX = 255, posY = 120;
    public static float interpolatedScale;
    private boolean closing;
    private boolean buttonDown;
    private ICategory selected;

    public PanelGui() {
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);

        float height = 15 + Fonts.urbanist.get(35).getHeight();
        float hAdd = Fonts.interRegular.get(18).getHeight() + 7;

        for (ModuleCategory category : ModuleCategory.values()) {
            if (Demise.INSTANCE.getModuleManager().getModulesByCategory(category).isEmpty()) {
                continue;
            }

            categories.add(new Category(category, posX + 7, posY + height));
            height += hAdd;
        }

        categories.add(new ClientCategory(posX + 7, posY + height));
        categories.add(new ConfigCategory(posX + 7, posY + height + hAdd));
        categories.add(new InfoCategory(posX + 7, posY + height + (hAdd * 2)));
        categories.add(new SearchCategory());

        if (selected == null) {
            selected = categories.get(0);
        }
    }

    @Override
    public void initGui() {
        closing = false;
        interpolatedScale = 0;

        if (selected != null) {
            selected.initCategory();
        }

        SoundUtil.playSound("demise.open");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        interpolatedScale = MathUtils.interpolate(interpolatedScale, !closing ? 1 : 0, 0.25f);

        if (interpolatedScale < 0.01f && closing) {
            mc.displayGuiScreen(null);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtils.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f, interpolatedScale);

        if (dragging) {
            float deltaX = mouseX - (dragX + posX);
            float deltaY = mouseY - (dragY + posY);
            posX = mouseX - dragX;
            posY = mouseY - dragY;

            for (ICategory category : categories) {
                category.setX(category.getX() + deltaX);
                category.setY(category.getY() + deltaY);
            }
        }

        RectUtils.drawRound(posX, posY, 450, 300, 7, new Color(21, 21, 21, 204));

//        float thingX = posX + 67;
//        float thingY = posY + 12 + Fonts.urbanist.get(35).getHeight();
//        float thingWidth = 375;
//        float thingHeight = 255;
//        RectUtils.drawRound(thingX - 5, thingY - 5, thingWidth + 10, thingHeight + 10, 7, new Color(21, 21, 21, 25));

        float x = posX + 7;
        float y = posY + 7;

        Fonts.urbanist.get(35).drawString(Demise.INSTANCE.getClientName(), x, y, new Color(255, 255, 255, 208).getRGB());
        Fonts.urbanist.get(24).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + x, Fonts.urbanist.get(35).getHeight() + y - Fonts.urbanist.get(24).getHeight() * 1.1f, new Color(245, 245, 245, 208).getRGB());

        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        float calcWidth = 450 - watermarkWidth - 19;

        RectUtils.drawRound(posX + watermarkWidth + 13, posY + 7, calcWidth, 20, 7, new Color(0, 0, 0, 100));

        for (ICategory category : categories) {
            category.render();

            boolean hovered = category.isHovered(mouseX, mouseY);

            if (hovered && buttonDown) {
                if (selected != category) {
                    category.initCategory();
                }

                selected = category;
            }

            category.setSelected(selected != null && selected == category);
        }

        if (selected != null) {
            selected.drawScreen(mouseX, mouseY);
        }

        if (!(selected instanceof SearchCategory)) {
            Fonts.interRegular.get(18).drawString("Search...", posX + watermarkWidth + 18, posY + 7 + Fonts.interRegular.get(15).getHeight() - 2, new Color(147, 147, 147, 255).getRGB());
        }

        String str = "Total modules: " + Demise.INSTANCE.getModuleManager().getAllModules().size() + ", Total options: " + Demise.INSTANCE.getModuleManager().getAllModuleValues().size();

        Fonts.interRegular.get(14).drawString(str, posX + 450 - Fonts.interRegular.get(14).getStringWidth(str) - 3.5F, posY + 300.5f - Fonts.interRegular.get(14).getHeight(), new Color(255, 255, 255, 208).getRGB());
        Fonts.interRegular.get(14).drawString(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), posX + 3, posY + 300.5f - Fonts.interRegular.get(14).getHeight(), new Color(255, 255, 255, 208).getRGB());
        RenderUtils.scaleEnd();

        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
            ScaledResolution sr1 = new ScaledResolution(mc);
            RenderUtils.scaleStart(sr1.getScaledWidth() / 2f, sr1.getScaledHeight() / 2f, interpolatedScale);
            RectUtils.drawShaderRound(posX, posY, 450, 300, 7, Color.black);
            RenderUtils.scaleEnd();
        });

        buttonDown = false;
        Demise.INSTANCE.getEventManager().call(new RenderGuiEvent(RenderGuiEvent.State.POST));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            buttonDown = true;

            if (MouseUtils.isHovered(posX, posY, 450, 35, mouseX, mouseY)) {
                dragging = true;
                dragX = mouseX - posX;
                dragY = mouseY - posY;
            }
        }

        if (selected != null && !closing) {
            selected.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;

        if (selected != null && !closing) {
            selected.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (ModuleComponent.anyAwaitingKeyBind) {
            if (selected != null) {
                selected.keyTyped(typedChar, keyCode);
            }
            return;
        }

        if (keyCode == Demise.INSTANCE.getModuleManager().getModule(ClickGUI.class).getKeyBind()) {
            closing = !closing;
            SoundUtil.playSound(closing ? "demise.close" : "demise.open");
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            SoundUtil.playSound("demise.close");
            closing = true;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            selected = categories.get((categories.indexOf(selected) + 1) % categories.size());
            if (selected instanceof SearchCategory) {
                selected = categories.get((categories.indexOf(selected) + 1) % categories.size());
            }
            selected.initCategory();
        }

        if (selected != null && !closing) {
            selected.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}