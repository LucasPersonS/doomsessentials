package io.github.apace100.origins.screen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ViewOriginScreen extends OriginDisplayScreen {

	private final ArrayList<Tuple<Holder<OriginLayer>, Holder<Origin>>> originLayers;
	private int currentLayer = 0;
	private Button chooseOriginButton;

	public ViewOriginScreen() {
		super(Component.translatable(Origins.MODID + ".screen.view_origin"), false);
		Player player = Objects.requireNonNull(Minecraft.getInstance().player);
		Map<ResourceKey<OriginLayer>, ResourceKey<Origin>> origins = IOriginContainer.get(player).map(IOriginContainer::getOrigins).orElseGet(ImmutableMap::of);
		this.originLayers = new ArrayList<>(origins.size());
		Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry();
		Registry<OriginLayer> layersRegistry = OriginsAPI.getLayersRegistry();

		origins.forEach((layer, origin) -> {
			Optional<Holder.Reference<Origin>> origin1 = originsRegistry.getHolder(origin);
			Optional<Holder.Reference<OriginLayer>> layer1 = layersRegistry.getHolder(layer);
			if (origin1.isEmpty() || !origin1.get().isBound())
				return;
			if (layer1.isEmpty() || !layer1.get().isBound())
				return;

			ItemStack displayItem = origin1.get().value().getIcon();
			if (displayItem.getItem() == Items.PLAYER_HEAD) {
				if (!displayItem.hasTag() || !Objects.requireNonNull(displayItem.getTag()).contains("SkullOwner")) {
					displayItem.getOrCreateTag().putString("SkullOwner", player.getDisplayName().getString());
				}
			}
			if ((!origin1.get().is(OriginRegisters.EMPTY.getId()) || layer1.get().value().getOriginOptionCount(player) > 0) && !layer1.get().value().hidden()) {
				this.originLayers.add(new Tuple<>(layer1.get(), origin1.get()));
			}
		});
		this.originLayers.sort(Comparator.comparing(x -> x.getA().value()));
		if (this.originLayers.size() > 0) {
			Tuple<Holder<OriginLayer>, Holder<Origin>> current = this.originLayers.get(this.currentLayer);
			this.showOrigin(current.getB(), current.getA(), false);
		} else
			this.showNone();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
		if (this.originLayers.size() > 0) {
			this.addRenderableWidget(this.chooseOriginButton = Button.builder(Component.translatable(Origins.MODID + ".gui.choose"), b ->
					Minecraft.getInstance().setScreen(new ChooseOriginScreen(Lists.newArrayList(this.originLayers.get(this.currentLayer).getA()), 0, false))).bounds(this.guiLeft + windowWidth / 2 - 50, this.guiTop + windowHeight - 40, 100, 20).build());
			Player player = Objects.requireNonNull(Minecraft.getInstance().player);
			this.chooseOriginButton.active = this.chooseOriginButton.visible = this.originLayers.get(this.currentLayer).getB().is(OriginRegisters.EMPTY.getId()) && this.originLayers.get(this.currentLayer).getA().value().getOriginOptionCount(player) > 0;
			if (this.originLayers.size() > 1) {
				this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
					this.currentLayer = (this.currentLayer - 1 + this.originLayers.size()) % this.originLayers.size();
					this.switchLayer(player);
				}).bounds(this.guiLeft - 40, this.height / 2 - 10, 20, 20).build());
				this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
					this.currentLayer = (this.currentLayer + 1) % this.originLayers.size();
					this.switchLayer(player);
				}).bounds(this.guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20).build());
			}
		}
		this.addRenderableWidget(Button.builder(Component.translatable(Origins.MODID + ".gui.close"), b -> Minecraft.getInstance().setScreen(null)).bounds(this.guiLeft + windowWidth / 2 - 50, this.guiTop + windowHeight + 5, 100, 20).build());
	}

	private void switchLayer(Player player) {
		Tuple<Holder<OriginLayer>, Holder<Origin>> current = this.originLayers.get(this.currentLayer);
		this.showOrigin(current.getB(), current.getA(), false);
		this.chooseOriginButton.active = this.chooseOriginButton.visible = current.getB().is(OriginRegisters.EMPTY.getId()) && current.getA().value().getOriginOptionCount(player) > 0;
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		if (this.originLayers.size() == 0) {
			//if(OriginsClient.isServerRunningOrigins) {
			graphics.drawCenteredString(this.font, Component.translatable(Origins.MODID + ".gui.view_origin.empty").getString(), this.width / 2, this.guiTop + 48, 0xFFFFFF);
			//} else {
			//	drawCenteredText(matrices, this.textRenderer, new TranslatableText(Origins.MODID + ".gui.view_origin.not_installed").getString(), width / 2, guiTop + 48, 0xFFFFFF);
			//}
		}
	}

	@Override
	protected Component getTitleText() {
		Component titleText = this.getCurrentLayer().get().title().view();
		if (titleText != null)
			return titleText;
		return Component.translatable(Origins.MODID + ".gui.view_origin.title", this.getCurrentLayer().get().name());
	}
}
