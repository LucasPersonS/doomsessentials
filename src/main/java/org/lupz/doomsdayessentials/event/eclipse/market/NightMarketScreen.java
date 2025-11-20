package org.lupz.doomsdayessentials.event.eclipse.market;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lupz.doomsdayessentials.EssentialsMod;

public class NightMarketScreen extends AbstractContainerScreen<NightMarketMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/anvil.png");
    private Button confirmBtn;
    private Button cancelBtn;

    public NightMarketScreen(NightMarketMenu menu, Inventory playerInv, Component title){
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY){
        RenderSystem.setShaderColor(1,1,1,1);
        g.blit(BG, this.leftPos, this.topPos, 0,0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta){
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
        int cx = this.leftPos + this.imageWidth / 2;
        g.drawString(this.font, Component.literal("§6Editar Troca"), cx - this.font.width("Editar Troca")/2, this.topPos - 20, 0xFFFFFF, false);
        Slot s0 = this.menu.slots.get(0);
        Slot s1 = this.menu.slots.get(1);
        Slot s2 = this.menu.slots.get(2);
        if (!s0.getItem().isEmpty()) g.fill(s0.x + this.leftPos - 1, s0.y + this.topPos - 1, s0.x + this.leftPos + 17, s0.y + this.topPos + 17, 0x3300FF00);
        if (!s1.getItem().isEmpty()) g.fill(s1.x + this.leftPos - 1, s1.y + this.topPos - 1, s1.x + this.leftPos + 17, s1.y + this.topPos + 17, 0x3300FF00);
        if (!s2.getItem().isEmpty()) g.fill(s2.x + this.leftPos - 1, s2.y + this.topPos - 1, s2.x + this.leftPos + 17, s2.y + this.topPos + 17, 0x33FFAA00);
    }

    

    @Override
    protected void init(){
        super.init();
        int x = this.leftPos + this.imageWidth - 20;
        int y = this.topPos + 8;
        this.confirmBtn = this.addRenderableWidget(Button.builder(Component.literal("✔"), b -> onConfirm()).pos(x - 20, y).size(18, 18).build());
        this.cancelBtn = this.addRenderableWidget(Button.builder(Component.literal("↩"), b -> onCancel()).pos(x - 20, y + 22).size(18, 18).build());
    }

    private void onCancel(){
        this.minecraft.player.closeContainer();
    }

    private void onConfirm(){
        ItemStack buy1 = this.menu.getBuy1();
        ItemStack buy2 = this.menu.getBuy2();
        ItemStack sell = this.menu.getSell();
        if (buy1.isEmpty() || sell.isEmpty()){
            if (this.minecraft != null && this.minecraft.player != null){ this.minecraft.player.displayClientMessage(Component.literal("Configuração inválida"), true); }
            return;
        }
        String buy1Id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(buy1.getItem()).toString();
        String buy2Id = buy2.isEmpty()?null:net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(buy2.getItem()).toString();
        String sellId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(sell.getItem()).toString();
        int buy1Count = Math.max(1, buy1.getCount());
        int buy2Count = buy2.isEmpty()?0:Math.max(1, buy2.getCount());
        int sellCount = Math.max(1, sell.getCount());
        int maxUses = 64;
        int xp = 1;
        float priceMult = 0.05f;
        org.lupz.doomsdayessentials.network.PacketHandler.CHANNEL.sendToServer(
                org.lupz.doomsdayessentials.event.eclipse.market.admin.NightMarketAdminActionPacket.addTradeHere(
                        this.menu.getMarketId(), this.menu.getBlockPos(), buy1Id, buy1Count, buy2Id, buy2Count, sellId, sellCount, maxUses, xp, priceMult));
        this.minecraft.player.closeContainer();
    }
}
