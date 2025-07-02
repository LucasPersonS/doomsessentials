package org.lupz.doomsdayessentials.professions.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.professions.network.ProfessionNetwork;

@OnlyIn(Dist.CLIENT)
public class ProfissoesScreen extends AbstractContainerScreen<ProfissoesMenu> {

    private static final ResourceLocation CHEST_LOCATION = new ResourceLocation("textures/gui/container/generic_54.png");
    // no custom overlay texture – rely on vanilla chest background

    public ProfissoesScreen(ProfissoesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 3 * 18 + 17 + 7; // rows * slot + top+bottom
        this.inventoryLabelY = -10000; // keep player inventory label hidden
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Chest background (vanilla texture)
        guiGraphics.blit(CHEST_LOCATION, x, y, 0, 0, this.imageWidth, 17);
        for (int row = 0; row < 3; ++row) {
            guiGraphics.blit(CHEST_LOCATION, x, y + 17 + row * 18, 0, 17, this.imageWidth, 18);
        }
        guiGraphics.blit(CHEST_LOCATION, x, y + 17 + 54, 0, 215, this.imageWidth, 7);

        // Title (centered)
        guiGraphics.drawString(this.font, this.title, x + (this.imageWidth - this.font.width(this.title)) / 2, y + 6, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (stack.is(Items.EMERALD)) {
                ProfessionNetwork.selectProfession("medico");
                this.minecraft.player.closeContainer();
                return true;
            }
            if (stack.is(Items.IRON_SWORD)) {
                ProfessionNetwork.selectProfession("combatente");
                this.minecraft.player.closeContainer();
                return true;
            }
            if (stack.is(Items.COMPASS)) {
                ProfessionNetwork.selectProfession("rastreador");
                this.minecraft.player.closeContainer();
                return true;
            }
            if (stack.is(Items.BARRIER)) {
                ProfessionNetwork.abandonProfession();
                this.minecraft.player.closeContainer();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // no-op; rely on vanilla tooltips when hovering items
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Explicitly render tooltip for hovered slot (vanilla style)
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            guiGraphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
        }
    }
} 