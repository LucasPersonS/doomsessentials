package org.lupz.doomsdayessentials.injury.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lupz.doomsdayessentials.injury.network.InjuryNetwork;
import org.lupz.doomsdayessentials.injury.network.PlayerActionPacket;

public class DownedScreen extends Screen {

    public DownedScreen() {
        super(Component.literal("Downed"));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("Desistir"), (button) -> {
            InjuryNetwork.sendToServer(new PlayerActionPacket(PlayerActionPacket.Action.GIVE_UP));
            // The screen will be closed by the server's response or player death
        }).bounds(this.width / 2 - 100, this.height / 2 + 50, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Chamar um médico"), (button) -> {
            InjuryNetwork.sendToServer(new PlayerActionPacket(PlayerActionPacket.Action.CALL_MEDIC));
            button.active = false;
            button.setMessage(Component.literal("Médico chamado"));
        }).bounds(this.width / 2 + 2, this.height / 2 + 50, 98, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, "Você está ferido!", this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Espere por um médico ou desista.", this.width / 2, this.height / 2 - 20, 0xFFFFFF);

        long remainingMillis = InjuryClientState.getDownedUntil() - System.currentTimeMillis();
        if (remainingMillis > 0) {
            int remainingSeconds = (int) (remainingMillis / 1000);
            Component timerText = Component.literal("Sangrando em: " + remainingSeconds + "s").withStyle(ChatFormatting.RED);
            guiGraphics.drawCenteredString(this.font, timerText, this.width / 2, this.height / 2 + 10, 0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
} 