package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lupz.doomsdayessentials.professions.network.ProfessionNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import org.lupz.doomsdayessentials.EssentialsMod;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.lupz.doomsdayessentials.injury.InjuryItems;
import org.lupz.doomsdayessentials.professions.items.ProfessionItems;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SelectProfessionScreen extends Screen {

	private static class Entry {
		final String id;
		final String title;
		final String passive;
		final String skill;
		final String utility;
		final String summary;
		final String summary2;
		Entry(String id, String title, String passive, String skill, String utility, String summary, String summary2){
			this.id=id;this.title=title;this.passive=passive;this.skill=skill;this.utility=utility;this.summary=summary;this.summary2=summary2;
		}
	}

	// Botão com cor de fundo e texto branco
	private static class ColoredButton extends Button {
		private final int baseColor;
		public ColoredButton(int x, int y, int w, int h, Component message, OnPress onPress, int baseColor) {
			super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
			this.baseColor = baseColor;
		}
		@Override
		public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
			int color = this.baseColor;
			if (!this.active) {
				color = withAlpha(color, 0x88);
			} else if (this.isHoveredOrFocused()) {
				color = brighten(color, 0.12f);
			}
			int l = this.getX();
			int t = this.getY();
			int r = l + this.getWidth();
			int b = t + this.getHeight();
			g.fill(l, t, r, b, color);
			// borda
			g.fill(l, t, r, t + 1, 0x40FFFFFF);
			g.fill(l, b - 1, r, b, 0x40111111);
			g.fill(l, t, l + 1, b, 0x40FFFFFF);
			g.fill(r - 1, t, r, b, 0x40111111);
			var font = Minecraft.getInstance().font;
			g.drawCenteredString(font, this.getMessage(), l + this.getWidth() / 2, t + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
		}
		private static int withAlpha(int color, int alpha) { return (alpha << 24) | (color & 0x00FFFFFF); }
		private static int brighten(int color, float factor) {
			int a = (color >>> 24) & 0xFF;
			int r = (color >>> 16) & 0xFF;
			int g = (color >>> 8) & 0xFF;
			int b = color & 0xFF;
			r = Math.min(255, (int)(r * (1 + factor)));
			g = Math.min(255, (int)(g * (1 + factor)));
			b = Math.min(255, (int)(b * (1 + factor)));
			return (a << 24) | (r << 16) | (g << 8) | b;
		}
	}

	// Dados do ícone: ou Item ou textura fallback
	private static class IconData {
		final ItemStack item;
		final ResourceLocation texture;
		final int texSize;
		final int drawSize;
		IconData(ItemStack item) { this.item = item; this.texture = null; this.texSize = 0; this.drawSize = 0; }
		IconData(ResourceLocation texture, int texSize) { this.item = ItemStack.EMPTY; this.texture = texture; this.texSize = texSize; this.drawSize = 0; }
		IconData(ResourceLocation texture, int texSize, int drawSize) { this.item = ItemStack.EMPTY; this.texture = texture; this.texSize = texSize; this.drawSize = drawSize; }
	}

	private final List<Entry> entries = new ArrayList<>();
	private int idx;
	private int guiLeft, guiTop;
	private int windowWidth = 260;
	private int windowHeight = 200;
	private int scroll;

	// área do ícone para hover
	private int iconX, iconY, iconSize = 28;

	private static final ResourceLocation BG_PRIMARY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/professions_bg.png");
	private static final ResourceLocation BG_FALLBACK = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/closed_zone.png");
	private static final ResourceLocation COMBAT_FALLBACK = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/gui/combat_icon.png");
	// Rastreador feature icons
	private static final ResourceLocation TRACKER_PASSIVE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/rastreador/passive.png");
	private static final ResourceLocation TRACKER_SKILL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/rastreador/skill.png");
	private static final ResourceLocation TRACKER_UTILITY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/rastreador/utility.png");
	// Engenheiro feature icons
	private static final ResourceLocation ENGINEER_PASSIVE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/engenheiro/passive.png");
	private static final ResourceLocation ENGINEER_SKILL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/engenheiro/skill.png");
	private static final ResourceLocation ENGINEER_UTILITY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/engenheiro/utility.png");
	// Médico feature icons
	private static final ResourceLocation MEDIC_PASSIVE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/medico/passive.png");
	private static final ResourceLocation MEDIC_SKILL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/medico/skill.png");
	private static final ResourceLocation MEDIC_UTILITY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/medico/utility.png");
	// Combatente feature icons
	private static final ResourceLocation FIGHTER_PASSIVE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/combatente/passive.png");
	private static final ResourceLocation FIGHTER_SKILL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/combatente/skill.png");
	private static final ResourceLocation FIGHTER_UTILITY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/combatente/utility.png");
	// Caçador feature icons
	private static final ResourceLocation HUNTER_PASSIVE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/cacador/passive.png");
	private static final ResourceLocation HUNTER_SKILL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/cacador/skill.png");
	private static final ResourceLocation HUNTER_UTILITY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/cacador/utility.png");
	// Armeiro feature icons
	private static final ResourceLocation ARMEIRO_PASSIVE = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/armeiro/passive.png");
	private static final ResourceLocation ARMEIRO_SKILL = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/armeiro/skill.png");
	private static final ResourceLocation ARMEIRO_UTILITY = ResourceLocation.fromNamespaceAndPath(EssentialsMod.MOD_ID, "textures/misc/professions/armeiro/utility.png");
	
	public SelectProfessionScreen() {
		super(Component.literal("Profissões"));
		entries.add(new Entry(
			"combatente","Combatente",
			"+30% de resistência a dano.",
			"Adrenalina de Combate – Velocidade 2 e Resistência 2 por 10s; matar inimigos aumenta a duração.",
			"Última Resistência: quando cair para 0 de vida, fica indestrutível por 5s, e depois morre.",
			"Lutador resistente focado em confronto direto e controle da linha de frente.",
			"Excelente para avançar, segurar posições e proteger aliados em confrontos diretos."));
		entries.add(new Entry(
			"rastreador","Rastreador",
			"Destaque de loot raro em 15 blocos.",
			"Rede de Caça: joga uma armadilha no chão que prende o inimigo por 5s",
			"Território Marcado: cria uma “zona” de 15 blocos por 40s que revela inimigos dentro e aplica slowness 2 neles",
			"Explorador tático que antecipa movimentos inimigos e controla o terreno.",
			"Perfeito para reconhecimento, leitura de mapa e emboscadas coordenadas."));
		entries.add(new Entry(
			"engenheiro","Engenheiro",
			"Coloca uma barreira em sua frente ao tomar dano.",
			"Torreta Improvisada – coloca sentinela por 30s.",
			"Repara armas e armaduras com menos custo.",
			"Especialista em construções e dispositivos que estabilizam a defesa.",
			"Brilha em composições defensivas e setups táticos de área."));
		entries.add(new Entry(
			"medico","Médico",
			"Levanta jogadores caídos em 5s (vs. 30s).",
			"Cura em área (regen + absorção).",
			"Remove níveis de ferimento e recebe recompensas.",
			"Suporte essencial que mantém a equipe viva e operante.",
			"Fundamental em lutas prolongadas, garantindo sustain e retomada do combate."));
		entries.add(new Entry(
			"cacador","Caçador de Recompensas",
			"+20% de loot de zumbis.",
			"Marca Letal – +30% de dano no alvo por 20s (linha de visão).",
			"Mural de recompensas para contratos.",
			"Predador paciente que escolhe alvos e maximiza ganhos.",
			"Especialista em perseguir, finalizar e lucrar com objetivos claros."));
		entries.add(new Entry(
			"armeiro","Armeiro",
			"Engatilhado – recarrega 30% mais rápido.",
			"Tiro de Supressão – por 10s, cada tiro acerta inimigos causando stun de 0.2s.",
			"Bancada Improvisada – permite craftar armas, acoplamentos e munição.",
			"Técnico de armamento que dita o ritmo e sustenta o fogo aliado.",
			"Indicado para quem controla ritmo de combate e garante suprimentos ao time."));
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
		if (entries.size() > 1) {
			this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
				idx = (idx - 1 + entries.size()) % entries.size();
				scroll = 0;
			}).bounds(guiLeft - 40, this.height / 2 - 10, 24, 22).build());
			this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				idx = (idx + 1) % entries.size();
				scroll = 0;
			}).bounds(guiLeft + windowWidth + 16, this.height / 2 - 10, 24, 22).build());
		}
		// Botões lado a lado
		int btnW = 110;
		int gap = 8;
		int total = btnW * 2 + gap;
		int startX = this.width / 2 - total / 2;
		int by = guiTop + windowHeight + 12;
		this.addRenderableWidget(new ColoredButton(startX, by, btnW, 22, Component.literal("Selecionar"), b -> select(entries.get(idx).id), 0xFF2ECC71));
		this.addRenderableWidget(new ColoredButton(startX + btnW + gap, by, btnW, 22, Component.literal("Cancelar"), b -> onClose(), 0xFFE74C3C));
	}

	private void select(String id) {
		ProfessionNetwork.selectProfession(id);
		this.minecraft.setScreen(null);
	}

	private void renderTiledBackground(GuiGraphics g) {
		var rm = Minecraft.getInstance().getResourceManager();
		ResourceLocation tex = rm.getResource(BG_PRIMARY).isPresent() ? BG_PRIMARY : BG_FALLBACK;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 0.18f);
		int tile = 64;
		for (int y = 0; y < this.height; y += tile) {
			for (int x = 0; x < this.width; x += tile) {
				g.blit(tex, x, y, 0, 0, tile, tile, tile, tile);
			}
		}
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	private IconData iconFor(String id) {
		if ("medico".equals(id)) {
			return new IconData(new ItemStack(InjuryItems.MEDIC_KIT.get()));
		}
		if ("engenheiro".equals(id)) {
			return new IconData(new ItemStack(ProfessionItems.ENGINEER_HAMMER.get()));
		}
		if ("rastreador".equals(id)) {
			return new IconData(new ItemStack(Items.COMPASS));
		}
		if ("cacador".equals(id)) {
			return new IconData(new ItemStack(Items.CROSSBOW));
		}
		if ("combatente".equals(id)) {
			// Exibir peitoral de ferro como ícone
			return new IconData(new ItemStack(Items.IRON_CHESTPLATE));
		}
		if ("armeiro".equals(id)) {
			return new IconData(new ItemStack(Items.SMITHING_TABLE));
		}
		return new IconData(COMBAT_FALLBACK, 32);
	}

	private ResourceLocation resolveTaczSlotTexture(String weaponId) {
		try {
			if (weaponId == null || weaponId.isBlank() || !weaponId.contains(":")) return null;
			String[] parts = weaponId.split(":", 2);
			String ns = parts[0];
			String path = parts[1];
			ResourceLocation displayJson = ResourceLocation.fromNamespaceAndPath(ns, "display/guns/" + path + "_display.json");
			var rm = Minecraft.getInstance().getResourceManager();
			var resOpt = rm.getResource(displayJson);
			if (resOpt.isEmpty()) return null;
			try (var is = resOpt.get().open(); var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				JsonObject root = JsonParser.parseReader(br).getAsJsonObject();
				if (!root.has("slot")) return null;
				String slotStr = root.get("slot").getAsString();
				ResourceLocation slotRl = ResourceLocation.parse(slotStr);
				ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(slotRl.getNamespace(), "textures/" + slotRl.getPath() + ".png");
				return rm.getResource(texture).isPresent() ? texture : null;
			}
		} catch (Exception ignored) {}
		return null;
	}

	private void renderIcon(GuiGraphics g, IconData icon) {
		int pad = 6;
		int boxL = iconX - pad;
		int boxT = iconY - pad;
		int boxR = iconX + iconSize + pad;
		int boxB = iconY + iconSize + pad;
		// fundo da moldura
		g.fill(boxL, boxT, boxR, boxB, 0xB4000000);
		g.fill(boxL, boxT, boxR, boxT + 1, 0x60FFFFFF);
		g.fill(boxL, boxB - 1, boxR, boxB, 0x60111111);
		g.fill(boxL, boxT, boxL + 1, boxB, 0x60111111);
		g.fill(boxR - 1, boxT, boxR, boxB, 0x60111111);

		if (icon.item != null && !icon.item.isEmpty()) {
			g.pose().pushPose();
			g.pose().translate(iconX, iconY, 200);
			float s = iconSize / 16f;
			g.pose().scale(s, s, 1f);
			g.renderItem(icon.item, 0, 0);
			g.pose().popPose();
		} else if (icon.texture != null) {
			int draw = icon.drawSize > 0 ? icon.drawSize : iconSize;
			int texSize = icon.texSize <= 0 ? draw : icon.texSize;
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(icon.texture, iconX + (iconSize - draw) / 2, iconY + (iconSize - draw) / 2, 0, 0, draw, draw, texSize, texSize);
		}
	}

	private static void renderScaledItem(GuiGraphics g, ItemStack item, int x, int y, int size) {
		g.pose().pushPose();
		g.pose().translate(x, y, 200);
		float s = size / 16f;
		g.pose().scale(s, s, 1f);
		g.renderItem(item, 0, 0);
		g.pose().popPose();
	}

	private static java.util.List<net.minecraft.util.FormattedCharSequence> buildTooltipLines(String title, String text, int wordsPerLine, int color) {
		java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
		lines.add(Component.literal(title).withStyle(s -> s.withColor(color).withBold(true)).getVisualOrderText());
		if (text == null || text.isBlank()) return lines;
		String[] words = text.split("\\s+");
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (String w : words) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(w);
			count++;
			if (count == wordsPerLine) {
				lines.add(Component.literal(sb.toString()).withStyle(s -> s.withColor(color)).getVisualOrderText());
				sb.setLength(0);
				count = 0;
			}
		}
		if (sb.length() > 0) {
			lines.add(Component.literal(sb.toString()).withStyle(s -> s.withColor(color)).getVisualOrderText());
		}
		return lines;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Custom background
		g.fillGradient(0, 0, this.width, this.height, 0xAA101010, 0xAA101010);
		renderTiledBackground(g);
		// Center panel
		int pad = 10;
		int left = guiLeft - pad;
		int top = guiTop - pad;
		int right = guiLeft + windowWidth + pad;
		int bottom = guiTop + windowHeight + pad;
		g.fill(left, top, right, bottom, 0xB4000000);
		// Border suave
		g.fill(left, top, right, top + 1, 0x80FFFFFF);
		g.fill(left, bottom - 1, right, bottom, 0x80111111);
		g.fill(left, top, left + 1, bottom, 0x80111111);
		g.fill(right - 1, top, right, bottom, 0x80111111);
		// Traçado externo contínuo (2px)
		int outline = 0x66111111;
		g.fill(left - 2, top - 2, right + 2, top, outline);
		g.fill(left - 2, bottom, right + 2, bottom + 2, outline);
		g.fill(left - 2, top, left, bottom, outline);
		g.fill(right, top, right + 2, bottom, outline);

		// Ícone central com moldura
		Entry e = entries.get(idx);
		iconX = this.width / 2 - iconSize / 2;
		iconY = guiTop - 30 - iconSize / 2;
		renderIcon(g, iconFor(e.id));

		// Conteúdo: resumo + "Ideal para..."
		int innerLeft = guiLeft + 8;
		int innerTop = guiTop + 16 - scroll;
		int contentWidth = windowWidth - 16;
		int y = innerTop;
		// Resumo
		Component cSummary = Component.literal(e.summary);
		g.drawWordWrap(this.font, cSummary, innerLeft, y, contentWidth, 0xFFDDDDDD);
		y += this.font.wordWrapHeight(cSummary, contentWidth) + 6;
		// Resumo 2
		Component cSummary2 = Component.literal(e.summary2);
		g.drawWordWrap(this.font, cSummary2, innerLeft, y, contentWidth, 0xFFA7FFAE);
		y += this.font.wordWrapHeight(cSummary2, contentWidth) + 6;
		// Preferência do jogador baseada na classe (removida duplicação)
		// Ícones de features na parte inferior
		int featSize = 22;
		int gap = 10;
		int total = featSize * 3 + gap * 2;
		int startX = this.width / 2 - total / 2;
		int featY = (guiTop + windowHeight) - featSize - 8;
		if ("rastreador".equals(e.id)) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(TRACKER_PASSIVE, startX, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(TRACKER_SKILL, startX + featSize + gap, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(TRACKER_UTILITY, startX + (featSize + gap) * 2, featY, 0, 0, featSize, featSize, featSize, featSize);
		} else if ("engenheiro".equals(e.id)) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(ENGINEER_PASSIVE, startX, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(ENGINEER_SKILL, startX + featSize + gap, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(ENGINEER_UTILITY, startX + (featSize + gap) * 2, featY, 0, 0, featSize, featSize, featSize, featSize);
		} else if ("medico".equals(e.id)) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(MEDIC_PASSIVE, startX, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(MEDIC_SKILL, startX + featSize + gap, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(MEDIC_UTILITY, startX + (featSize + gap) * 2, featY, 0, 0, featSize, featSize, featSize, featSize);
		} else if ("combatente".equals(e.id)) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(FIGHTER_PASSIVE, startX, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(FIGHTER_SKILL, startX + featSize + gap, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(FIGHTER_UTILITY, startX + (featSize + gap) * 2, featY, 0, 0, featSize, featSize, featSize, featSize);
		} else if ("cacador".equals(e.id)) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(HUNTER_PASSIVE, startX, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(HUNTER_SKILL, startX + featSize + gap, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(HUNTER_UTILITY, startX + (featSize + gap) * 2, featY, 0, 0, featSize, featSize, featSize, featSize);
		} else if ("armeiro".equals(e.id)) {
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			g.blit(ARMEIRO_PASSIVE, startX, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(ARMEIRO_SKILL, startX + featSize + gap, featY, 0, 0, featSize, featSize, featSize, featSize);
			g.blit(ARMEIRO_UTILITY, startX + (featSize + gap) * 2, featY, 0, 0, featSize, featSize, featSize, featSize);
		} else {
			renderScaledItem(g, new ItemStack(Items.BOOK), startX, featY, featSize);
			renderScaledItem(g, new ItemStack(Items.NETHER_STAR), startX + featSize + gap, featY, featSize);
			renderScaledItem(g, new ItemStack(Items.CHEST), startX + (featSize + gap) * 2, featY, featSize);
		}

		super.render(g, mouseX, mouseY, delta);

		// Tooltip do ícone principal
		if (mouseX >= iconX - 6 && mouseX <= iconX + iconSize + 6 && mouseY >= iconY - 6 && mouseY <= iconY + iconSize + 6) {
			g.renderTooltip(this.font, Component.literal(e.title), mouseX, mouseY);
		}
		// Tooltips das features (inferior)
		int fX = startX;
		if (mouseX >= fX && mouseX <= fX + featSize && mouseY >= featY && mouseY <= featY + featSize) {
			var tip = buildTooltipLines("Passiva", e.passive, 5, 0x00FF00);
			g.renderTooltip(this.font, tip, mouseX, mouseY);
		}
		fX += featSize + gap;
		if (mouseX >= fX && mouseX <= fX + featSize && mouseY >= featY && mouseY <= featY + featSize) {
			var tip = buildTooltipLines("Skill", e.skill, 5, 0xE6B866);
			g.renderTooltip(this.font, tip, mouseX, mouseY);
		}
		fX += featSize + gap;
		if (mouseX >= fX && mouseX <= fX + featSize && mouseY >= featY && mouseY <= featY + featSize) {
			var tip = buildTooltipLines("Utilidade", e.utility, 5, 0xFFFFFF);
			g.renderTooltip(this.font, tip, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double z) {
		scroll = Math.max(0, scroll - (int) z * 12);
		return super.mouseScrolled(x, y, z);
	}
} 