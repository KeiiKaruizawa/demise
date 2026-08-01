package net.minecraft.client.gui;

import gr.larryngton.demise.Demise;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;

import java.util.List;

public class GuiDisconnected extends GuiScreen {
    private final String reason;
    private final IChatComponent message;
    private List<String> multilineMessage;
    private final GuiScreen parentScreen;
    private int field_175353_i;

    public GuiDisconnected(GuiScreen screen, String reasonLocalizationKey, IChatComponent chatComp) {
        this.parentScreen = screen;
        this.reason = I18n.format(reasonLocalizationKey);
        this.message = chatComp;
    }

    protected void keyTyped(char typedChar, int keyCode) {
    }

    public void initGui() {
        this.buttonList.clear();
        this.multilineMessage = this.fontRendererObj.listFormattedStringToWidth(this.message.getFormattedText(), width - 50);
        this.field_175353_i = this.multilineMessage.size() * this.fontRendererObj.FONT_HEIGHT;
        this.buttonList.add(new GuiButton(0, (float) width / 2 - 100, (float) height / 2 + (float) this.field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT, I18n.format("gui.toMenu")));
        this.buttonList.add(new GuiButton(1, (float) width / 2 - 100, (float) height / 2 + (float) this.field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT * 2 + 15, "Reconnect"));
    }

    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(this.parentScreen);
                break;
            case 1:
                mc.displayGuiScreen(new GuiConnecting(Demise.serverHistory.getP_i1181_1_(), mc, Demise.serverHistory.getP_i1181_3_()));
                break;
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, this.reason, (float) width / 2, (float) height / 2 - (float) this.field_175353_i / 2 - this.fontRendererObj.FONT_HEIGHT * 2, 11184810);
        int i = height / 2 - this.field_175353_i / 2;

        if (this.multilineMessage != null) {
            for (String s : this.multilineMessage) {
                drawCenteredString(this.fontRendererObj, s, (float) width / 2, i, 16777215);
                i += this.fontRendererObj.FONT_HEIGHT;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
