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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lupz.doomsdayessentials.professions.network.EngineerNetwork;

@OnlyIn(Dist.CLIENT)
public class EngineerCraftScreen extends AbstractContainerScreen<EngineerCraftMenu> {
    private static final ResourceLocation CHEST = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    public EngineerCraftScreen(EngineerCraftMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 3*18+17+7;
        this.inventoryLabelY = -10000;
    }
    @Override protected void renderBg(GuiGraphics g,float pt,int mx,int my){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f,1f,1f,1f);
        int x=(this.width-this.imageWidth)/2;int y=(this.height-this.imageHeight)/2;
        g.blit(CHEST,x,y,0,0,this.imageWidth,17);
        for(int row=0;row<3;++row)g.blit(CHEST,x,y+17+row*18,0,17,this.imageWidth,18);
        g.blit(CHEST,x,y+71,0,215,this.imageWidth,7);
        g.drawString(this.font,this.title,x+(this.imageWidth-this.font.width(this.title))/2,y+6,4210752,false);
    }
    @Override public boolean mouseClicked(double mx,double my,int btn){
        Slot slot=this.getSlotUnderMouse();
        if(slot!=null&&slot.hasItem()){
            ItemStack st=slot.getItem();
            var key= net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(st.getItem());
            if(key!=null){
                EngineerNetwork.craft(key.getPath());
                return true;
            }
        }
        return super.mouseClicked(mx,my,btn);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        this.renderBackground(g);super.render(g,mx,my,pt);
        if(this.hoveredSlot!=null&&this.hoveredSlot.hasItem()) g.renderTooltip(this.font,this.hoveredSlot.getItem(),mx,my);
    }
} 