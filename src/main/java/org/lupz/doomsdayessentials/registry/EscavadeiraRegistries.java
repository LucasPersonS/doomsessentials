package org.lupz.doomsdayessentials.registry;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.lupz.doomsdayessentials.client.renderer.EscavadeiraRenderer;
import org.lupz.doomsdayessentials.client.screen.EscavadeiraScreen;
import org.lupz.doomsdayessentials.menu.EscavadeiraMenu;
import org.lupz.doomsdayessentials.block.escavadeira.EscavadeiraControllerBlock;
import org.lupz.doomsdayessentials.blockentity.EscavadeiraControllerBlockEntity;

@Mod.EventBusSubscriber(modid = EscavadeiraRegistries.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EscavadeiraRegistries {
	public static final String MODID = "doomsdayessentials";

	public static Block ESCAVADEIRA_CONTROLLER_BLOCK;
	public static Item ESCAVADEIRA_CONTROLLER_ITEM;
	public static BlockEntityType<EscavadeiraControllerBlockEntity> ESCAVADEIRA_CONTROLLER_BE;
	public static MenuType<EscavadeiraMenu> ESCAVADEIRA_MENU_TYPE;

	@SubscribeEvent
	public static void registerAll(RegisterEvent event) {
		event.register(Registries.BLOCK, helper -> {
			escavadeiraBlock(helper);
		});
		event.register(Registries.ITEM, helper -> {
			escavadeiraBlockItem(helper);
		});
		event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
			escavadeiraBlockEntity(helper);
		});
		event.register(Registries.MENU, helper -> {
			escavadeiraMenu(helper);
		});
	}

	private static void escavadeiraBlock(RegisterEvent.RegisterHelper<Block> helper) {
		ResourceLocation id = new ResourceLocation(MODID, "escavadeira_controller");
		ESCAVADEIRA_CONTROLLER_BLOCK = new EscavadeiraControllerBlock(Block.Properties.of().strength(5.0F, 30.0F).requiresCorrectToolForDrops());
		helper.register(id, ESCAVADEIRA_CONTROLLER_BLOCK);
	}

	private static void escavadeiraBlockItem(RegisterEvent.RegisterHelper<Item> helper) {
		ResourceLocation id = new ResourceLocation(MODID, "escavadeira_controller");
		ESCAVADEIRA_CONTROLLER_ITEM = new BlockItem(ESCAVADEIRA_CONTROLLER_BLOCK, new Item.Properties());
		helper.register(id, ESCAVADEIRA_CONTROLLER_ITEM);
	}

	private static void escavadeiraBlockEntity(RegisterEvent.RegisterHelper<BlockEntityType<?>> helper) {
		ResourceLocation id = new ResourceLocation(MODID, "escavadeira_controller");
		ESCAVADEIRA_CONTROLLER_BE = BlockEntityType.Builder.of(EscavadeiraControllerBlockEntity::new, ESCAVADEIRA_CONTROLLER_BLOCK).build(null);
		helper.register(id, ESCAVADEIRA_CONTROLLER_BE);
	}

	private static void escavadeiraMenu(RegisterEvent.RegisterHelper<MenuType<?>> helper) {
		ResourceLocation id = new ResourceLocation(MODID, "escavadeira_menu");
		ESCAVADEIRA_MENU_TYPE = net.minecraftforge.common.extensions.IForgeMenuType.create(EscavadeiraMenu::fromNetwork);
		helper.register(id, ESCAVADEIRA_MENU_TYPE);
	}

	@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static final class ClientOnly {
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent evt) {
			evt.enqueueWork(() -> {
				MenuScreens.register(ESCAVADEIRA_MENU_TYPE, EscavadeiraScreen::new);
				BlockEntityRenderers.register(ESCAVADEIRA_CONTROLLER_BE, EscavadeiraRenderer::new);
			});
		}
	}
} 