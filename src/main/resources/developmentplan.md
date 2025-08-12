Fase 1 — Mecânica de Escuridão (ECLIPSE)

Objetivo: Criar o clima — visão reduzida, sem HUD/minimapa, com sons e efeitos atmosféricos.

Visão e neblina:

Injetar ClientTickEvent para aplicar FogDensity reduzida (3–8 blocos).

Aplicar shader/filtro de escuridão extra (GLSL) ou sobreposição de textura.

Remover brilho global (minecraft:brightness).

HUD & UI:

Override de renderização (RenderGameOverlayEvent) para esconder elementos:

Barra de vida, armadura, fome, experiência.

Nicknames (RenderLivingEvent.Specials).

Skins dos jogadores (substituir textura por sombra preta ou silhueta).

Áudio & Ambiente:

Randomização de sons via WorldTickEvent:

Footstep “fantasma” a X blocos de distância.

Sussurros direcionais no áudio 3D.

Sons raros de grito/estática.

Fase 2 — Contaminação (INFECTADOS)

Objetivo: Transformar certos jogadores em agentes da Frequência com objetivos secretos.

Sistema de status de contaminação:

Capability custom para cada player com:

Nível de infecção (0–100%).

Buffs ativos (vida extra, speed, visão).

Objetivo atual (assassinar, sabotar, etc.).

Seleção inicial:

Lista de jogadores pré-configurada pelo admin.

Evento PlayerLoggedInEvent para aplicar infecção inicial.

Buffs progressivos:

+X vida, +velocidade e +alcance de visão a cada kill.

Forma avançada desbloqueada (voz, partículas ao redor do player).

Missões secretas:

Sistema de mensagens privadas (ex.: título na tela, som de estática, texto corrompido).

Randomização de tarefas.

Fase 3 — Sistema de Caçada (Pontuação)

Objetivo: Gamificar o caos.

Pontuação:

+1 ponto por kill, −1 por morte.

Se pontos ≤ −5 → player marcado como “morto permanente” (kick do evento ou espectador).

Armazenar pontos na Capability do player.

HUD para admins:

Lista oculta com pontuação de todos.

Comando /eclipse score <player> para checar.

Fase 4 — Mercado Negro

Objetivo: Locais seguros onde players compram itens raros.

NPC vendedor:

Baseado em Villager ou custom entity.

GUI de troca custom (MerchantOffers) com moedas custom (item currency_coin).

Itens:

Poção anti-neblina (temporária).

Munição rara.

Kit médico.

Documentos (lore).

Estoque limitado por ciclo de evento.

Localização:

Spawns fixos e ocultos.

Possibilidade de rotação aleatória.

Fase 5 — Ciclos de Caos

Objetivo: Intensificar o evento com gatilhos globais.

Timer global:

Executa a cada 10–15 minutos.

Triggers:

Memórias invadindo: fade-in de imagens/textos na tela.

Gritos distantes: som global.

Vultos: entidades sombra passando rapidamente.

HUD Removido: aplicar remoção parcial ou total.

Chat corrompido: interceptar mensagens e substituir caracteres.

Sintomas de Frequência:

Tela piscando em preto (OverlayEvent).

Sons binaurais (esquerda/direita alternando).

Pequenas distorções no campo de visão.

Hostilidade geral:

Infectados normais recebem ordem global de atacar.

Mensagens automáticas (“espalhem…”, “eles não entendem…”).

Fase 6 — Encerramento & Recompensas

Objetivo: Fechar o evento e distribuir prêmios.

Finalização:

Contagem de pontos (Mostrar pontuação final (de mortes) de cada player).

Mensagem final global.

Recompensas:

Sobreviventes: itens, moedas, buffs temporários.

Infectados top score: buffs permanentes.

Prioridade de Desenvolvimento

Escuridão e ambiente sonoro (base do evento).

Status de infecção + buffs.

Pontuação e morte permanente.

Mercado Negro.

Ciclos de Caos (eventos aleatórios).

IA do Melu e finalizações.