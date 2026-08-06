package gr.larryngton.demise.gui.ingame;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.render.ChatGUIEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.impl.visual.CustomWidgetsModule;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.mainmenu.GuiMainMenu;
import gr.larryngton.demise.gui.widget.impl.ModuleListWidget;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//todo port the rest to widgets
public class CustomWidgets implements InstanceAccess {
    public static ScaledResolution sr;
    public static float f;
    public static ScoreObjective scoreObjective;
    public static boolean fade = false;
    private float alpha = 255;
    private final TimerUtil fadeTimer = new TimerUtil();
    private final CustomWidgetsModule customWidgetsModule = Demise.INSTANCE.getModuleManager().getModule(CustomWidgetsModule.class);
    private float interpolatedScoreY;
    private float interpolatedScorebgY;
    private float interpolatedScoreHeight;
    private float interpolatedScoreWidth;
    public static float animY = 25;

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (!customWidgetsModule.isEnabled()) {
            animY = 0;
            return;
        }

        if (customWidgetsModule.scoreboard.get()) {
            drawScoreboard(scoreObjective, sr, false);
        }

        if (customWidgetsModule.worldFade.get()) {
            if (fade) {
                RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, (int) alpha).getRGB());

                if (fadeTimer.hasTimeElapsed(10)) {
                    alpha -= 0.75f;
                    fadeTimer.reset();
                }

                if (alpha < 0) {
                    alpha = 255;
                    fade = false;
                }
            } else {
                alpha = 255;
            }
        }

        if (customWidgetsModule.chat.get()) {
            if (!GuiNewChat.getChatOpen()) {
                animY = MathUtils.interpolate(animY, 25, 0.25F);
            } else {
                animY = MathUtils.interpolate(animY, 0, 0.25F);
            }
        } else {
            animY = 0;
        }

        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
            if (!customWidgetsModule.isEnabled()) {
                return;
            }

            if (customWidgetsModule.chat.get()) {
                if (GuiNewChat.getChatOpen()) {
                    drawChatScreen(true);
                }
            }

            if (customWidgetsModule.scoreboard.get()) {
                drawScoreboard(scoreObjective, sr, true);
            }
        });
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (!(mc.currentScreen instanceof GuiMainMenu)) {
            GuiMainMenu.fade = false;
        }
    }

    public static final Pattern LINK_PATTERN = Pattern.compile("(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[A-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)");

    private void drawScoreboard(ScoreObjective objective, ScaledResolution scaledRes, boolean shader) {
        if (objective == null) {
            return;
        }

        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> collection = scoreboard.getSortedScores(objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(collection, p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#")));

        if (list.size() > 15) {
            collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
        } else {
            collection = list;
        }

        int i = Fonts.interRegular.get(15).getStringWidth(objective.getDisplayName());

        for (Score score : collection) {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());
            i = Math.max(i, Fonts.interRegular.get(15).getStringWidth(s));
        }

        int i1 = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
        int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3;
        int k1 = 3;
        int l1 = scaledRes.getScaledWidth() - i - k1 - 10;
        int j = 0;

        int totalHeight = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
        int topY = j1 - totalHeight;

        float x = l1 - 2;
        float y = j1;

        float width = (scaledRes.getScaledWidth() - k1 + 2) - (l1 - 2) + 6 - 10;
        float bgY = y - i1 - Fonts.interRegular.get(15).getHeight() - 7;
        float height = j1 - (topY - mc.fontRendererObj.FONT_HEIGHT - 1) + 7;

        Interface anInterface = getModule(Interface.class);

        if (anInterface.elements.isEnabled("Module list")) {
            int scoreboardTop = (int) (j1 - height);

            int moduleListBottom = (int) (ModuleListWidget.currY + ModuleListWidget.getEnabledModules().size() * ModuleListWidget.getModuleHeight());

            boolean overlapsVertically = moduleListBottom > scoreboardTop - 15;
            boolean overlapsHorizontally = ModuleListWidget.currX > l1 - i - 25;

            if (overlapsVertically && overlapsHorizontally) {
                int shift = (moduleListBottom - scoreboardTop) + 10;
                y += shift;
                bgY += shift;
            }
        }

        if (!shader) {
            interpolatedScorebgY = MathUtils.interpolate(interpolatedScorebgY, bgY, 0.25f);
            interpolatedScoreY = MathUtils.interpolate(interpolatedScoreY, y, 0.25f);
            interpolatedScoreHeight = MathUtils.interpolate(interpolatedScoreHeight, height, 0.25f);
            interpolatedScoreWidth = MathUtils.interpolate(interpolatedScoreWidth, width, 0.25f);
        }

        y = interpolatedScoreY;

        if (!shader) {
            RectUtils.drawRound(x - 3, interpolatedScorebgY, interpolatedScoreWidth, interpolatedScoreHeight, 7, new Color(getModule(Interface.class).bgColor(), true));

            for (Score score1 : collection) {
                ++j;

                y -= mc.fontRendererObj.FONT_HEIGHT;

                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
                if (gr.larryngton.demise.features.modules.impl.visual.NickHider.INSTANCE != null) {
                    s1 = gr.larryngton.demise.features.modules.impl.visual.NickHider.INSTANCE.apply(s1);
                }

                final Matcher linkMatcher = LINK_PATTERN.matcher(s1);

                if (j <= 2 && (linkMatcher.find() || s1.contains("net") || s1.contains("org") || s1.contains("com"))) {
                    s1 = "gr.demise";
                    Fonts.interRegular.get(15).drawGradientWithShadow(s1, l1, y, (index) -> new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(index)));
                } else {
                    Fonts.interRegular.get(15).drawStringWithShadow(s1, l1, y, -1);
                }

                if (j == collection.size()) {
                    String s3 = objective.getDisplayName();
                    Fonts.interRegular.get(15).drawStringWithShadow(s3, l1 + i / 2f - Fonts.interRegular.get(15).getStringWidth(s3) / 2f, y - mc.fontRendererObj.FONT_HEIGHT, -1);
                }
            }
        } else {
            RectUtils.drawShaderRound(x - 3, interpolatedScorebgY, interpolatedScoreWidth, interpolatedScoreHeight, 7, Color.black);
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        fade = true;
    }

    private void drawChatScreen(boolean shader) {
//        float width = MathHelper.ceiling_float_int((float) GuiNewChat.getChatWidth() / GuiNewChat.getChatScale()) + 4;
        float width = sr.getScaledWidth() - 16;
        float height = 12;
        float x = 8;
        float y = sr.getScaledHeight() - height - x;
        y += animY;

        if (!shader) {
            RectUtils.drawRound(x, y, width, height, 5, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));
            Demise.INSTANCE.getChatTabsHandler().render();
        } else {
            RectUtils.drawShaderRound(x, y, width, height, 5, Color.black);
        }
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent e) {
        if (!customWidgetsModule.isEnabled()) {
            return;
        }

        if (customWidgetsModule.chat.get()) {
            drawChatScreen(false);
        }
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }
}