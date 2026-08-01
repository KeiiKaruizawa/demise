package gr.larryngton.demise.gui.widget.impl;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.animations.ContinualAnimation;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.render.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SpotifyWidget extends Widget {
    private ResourceLocation texture;
    private BufferedImage lastImg;
    public ContinualAnimation progressAnimation = new ContinualAnimation();
    private Color lastAvgCol = Color.BLACK;
    private Color interpolatedCol = Color.BLACK;
    private final TimerUtil frameTimer = new TimerUtil();
    public final String[] currentTrack = new String[2];
    public BufferedImage currentTrackImage;
    private SpotifyAPI localApi;
    public float percentage;
    private float lastPosition;
    private long lastUpdateTime;
    public boolean playing;

    public SpotifyWidget() {
        super("Spotify");
        this.height = 37;
        this.width = 120;
    }

    @Override
    public void onEnable() {
        localApi = SpotifyAPIFactory.create();
        localApi.registerListener(new SpotifyListener() {
            @Override
            public void onConnect() {
                ChatUtils.sendMessageClient("Spotify | Successfully authorized");
            }

            @Override
            public void onTrackChanged(Track track) {
                currentTrack[0] = track.getName();
                currentTrack[1] = track.getArtist();

                if (track.getCoverArt() != null) {
                    currentTrackImage = track.getCoverArt();
                }
            }

            @Override
            public void onPositionChanged(int position) {
                if (!localApi.hasTrack()) {
                    return;
                }

                lastPosition = position;
                int length = localApi.getTrack().getLength();
                percentage = (100.0F / length * position) / 100;
            }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                playing = isPlaying;
            }

            @Override
            public void onSync() {
            }

            @Override
            public void onDisconnect(Exception exception) {
                ChatUtils.sendMessageClient("Spotify | Disconnected: " + exception.getMessage());
            }
        });

        localApi.initialize();
    }

    @Override
    public void onDisable() {
        localApi.stop();
        localApi = null;
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (playing) {
            if (lastUpdateTime == 0) {
                lastUpdateTime = System.currentTimeMillis();
            }

            lastPosition += System.currentTimeMillis() - lastUpdateTime;
            int length = localApi.getTrack().getLength();
            percentage = (100.0F / length * lastPosition) / 100;
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render() {
        width = Math.max(
                Math.max(
                        Fonts.interSemiBold.get(18).getStringWidth(currentTrack[0]),
                        Fonts.interSemiBold.get(13).getStringWidth(currentTrack[1])
                ) + 42,
                120
        );

        RectUtils.drawRound(renderX, renderY, width, height, 7, new Color(setting.bgColor(), true));

        Fonts.interSemiBold.get(18).drawString(currentTrack[0], renderX + 37, renderY + 6, -1);
        Fonts.interSemiBold.get(13).drawString(currentTrack[1], renderX + 37, renderY + 17, Color.lightGray.getRGB());

        float width = this.width - 43F;
        progressAnimation.animate(width * MathHelper.clamp_float(percentage, 0, 1), 50);
        RectUtils.drawRound(renderX + 38.5F, renderY + 28, width, 4, 2, new Color(0, 0, 0, 150));
        RectUtils.drawGradientHorizontal(renderX + 38.5F, renderY + 28, progressAnimation.getOutput(), 4, 2, new Color(setting.color()), new Color(setting.color((int) progressAnimation.getOutput())));

        float x = renderX + 2.5f;
        float y = renderY + 2.5f;
        float size = 32;
        float radius = 10;

        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RectUtils.drawShaderRound(renderX, renderY, this.width, height, 7, Color.black));

        if (currentTrackImage != null) {
            if (currentTrackImage != lastImg) {
                mc.addScheduledTask(() -> {
                    DynamicTexture dynamicTexture = new DynamicTexture(currentTrackImage);
                    texture = mc.getTextureManager().getDynamicTextureLocation("custom_external", dynamicTexture);
                });
                lastImg = currentTrackImage;

                lastAvgCol = ColorUtils.getAverageColor(currentTrackImage);
            }

            if (texture == null) return;

            if (frameTimer.hasTimeElapsed((long) 16.67)) {
                interpolatedCol = ColorUtils.interpolateColorC(interpolatedCol, playing ? lastAvgCol : new Color(interpolatedCol.getRed(), interpolatedCol.getGreen(), interpolatedCol.getBlue(), 0), 0.1F);
                frameTimer.reset();
            }

            StencilUtils.initStencilToWrite();
            RenderUtils.drawRoundedRect(x, y, size, size, radius, -1);
            StencilUtils.readStencilBuffer(1);
            RenderUtils.color(-1);
            GLUtils.startBlend();
            mc.getTextureManager().bindTexture(texture);
            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 300, 300, size, size, 300, 300);
            GLUtils.endBlend();
            StencilUtils.uninitStencilBuffer();

            Demise.INSTANCE.getShaderManager().addToBloomQueue(() -> {
                RectUtils.drawGradientHorizontal(renderX + 38.5F, renderY + 28, progressAnimation.getOutput(), 4, 2, new Color(setting.color(), false), new Color(setting.color((int) progressAnimation.getOutput()), false));
                RenderUtils.drawRoundedRect(x, y, size, size, radius, interpolatedCol.getRGB());
            });
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Spotify");
    }
}