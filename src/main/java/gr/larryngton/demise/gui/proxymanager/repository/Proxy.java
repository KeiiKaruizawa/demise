package gr.larryngton.demise.gui.proxymanager.repository;

import com.google.gson.JsonObject;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.gui.proxymanager.ProxyManager;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.api.HttpUtils;
import gr.larryngton.demise.utils.render.GLUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.render.StencilUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Proxy implements InstanceAccess {
    private final ProxyRepositoryGUI repository;
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean selected;

    private volatile String countryCode;
    private volatile boolean countryResolveFailed;

    long lastClickTime;

    public Proxy(@NotNull ProxyRepositoryGUI repository, @NotNull String host, int port, @NotNull String username, @NotNull String password) {
        this.repository = repository;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        resolveCountry();
    }

    private static final Map<String, String> COUNTRY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, FlagTexture> FLAG_TEXTURES = new ConcurrentHashMap<>();
    private static final Set<String> FLAGS_LOADING = ConcurrentHashMap.newKeySet();

    private record FlagTexture(ResourceLocation location, int width, int height) {
        float aspect() {
            return width / (float) height;
        }
    }

    private void resolveCountry() {
        String cached = COUNTRY_CACHE.get(host);
        if (cached != null) {
            this.countryCode = cached;
            loadFlagTexture(cached);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String ip = InetAddress.getByName(host).getHostAddress();

                try (Response response = HttpUtils.sendSync(it -> it.url("https://ipinfo.io/" + ip + "/json"))) {
                    JsonObject json = HttpUtils.bodyGson(JsonObject.class).apply(response);

                    if (json.has("country") && !json.get("country").isJsonNull()) {
                        String code = json.get("country").getAsString().toUpperCase(Locale.ROOT);

                        COUNTRY_CACHE.put(host, code);
                        this.countryCode = code;

                        loadFlagTexture(code);
                        return;
                    }
                }
            } catch (Exception e) {
                Demise.LOGGER.warn("Failed to resolve country for proxy {}", host, e);
            }

            this.countryResolveFailed = true;
        }, HttpUtils.IO_WORKER);
    }

    private static void loadFlagTexture(@NotNull String countryCode) {
        if (FLAG_TEXTURES.containsKey(countryCode) || !FLAGS_LOADING.add(countryCode)) return;

        CompletableFuture.runAsync(() -> {
            try {
                try (Response response = HttpUtils.sendSync(it ->
                        it.url("https://flagcdn.com/h80/" + countryCode.toLowerCase(Locale.ROOT) + ".png"))) {

                    if (!response.isSuccessful() || response.body() == null) return;

                    BufferedImage image = ImageIO.read(response.body().byteStream());
                    if (image == null) return;

                    int w = image.getWidth();
                    int h = image.getHeight();

                    Minecraft mc = Minecraft.getMinecraft();
                    mc.addScheduledTask(() -> {
                        ResourceLocation location = new ResourceLocation("demise", "flags/" + countryCode.toLowerCase(Locale.ROOT));
                        mc.getTextureManager().loadTexture(location, new DynamicTexture(image));
                        FLAG_TEXTURES.put(countryCode, new FlagTexture(location, w, h));
                    });
                }
            } catch (Exception e) {
                Demise.LOGGER.warn("Failed to load flag texture for {}", countryCode, e);
            } finally {
                FLAGS_LOADING.remove(countryCode);
            }
        }, HttpUtils.IO_WORKER);
    }

    private static void drawFlag(@NotNull FlagTexture flag, float x, float y, float width, float height, float radius) {
        StencilUtils.initStencilToWrite();
        RenderUtils.drawRoundedRect(x, y, width, height, radius, -1);
        StencilUtils.readStencilBuffer(1);
        RenderUtils.resetColor();
        GLUtils.startBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(flag.location());
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, flag.width(), flag.height(), width, height, flag.width(), flag.height());
        GLUtils.endBlend();
        StencilUtils.uninitStencilBuffer();
    }

    protected boolean mouseClicked(float width, float x, float y, int mouseX, int mouseY) {
        if (!MouseUtils.isHovered(x, y, width, ROW_HEIGHT, mouseX, mouseY)) return false;

        if (Minecraft.getSystemTime() - lastClickTime < 250L) {
            connect();
        } else {
            select();
        }

        this.lastClickTime = Minecraft.getSystemTime();
        return true;
    }

    public static final float ROW_WIDTH = 160;
    public static final float ROW_HEIGHT = 42;

    private static final float ICON_SLOT_WIDTH = 36;
    private static final float ICON_HEIGHT = 24;
    private static final float ICON_RADIUS = 6;

    public void drawProxy(float width, float x, float y, int mouseX, int mouseY) {
        boolean hovered = MouseUtils.isHovered(x, y, width, ROW_HEIGHT, mouseX, mouseY);

        int bgColor = isSelected() ? SELECTED_COLOR : (hovered ? HOVER_COLOR : DEFAULT_COLOR);
        RectUtils.drawRound(x, y, width, ROW_HEIGHT, 10, new Color(bgColor, true));

        float iconSlotX = x + 9;
        float iconSlotY = y + (ROW_HEIGHT - ICON_HEIGHT) / 2f;

        FlagTexture flag = countryCode != null ? FLAG_TEXTURES.get(countryCode) : null;

        if (flag != null) {
            float flagHeight = ICON_HEIGHT;
            float flagWidth = flagHeight * flag.aspect();

            if (flagWidth > ICON_SLOT_WIDTH) {
                flagWidth = ICON_SLOT_WIDTH;
                flagHeight = flagWidth / flag.aspect();
            }

            float flagX = iconSlotX + (ICON_SLOT_WIDTH - flagWidth) / 2f;
            float flagY = iconSlotY + (ICON_HEIGHT - flagHeight) / 2f;

            drawFlag(flag, flagX, flagY, flagWidth, flagHeight, ICON_RADIUS);
        } else {
            RectUtils.drawRound(iconSlotX + (ICON_SLOT_WIDTH - ICON_HEIGHT) / 2f, iconSlotY, ICON_HEIGHT, ICON_HEIGHT, ICON_RADIUS, new Color(255, 255, 255, 25));
        }

        float textX = iconSlotX + ICON_SLOT_WIDTH + 8;
        float maxTextWidth = x + width - 10 - textX;

        String label = host + ":" + port;
        var hostFont = Fonts.interSemiBold.get(14);

        while (hostFont.getStringWidth(label) > maxTextWidth && label.length() > 4) {
            label = label.substring(0, label.length() - 4) + "...";
        }

        String subLabel;
        if (countryCode != null) {
            subLabel = countryCode;
        } else if (countryResolveFailed) {
            subLabel = "Unknown location";
        } else {
            subLabel = "Resolving...";
        }

        var subFont = Fonts.interMedium.get(12);

        float blockHeight = hostFont.getHeight() + 4 + subFont.getHeight();
        float blockY = y + (ROW_HEIGHT - blockHeight) / 2f;

        hostFont.drawString(label, textX, blockY, TEXT_SELECTED_COLOR);
        subFont.drawString(subLabel, textX, blockY + hostFont.getHeight() + 4, new Color(255, 255, 255, 130).getRGB());
    }

    public void connect() {
        ProxyManager.INSTANCE.setActiveProxy(this);
        ProxyManager.INSTANCE.setEnabled(true);

        Demise.INSTANCE.getNotificationManager().post(NotificationType.SUCCESS, "Proxy set to " + host + ":" + port, new ScaledResolution(mc));
    }

    public void select() {
        Proxy selected = repository.getProxies().stream().filter(Proxy::isSelected).findAny().orElse(null);
        if (selected != null) selected.setSelected(false);

        setSelected(true);
    }

    private static final int DEFAULT_COLOR = Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor();
    private static final int SELECTED_COLOR = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor()).darker().getRGB();
    private static final int HOVER_COLOR = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor()).brighter().getRGB();
    private static final int TEXT_SELECTED_COLOR = new Color(198, 198, 198).getRGB();

    @NotNull
    public static Proxy fromNBT(ProxyRepositoryGUI gui, @NotNull NBTTagCompound tagCompound) {
        return new Proxy(gui,
                tagCompound.getString("host"),
                tagCompound.getInteger("port"),
                tagCompound.getString("username"),
                tagCompound.getString("password"));
    }

    public NBTBase asNBTCompound() {
        NBTTagCompound compound = new NBTTagCompound();

        compound.setString("host", host);
        compound.setInteger("port", port);
        compound.setString("username", username);
        compound.setString("password", password);

        return compound;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}