package gr.larryngton.demise.gui.proxymanager.repository;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.mainmenu.GuiMainMenu;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.gui.proxymanager.ProxyManager;
import gr.larryngton.demise.utils.render.shader.impl.MenuBackground;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProxyRepositoryGUI extends GuiScreen {
    @NonNull
    private final Demise demise;
    private final Logger logger = LogManager.getLogger();

    private final List<Proxy> proxies = new ArrayList<>();

    private static final float CELL_WIDTH = Proxy.ROW_WIDTH;
    private static final float CELL_HEIGHT = Proxy.ROW_HEIGHT;
    private static final float CELL_GAP = 6f;
    private static final int MAX_COLUMNS = 4;

    public ProxyRepositoryGUI(@NonNull Demise demise) {
        this.demise = demise;
        loadProxies();
    }

    @Override
    public void initGui() {
        float btnWidth = 75;
        float gap = 5;
        float pairWidth = btnWidth * 3 + gap * 2;
        float startX = width / 2f - pairWidth / 2f;
        float y = height - 25;

        this.buttonList.add(new GuiButton(0, startX, y, btnWidth, 20, "Add"));
        this.buttonList.add(new GuiButton(1, startX + btnWidth + gap, y, btnWidth, 20, "Remove"));
        this.buttonList.add(new GuiButton(2, startX + (btnWidth + gap) * 2, y, btnWidth, 20, "Disconnect"));
        this.buttonList.add(new GuiButton(69, GuiScreen.width - btnWidth - 5, y, btnWidth, 20, "Back"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(
                        new GuiAddProxy(this, "Add Proxy", "Add Proxy", (gui, proxy) -> {
                            addProxy(proxy);
                            saveProxies();

                            mc.displayGuiScreen(this);
                        }));
                break;
            case 1:
                removeCurrentProxy();
                break;
            case 2:
                ProxyManager.INSTANCE.setEnabled(false);
                ProxyManager.INSTANCE.setActiveProxy(null);

                Demise.INSTANCE.getNotificationManager().post(NotificationType.SUCCESS, "Disconnected from proxy", new ScaledResolution(mc));
                break;
            case 69:
                mc.displayGuiScreen(new GuiMainMenu());
                break;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (GuiButton button : this.buttonList) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                button.playPressSound(mc.getSoundHandler());
                actionPerformed(button);
                return;
            }
        }

        int totalProxies = this.proxies.size();
        int usedColumns = Math.min(MAX_COLUMNS, totalProxies);
        int rows = (int) Math.ceil((double) totalProxies / (double) MAX_COLUMNS);

        float totalGridWidth = usedColumns * CELL_WIDTH + (usedColumns - 1) * CELL_GAP;
        float totalGridHeight = rows * CELL_HEIGHT + (rows - 1) * CELL_GAP;

        float startX = ProxyRepositoryGUI.width / 2f - totalGridWidth / 2f;
        float startY = ProxyRepositoryGUI.height / 2f - totalGridHeight / 2f;

        for (int i = 0; i < this.proxies.size(); i++) {
            final Proxy proxy = this.proxies.get(i);

            int column = i % MAX_COLUMNS;
            int row = i / MAX_COLUMNS;

            float x = startX + column * (CELL_WIDTH + CELL_GAP);
            float y = startY + row * (CELL_HEIGHT + CELL_GAP);

            if (proxy.mouseClicked(CELL_WIDTH, x, y, mouseX, mouseY)) {
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MenuBackground.draw(Demise.INSTANCE.getStartTimeLong());

        String title = "Proxy Manager";
        Fonts.interSemiBold.get(22).drawString(title, (width - Fonts.interSemiBold.get(22).getStringWidth(title)) / 2.0F, 15 + 5, -1);

        if (this.proxies.isEmpty()) {
            String empty = "No proxies added yet";
            var emptyFont = Fonts.interMedium.get(14);
            emptyFont.drawString(empty, (width - emptyFont.getStringWidth(empty)) / 2.0F, height / 2f - 7, new Color(255, 255, 255, 120).getRGB());
        } else {
            drawProxies(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawProxies(int mouseX, int mouseY) {
        int totalProxies = this.proxies.size();
        int usedColumns = Math.min(MAX_COLUMNS, totalProxies);
        int rows = (int) Math.ceil((double) totalProxies / (double) MAX_COLUMNS);

        float totalGridWidth = usedColumns * CELL_WIDTH + (usedColumns - 1) * CELL_GAP;
        float totalGridHeight = rows * CELL_HEIGHT + (rows - 1) * CELL_GAP;

        float startX = ProxyRepositoryGUI.width / 2f - totalGridWidth / 2f;
        float startY = ProxyRepositoryGUI.height / 2f - totalGridHeight / 2f;

        for (int i = 0; i < totalProxies; i++) {
            final Proxy proxy = this.proxies.get(i);

            int column = i % MAX_COLUMNS;
            int row = i / MAX_COLUMNS;

            float x = startX + column * (CELL_WIDTH + CELL_GAP);
            float y = startY + row * (CELL_HEIGHT + CELL_GAP);

            proxy.drawProxy(CELL_WIDTH, x, y, mouseX, mouseY);
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    @Nullable
    public Proxy getSelectedProxy() {
        return this.proxies.stream().filter(Proxy::isSelected).findAny().orElse(null);
    }

    public void addProxy(@NonNull Proxy proxy) {
        this.proxies.add(proxy);
    }

    public void removeProxy(@NonNull Proxy proxy) {
        this.proxies.remove(proxy);
    }

    private void removeCurrentProxy() {
        Proxy selected = getSelectedProxy();
        if (selected == null) return;

        removeProxy(selected);
        saveProxies();
    }

    public List<Proxy> getProxies() {
        return this.proxies;
    }

    public void loadProxies() {
        this.proxies.clear();

        try {
            final NBTTagCompound tagCompound = CompressedStreamTools
                    .read(new File(Demise.INSTANCE.getMainDir(), "proxies.ml"));
            if (tagCompound == null) return;

            final NBTTagList proxyListTag = tagCompound.getTagList("proxies", 10);

            for (int i = 0; i < proxyListTag.tagCount(); ++i) {
                try {
                    this.proxies.add(Proxy.fromNBT(this, proxyListTag.getCompoundTagAt(i)));
                } catch (Throwable t) {
                    this.logger.error("Failed to parse proxy: {}", proxyListTag.getCompoundTagAt(i).toString(), t);
                }
            }
        } catch (Exception e) {
            this.logger.error("Couldn't load proxy list", e);
        }
    }

    public void saveProxies() {
        try {
            final NBTTagList tagList = new NBTTagList();

            for (Proxy proxy : this.proxies) {
                tagList.appendTag(proxy.asNBTCompound());
            }

            final NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("proxies", tagList);

            CompressedStreamTools.safeWrite(tagCompound, new File(Demise.INSTANCE.getMainDir(), "proxies.ml"));
        } catch (Exception e) {
            this.logger.error("Couldn't save proxy list", e);
        }
    }

    @Override
    public void onGuiClosed() {
        saveProxies();
    }
}