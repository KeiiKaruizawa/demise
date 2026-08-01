package gr.larryngton.demise.utils.misc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.impl.misc.Opsec;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.api.HttpUtils;
import gr.larryngton.demise.utils.fun.ShitUtils;
import gr.larryngton.demise.utils.fun.WinUtils;
import lombok.Getter;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiSelectWorld;

import java.io.IOException;

public class DiscordInfo implements InstanceAccess {
    private boolean running = true;
    private long timeElapsed = 0;
    @Getter
    private String name = "null";
    @Getter
    private String id;
    @Getter
    private String smallImageText;

    private int getTotal() {
        return INSTANCE.getModuleManager().getModules().size();
    }

    private long getCount() {
        return INSTANCE.getModuleManager().getModules().stream().filter(Module::isEnabled).count();
    }

    public void init() {
        this.timeElapsed = System.currentTimeMillis();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().setReadyEventHandler(discordUser -> {
            Demise.LOGGER.info("[Discord] Connected to user {}", discordUser.username);

            if (discordUser.userId != null) {
                name = discordUser.username;

                // demise client protection leak
                // if (name.equals("khadowuwu") || name.equals("widget347")
                //        || name.equals("michaelstetson.")
                //        || name.equals("brianmosertheicetruckkiller")

                //) {
                //    try {
                //        if (System.getProperty("os.name").toLowerCase().equals("win")) {
                //            WinUtils.bsod();
                //        } else {
                //            ShitUtils.shutDown();
                //        }
                //    } catch (IOException e) {
                //        throw new RuntimeException(e);
                //    }
                // }
                // They can now use discord!
            } else {
                name = "null";
                System.exit(0);
            }
        }).build();

        DiscordRPC.discordInitialize("1452377935263563991", handlers, true);
        HttpUtils.IO_WORKER.execute(() -> {
            while (running) {
                if (mc.thePlayer != null) {
                    String srv;

                    if (!Demise.INSTANCE.getModuleManager().getModule(Opsec.class).isEnabled()) {
                        if (mc.isSingleplayer()) {
                            srv = "singleplayer";
                        } else if (mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy.net")) {
                            srv = "liquidproxy.net";
                        } else {
                            srv = mc.getCurrentServerData().serverIP;
                        }
                    } else {
                        // OPSEC METHOD
                        srv = "https://www.pixiv.net/en/users/110918912";
                    }

                    setInfo("Playing as " + mc.thePlayer.getName(), "on " + srv, true);
                    smallImageText = getCount() + " out of " + getTotal() + " modules enabled.";
                } else {
                    if (mc.currentScreen instanceof GuiSelectWorld) {
                        setInfo("Selecting world", "", false);
                    } else if (mc.currentScreen instanceof GuiMultiplayer) {
                        setInfo("Selecting server", "", false);
                    } else if (mc.currentScreen instanceof GuiDownloadTerrain) {
                        setInfo("Loading world", "", false);
                    } else {
                        setInfo("Idling", "", false);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                DiscordRPC.discordRunCallbacks();
            }
        });
    }

    private void setInfo(String line1, String line2, Boolean smallImage) {
        DiscordRichPresence.Builder rpc = new DiscordRichPresence.Builder(line2).setDetails(line1).setBigImage("logo", Demise.INSTANCE.getClientName() + " " + Demise.INSTANCE.getVersion());
        if (smallImage) {
            rpc.setSmallImage("closer", smallImageText);
        }
        rpc.setStartTimestamps(timeElapsed);
        DiscordRPC.discordUpdatePresence(rpc.build());
    }

    public void stop() {
        running = false;
        DiscordRPC.discordShutdown();
    }
}