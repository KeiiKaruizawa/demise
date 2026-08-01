package gr.larryngton.demise.gui.mainmenu;

import com.google.common.collect.Lists;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Shaders;
import gr.larryngton.demise.gui.button.MenuButton;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.render.shader.ShaderManager;
import gr.larryngton.demise.utils.render.shader.impl.Blur;
import gr.larryngton.demise.utils.render.shader.impl.MenuBackground;
import gr.larryngton.demise.utils.render.shader.impl.Shadow;
import gr.larryngton.demise.utils.sound.SoundUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.Charsets;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class GuiMainMenu extends GuiScreen {
    private final List<MenuButton> buttons = List.of(
            new MenuButton("Singleplayer"),
            new MenuButton("Multiplayer"),
            new MenuButton("Alt manager"),
            new MenuButton("Proxy manager"),
            new MenuButton("Options"),
            new MenuButton("Quit")
    );

    public static boolean fade;
    private int alpha = 255;
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil splashTimer = new TimerUtil();
    private double fontScale;
    private int alpha1 = 0;
    private final TimerUtil splashTextTimer = new TimerUtil();
    private String splashText = "missingno";

    @Override
    public void initGui() {
        updateSplashText();

        buttons.forEach(MenuButton::initGui);
        if (fade) {
            fontScale = ((new ScaledResolution(mc).getScaledWidth() * 2.927400468384075f) / 100) * 4;
            splashTimer.reset();
            alpha1 = 0;
        } else {
            fontScale = 38;
            alpha1 = 208;
        }

        // i like this but Arlington does not - n
//        SoundUtil.playSound("demise.mainMenu");

    }

    private void updateSplashText() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int i = calendar.get(Calendar.DATE);
        int j = calendar.get(Calendar.MONTH) + 1;

        if (i == 27 && j == 6) {
            splashText = "Happy birthday Miko!";
        }
        else if (i == 8 && j == 3) {
            splashText = "Happy birthday Karuizawa!";
        } else {
//            Random rand = new Random(i ^ j);
            Random rand = new Random();

            BufferedReader bufferedreader = null;

            try {
                List<String> list = Lists.newArrayList();

                bufferedreader = new BufferedReader(
                        new InputStreamReader(
                                Minecraft.getMinecraft().getResourceManager()
                                        .getResource(new ResourceLocation("demise/splashes.txt"))
                                        .getInputStream(),
                                Charsets.UTF_8
                        )
                );

                String s;
                while ((s = bufferedreader.readLine()) != null) {
                    s = s.trim();

                    if (!s.isEmpty()) {
                        list.add(s);
                    }
                }

                if (!list.isEmpty()) {
                    do {
                        splashText = list.get(rand.nextInt(list.size()));
                    } while (splashText.isEmpty());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bufferedreader != null) {
                    try {
                        bufferedreader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MenuBackground.draw(Demise.INSTANCE.getStartTimeLong());

        float buttonWidth = 120;

        float buttonHeight = switch (Demise.INSTANCE.getClientSettings().buttonStyle.get()) {
            case "Vanilla" -> 20;
            case "Custom" -> 23;
            default -> 0;
        };

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).blur.get()) {
            MenuButton.shader = true;
            Blur.startBlur();
            renderButtons(buttonWidth, buttonHeight, mouseX, mouseY);
            Blur.endBlur(ShaderManager.blurRadius, 1);
        }

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).shadow.get()) {
            MenuButton.shader = true;
            Demise.INSTANCE.getShaderManager().stencilFramebuffer = RenderUtils.createFrameBuffer(Demise.INSTANCE.getShaderManager().stencilFramebuffer, true);
            Demise.INSTANCE.getShaderManager().stencilFramebuffer.framebufferClear();
            Demise.INSTANCE.getShaderManager().stencilFramebuffer.bindFramebuffer(true);
            renderButtons(buttonWidth, buttonHeight, mouseX, mouseY);
            Demise.INSTANCE.getShaderManager().stencilFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(Demise.INSTANCE.getShaderManager().stencilFramebuffer.framebufferTexture, ShaderManager.shadowRadius, 1);
        }

        MenuButton.shader = false;
        renderButtons(buttonWidth, buttonHeight, mouseX, mouseY);

        if (fade) {
            if (!splashTimer.hasTimeElapsed(500)) {
                fontScale = mc.drawSplashScreen(false, 255, 50);
            } else {
                mc.drawSplashScreen(true, alpha, (float) fontScale);
                fontScale = MathUtils.interpolateNoUpdateCheck(fontScale, 38, 0.25f);

                if (timer.hasTimeElapsed(10)) {
                    alpha -= 5;

                    if (alpha < 0) {
                        fade = false;
                    }

                    timer.reset();
                }
            }
        } else {
            float x = (width / 2f) - Fonts.urbanist.get((float) fontScale).getStringWidth(Demise.INSTANCE.getClientName()) / 2f;
            float y = (height / 2f) + 20 - (buttons.size() * buttonHeight) / 2f;
            Fonts.urbanist.get((float) fontScale).drawString(Demise.INSTANCE.getClientName(), x, y, new Color(255, 255, 255, 208).getRGB());

            float x1 = (width / 2f) - Fonts.interRegular.get((float) fontScale / 2).getStringWidth(splashText) / 2f;
            float y1 = ((height / 2f) + (buttonHeight + 6) * (buttons.size() + 1) - (buttons.size() * buttonHeight) / 2f) + Fonts.interBold.get(36).getHeight();

            if (splashTextTimer.hasTimeElapsed((long) 16.67) && alpha1 < 208) {
                alpha1 = Math.min(alpha1 + 5, 208);
                splashTextTimer.reset();
            }

            Fonts.interRegular.get((float) fontScale / 2).drawString(splashText, x1, y1, new Color(255, 255, 255, alpha1).getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderButtons(float buttonWidth, float buttonHeight, int mouseX, int mouseY) {
        int count = 20;

        for (MenuButton button : buttons) {
            button.x = width / 2f - buttonWidth / 2f;
            button.y = ((height / 2f) + count - (buttons.size() * buttonHeight) / 2f) + Fonts.interBold.get(36).getHeight() + 2;
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.radius = 7;
            button.clickAction = () -> {
                switch (button.text) {
                    case "Singleplayer" -> mc.displayGuiScreen(new GuiSelectWorld(this));
                    case "Multiplayer" -> mc.displayGuiScreen(new GuiMultiplayer(this));
                    case "Alt manager" -> mc.displayGuiScreen(Demise.INSTANCE.getAltRepositoryGUI());
                    case "Proxy manager" -> mc.displayGuiScreen(Demise.INSTANCE.getProxyRepositoryGUI());
                    case "Options" -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                    case "Quit" -> mc.shutdown();
                }
            };

            button.drawScreen(mouseX, mouseY);
            count += (int) (buttonHeight + 6);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }
}