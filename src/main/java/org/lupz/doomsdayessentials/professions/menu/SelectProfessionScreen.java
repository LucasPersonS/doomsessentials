package org.lupz.doomsdayessentials.professions.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lupz.doomsdayessentials.professions.network.ProfessionNetwork;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SelectProfessionScreen extends Screen {

	private static class Entry {
		final String id;
		final String title;
		final List<String> lines;
		Entry(String id, String title, List<String> lines){this.id=id;this.title=title;this.lines=lines;}
	}

	private final List<Entry> entries = new ArrayList<>();
	private int idx;
	private int guiLeft, guiTop;
	private final int windowWidth = 220;
	private final int windowHeight = 180;
	private int scroll;

	public SelectProfessionScreen() {
		super(Component.literal("Profissões"));
		entries.add(new Entry("combatente","Combatente", List.of(
			"Passiva: +30% de resistência a dano.",
			"Skill: Adrenalina de Combate – Velocidade 2 e Resistência 2 por 10s; matar inimigos aumenta a duração.",
			"Utilidade: Monta barricadas que podem ser quebradas com tiros do Tacz."
		)));
		entries.add(new Entry("rastreador","Rastreador", List.of(
			"Passiva: Destaque de loot raro em 15 blocos.",
			"Skill: Faro de Caça – revela inimigos próximos por 12s.",
			"Utilidade: Cria pegadas de inimigos que passaram por até 2 minutos."
		)));
		entries.add(new Entry("engenheiro","Engenheiro", List.of(
			"Passiva: Repara armas e ferramentas com menos recursos.",
			"Skill: Torreta Improvisada – coloca sentinela por 30s.",
			"Utilidade: HUD exclusiva para craftar itens diversos."
		)));
		entries.add(new Entry("medico","Médico", List.of(
			"Passiva: Levanta jogadores caídos em 5s (vs. 30s).",
			"Skill: Cura em área (regen + absorção).",
			"Utilidade: Remove níveis de ferimento e recebe recompensas."
		)));
		entries.add(new Entry("cacador","Caçador de Recompensas", List.of(
			"Passiva: +20% de loot de zumbis.",
			"Skill: Marca Letal – +30% de dano no alvo por 20s (linha de visão).",
			"Utilidade: Mural de recompensas para contratos."
		)));
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
			}).bounds(guiLeft - 40, this.height / 2 - 10, 20, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				idx = (idx + 1) % entries.size();
				scroll = 0;
			}).bounds(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20).build());
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.select"), b -> {
			select(entries.get(idx).id);
		}).bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 27, 100, 20).build());
	}

	private void select(String id) {
		ProfessionNetwork.selectProfession(id);
		this.minecraft.setScreen(null);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		this.renderBackground(g);
		Entry e = entries.get(idx);
		g.drawCenteredString(this.font, e.title, this.width / 2, guiTop - 15, 0xFFFFFF);
		int x = guiLeft + 16;
		int y = guiTop + 20 - scroll;
		for (String line : e.lines) {
			g.drawString(this.font, line, x, y, 0xCCCCCC, false);
			y += 12;
		}
		super.render(g, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double z) {
		scroll = Math.max(0, scroll - (int) z * 8);
		return super.mouseScrolled(x, y, z);
	}
} 