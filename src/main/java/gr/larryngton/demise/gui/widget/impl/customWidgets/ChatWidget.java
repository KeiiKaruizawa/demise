package gr.larryngton.demise.gui.widget.impl.customWidgets;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.CustomWidgetsModule;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.awt.*;
import java.util.List;

import static net.minecraft.client.gui.Gui.drawRect;

public class ChatWidget extends Widget {
    private float interpolatedChatHeight;
    private float interpolatedChatY;
    private float interpolatedChatX;

    public ChatWidget() {
        super("Chat");
        this.x = 0.0052083335F;
        this.y = 0.96666664F;
        this.height = 13;
    }

    @Override
    public void render() {
        //todo: top alignment
        boolean flag = GuiNewChat.getChatOpen();
        float renderX = this.renderX + 2;
        float renderY = this.renderY + 11;

        if (flag) {
            renderX = 10;
            renderY = sr.getScaledHeight() - 33;
        }

        this.width = GuiNewChat.getChatWidth() + 4;

        List<ChatLine> drawnChatLines = Demise.INSTANCE.getChatTabsHandler().overrideChatLines(GuiNewChat.drawnChatLines);

        if (mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
            int i = GuiNewChat.getLineCount();
            int j = 0;
            int k = drawnChatLines.size();
            float f = mc.gameSettings.chatOpacity * 0.9F + 0.1F;

            if (k > 0) {
                float f1 = GuiNewChat.getChatScale();
                int l = MathHelper.ceiling_float_int((float) GuiNewChat.getChatWidth() / f1);

                GlStateManager.pushMatrix();

                if (interpolatedChatY == 0) {
                    interpolatedChatY = renderY;
                }

                if (interpolatedChatX == 0) {
                    interpolatedChatX = renderX;
                }

                interpolatedChatY = MathUtils.interpolate(interpolatedChatY, renderY, 0.25F);
                interpolatedChatX = MathUtils.interpolate(interpolatedChatX, renderX, 0.25F);

                GlStateManager.scale(f1, f1, 1.0F);

                float baseX = interpolatedChatX / f1;
                float baseY = interpolatedChatY / f1;

                for (int i1 = 0; i1 + GuiNewChat.scrollPos < drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = drawnChatLines.get(i1 + GuiNewChat.scrollPos);

                    if (chatline != null) {
                        int j1 = GuiIngame.getUpdateCounter() - chatline.getUpdatedCounter();

                        if (j1 < 200 || flag) {
                            ++j;
                        }
                    }
                }

                int chatBackgroundAlpha = (int) (255 * f);
                int radius = Math.min(k * 5, 7);
                int lineHeight = j * 9;

                if (lineHeight != 0) {
                    interpolatedChatHeight = MathUtils.interpolate(interpolatedChatHeight, lineHeight, 0.25F);
                } else {
                    interpolatedChatHeight = MathUtils.interpolate(interpolatedChatHeight, 0, 0.25F);
                    chatBackgroundAlpha = 0;
                }

                if (chatBackgroundAlpha > 3) {
                    RectUtils.drawRound(
                            baseX - 2,
                            baseY - interpolatedChatHeight - 2,
                            l + 4,
                            interpolatedChatHeight + 4,
                            radius,
                            new Color(getModule(Interface.class).bgColor(), true)
                    );

                    Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
                        RectUtils.drawShaderRound(
                                baseX - 2,
                                baseY - interpolatedChatHeight - 2,
                                l + 4,
                                interpolatedChatHeight + 4,
                                radius,
                                Color.black
                        );
                    });
                }

                for (int i1 = 0; i1 + GuiNewChat.scrollPos < drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = drawnChatLines.get(i1 + GuiNewChat.scrollPos);

                    if (chatline != null) {
                        int j1 = GuiIngame.getUpdateCounter() - chatline.getUpdatedCounter();

                        if (j1 < 200 || flag) {
                            double d0 = (double) j1 / 200.0D;
                            d0 = 1.0D - d0;
                            d0 = d0 * 10.0D;
                            d0 = MathHelper.clamp_double(d0, 0.0D, 1.0D);
                            d0 = d0 * d0;

                            int l1 = (int) (255.0D * d0);

                            if (flag) {
                                l1 = 255;
                            }

                            l1 = (int) ((float) l1 * f);

                            if (l1 > 3) {
                                int j2 = -i1 * 9;
                                String s = chatline.getChatComponent().getFormattedText();

                                GlStateManager.enableBlend();
                                mc.fontRendererObj.drawStringWithShadow(
                                        s,
                                        baseX,
                                        baseY + j2 - 8,
                                        16777215 + (l1 << 24)
                                );
                                GlStateManager.disableAlpha();
                                GlStateManager.disableBlend();
                            }
                        }
                    }
                }

                if (flag) {
                    int k2 = mc.fontRendererObj.FONT_HEIGHT;
                    GlStateManager.translate(-3.0F, 0.0F, 0.0F);

                    int l2 = k * k2 + k;
                    int i3 = j * k2 + j;
                    int j3 = GuiNewChat.scrollPos * i3 / k;
                    int k1 = i3 * i3 / l2;

                    if (l2 != i3) {
                        int k3 = j3 > 0 ? 170 : 96;
                        int l3 = GuiNewChat.isScrolled ? 13382451 : 3355562;

                        drawRect(0, -j3, 2, -j3 - k1, l3 + (k3 << 24));
                        drawRect(2, -j3, 1, -j3 - k1, 13421772 + (k3 << 24));
                    }
                }

                GlStateManager.popMatrix();
            }
        }
    }

    @Override
    public boolean shouldRender() {
        CustomWidgetsModule customWidgetsModule = Demise.INSTANCE.getModuleManager().getModule(CustomWidgetsModule.class);
        return customWidgetsModule.isEnabled() && customWidgetsModule.chat.get();
    }
}
