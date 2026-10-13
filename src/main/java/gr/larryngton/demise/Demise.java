package gr.larryngton.demise;

import de.florianmichael.viamcp.ViaMCP;
import gr.larryngton.demise.events.EventManager;
import gr.larryngton.demise.features.anticheat.AntiCheat;
import gr.larryngton.demise.features.anticheat.CheckManager;
import gr.larryngton.demise.features.command.CommandManager;
import gr.larryngton.demise.features.config.ConfigManager;
import gr.larryngton.demise.features.creative.CustomCreativeTab;
import gr.larryngton.demise.features.friend.FriendManager;
import gr.larryngton.demise.features.modules.ModuleManager;
import gr.larryngton.demise.features.modules.impl.client.ClientSettings;
import gr.larryngton.demise.features.statistics.StatManager;
import gr.larryngton.demise.gui.altmanager.repository.AltRepositoryGUI;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.ingame.ChatTabsHandler;
import gr.larryngton.demise.gui.ingame.CustomWidgets;
import gr.larryngton.demise.gui.notification.NotificationManager;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.gui.proxymanager.repository.ProxyRepositoryGUI;
import gr.larryngton.demise.gui.widget.WidgetManager;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.misc.DiscordInfo;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import gr.larryngton.demise.utils.packet.BadPacketsComponent;
import gr.larryngton.demise.utils.packet.BlinkComponent;
import gr.larryngton.demise.utils.packet.LagUtils;
import gr.larryngton.demise.utils.player.trackers.CPSTracker;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickHandler;
import gr.larryngton.demise.utils.player.handlers.clicking.ScaffoldClickHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.aimpoint.JitterHandler;
import gr.larryngton.demise.utils.player.trackers.LagTracker;
import gr.larryngton.demise.utils.player.trackers.PingTracker;
import gr.larryngton.demise.utils.render.shader.ShaderManager;
import gr.larryngton.demise.utils.render.video.VideoComponent;
import gr.larryngton.demise.utils.sound.SoundUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerHistory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjglx.LWJGLUtil;
import org.lwjglx.opengl.Display;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

@Getter
public class Demise {
    public static final Logger LOGGER = LogManager.getLogger(Demise.class);
    public static final Demise INSTANCE = new Demise();
    public static ServerHistory serverHistory = new ServerHistory(null, null);

    @Setter
    public String clientName = "demise";
    @Setter
//    public String version = "Gūji";
//    public String version = "Miko";
    public String version = "omega";
    public final String cloud = "https://KeiiKaruizawa.github.io/demise-cloud/";
    private final ClientSettings clientSettings = new ClientSettings();

    private final File mainDir = new File(Minecraft.getMinecraft().mcDataDir, clientName);
    private final File configDir = new File(new File(Minecraft.getMinecraft().mcDataDir, clientName), "configs");

    private EventManager eventManager;
    private NotificationManager notificationManager;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private WidgetManager widgetManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private PanelGui panelGui;
    private AltRepositoryGUI altRepositoryGUI;
    private ProxyRepositoryGUI proxyRepositoryGUI;
    private CheckManager checkManager;
    private DiscordInfo discordInfo;
    private ShaderManager shaderManager;
    private StatManager statManager;
    private ChatTabsHandler chatTabsHandler;
    private CPSTracker CPSTracker;
    private PingTracker pingTracker;
    private LagTracker lagTracker;
    private AntiCheat antiCheat;

    private int startTime;
    private long startTimeLong;

    private Path dataFolder;

    private CustomCreativeTab creativeTab;

    public boolean legit;

    public void hotReload() {
        new Thread(() -> {
            onStop();
            Minecraft.getMinecraft().addScheduledTask(() -> init(true));
        }).start();
    }

    public void toggleLegit() {
        legit = !legit;
        hotReload();
    }

    public ShaderManager getShaderManager() {
        if (shaderManager == null) {
            shaderManager = new ShaderManager();
        }

        return shaderManager;
    }

    public ModuleManager getModuleManager() {
        if (moduleManager == null) {
            moduleManager = new ModuleManager();
        }

        return moduleManager;
    }

    public void init(boolean reload) {
        setupMainDirectory();
        initializeManagers();
        registerEventHandlers();

        if (!reload) {
            initializeStartTime();
        }

        initializeViaMCP();
        handleFastRender();
        setupDiscordRPC();

        // linux multiplayer fix
        if (LWJGLUtil.getPlatform() == 1) {
            Minecraft.getMinecraft().gameSettings.useNativeTransport = false;
        }

        VideoComponent.ensureVideoExists();
        VideoComponent.startVideoPlayback();

        dataFolder = Paths.get(Minecraft.getMinecraft().mcDataDir.getAbsolutePath()).resolve(clientName);
        LOGGER.info("{} {} initialized successfully.", clientName, version);

        if (!reload) {
            SoundUtil.playSound("demise.boot");
        }
    }

    @SuppressWarnings("all")
    private void setupMainDirectory() {
        if (!mainDir.exists()) {
            mainDir.mkdir();
        }

        if (!configDir.exists()) {
            configDir.mkdir();
        }

        this.dataFolder = Paths.get(Minecraft.getMinecraft().mcDataDir.getAbsolutePath()).resolve(clientName);
    }

    public void setDisplayTitle() {
        Display.setTitle(clientName);
        LOGGER.info("Display title set to: {}", clientName);
    }

    private void initializeManagers() {
        eventManager = new EventManager();
        notificationManager = new NotificationManager();
        moduleManager = new ModuleManager();
        checkManager = new CheckManager();
        widgetManager = new WidgetManager();
        configManager = new ConfigManager();
        commandManager = new CommandManager();
        friendManager = new FriendManager();
        panelGui = new PanelGui();
        altRepositoryGUI = new AltRepositoryGUI(this);
        proxyRepositoryGUI = new ProxyRepositoryGUI(this);
        creativeTab = new CustomCreativeTab();
        shaderManager = new ShaderManager();
        statManager = new StatManager();
        chatTabsHandler = new ChatTabsHandler();
        CPSTracker = new CPSTracker();
        pingTracker = new PingTracker();
        lagTracker = new LagTracker();
        antiCheat = new AntiCheat();
    }

    private void registerEventHandlers() {
        eventManager.register(new LagUtils());
        eventManager.register(new BadPacketsComponent());
        eventManager.register(new BlinkComponent());
        eventManager.register(new SpoofSlotUtils());
        eventManager.register(new ChatTabsHandler());
        eventManager.register(new CustomWidgets());
        eventManager.register(new ClickHandler());
        eventManager.register(new ScaffoldClickHandler());
        eventManager.register(new MathUtils());
        eventManager.register(new RotationHandler());
        eventManager.register(new CPSTracker());
        eventManager.register(new JitterHandler());
        eventManager.register(new PingTracker());
        eventManager.register(new LagTracker());

        // needs to be initialized like this else shaders will break
        eventManager.register(shaderManager);

        LOGGER.info("Event handlers registered.");
    }

    private void initializeStartTime() {
        startTime = (int) System.currentTimeMillis();
        startTimeLong = System.currentTimeMillis();
        LOGGER.info("Start time initialized: {} ms", startTime);
    }

    private void initializeViaMCP() {
        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();
        LOGGER.info("ViaMCP initialized.");
    }

    private void setupDiscordRPC() {
        try {
            discordInfo = new DiscordInfo();
            discordInfo.init();
            LOGGER.info("Discord Rich Presence initialized.");
        } catch (Throwable throwable) {
            LOGGER.error("Failed to set up Discord RPC.", throwable);
        }
    }

    private void handleFastRender() {
        if (Minecraft.getMinecraft().gameSettings.ofFastRender) {
            notificationManager.post(NotificationType.INFO, "Fast Rendering has been disabled", "due to compatibility issues", new ScaledResolution(Minecraft.getMinecraft()));
            Minecraft.getMinecraft().gameSettings.ofFastRender = false;
            LOGGER.info("Fast Rendering was disabled due to compatibility issues.");
        }
    }

    public void onStop() {
        discordInfo.stop();
        configManager.saveConfigs();
        statManager.onStop();
        LOGGER.info("All configurations saved.");
    }

    public static class HWID {
        public static String getHWID() {
            try {
                String toEncrypt = System.getenv("COMPUTERNAME") + System.getProperty("user.name") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL");
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(toEncrypt.getBytes());
                StringBuilder hexString = new StringBuilder();

                byte[] byteData = md.digest();

                for (byte aByteData : byteData) {
                    String hex = Integer.toHexString(0xff & aByteData);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }

                return hexString.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        }
    }
}