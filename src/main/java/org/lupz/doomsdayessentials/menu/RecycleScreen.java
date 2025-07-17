package org.lupz.doomsdayessentials.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.gui.components.Button;
import org.lupz.doomsdayessentials.recycler.network.ToggleRecyclerPacket;
import org.lupz.doomsdayessentials.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.Button.CreateNarration;
import org.lupz.doomsdayessentials.block.RecycleBlock;

@OnlyIn(Dist.CLIENT)
public class RecycleScreen extends AbstractContainerScreen<RecycleMenu> {

    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/recycle_hud.png");
    private static final ResourceLocation BTN_ON = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/recycle_button_on.png");
    private static final ResourceLocation BTN_OFF = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/recycle_button_off.png");
    public RecycleScreen(RecycleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // background
        graphics.blit(HUD, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // on/off overlay
        boolean on = this.menu.getBlockEntity().getBlockState().getValue(RecycleBlock.RUNNING);
        ResourceLocation tex = on ? BTN_ON : BTN_OFF;
        graphics.blit(tex, x + 152, y + 3, 0, 0, 14, 14, 14, 14);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.pose().pushPose();
        float scale = 0.8f;
        g.pose().scale(scale, scale, scale);
        g.drawString(this.font, Component.literal("Input"), (int) (44 / scale), (int) (8 / scale), 0xFFFFFF, false);
        g.drawString(this.font, Component.literal("Output"), (int) (44 / scale), (int) (34 / scale) + 2, 0xFFFFFF, false);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // button size 14x14 placed at top right of input row
        this.addRenderableWidget(new Button(x+152, y+3,14,14,Component.empty(), b->{
            PacketHandler.CHANNEL.sendToServer(new ToggleRecyclerPacket(menu.getBlockPos()));
            Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1f, 1f);
        }, (supplier) -> supplier.get()){
            @Override
            public void renderWidget(GuiGraphics g,int mx,int my,float pt){
                // Invisible button; rendering is handled in renderBg
            }
        });
    }
} 