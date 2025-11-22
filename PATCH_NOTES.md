# DoomsEssentials — Patch Notes (Forge 1.20.1)

Data: 22/11/2025

## Visão Geral
- Foco em performance do servidor, UX clara em combate/zonas e consistência visual/áudio da Frequência.
- Ajustes de inventário e corpse em zonas neutras para evitar frustração e exploração.

## Novidades
- Recicladora sem dependência de ticks
  - Processamento agora é agendado em tempo real, reduzindo impacto em TPS.
  - Persistência de estado garante retomada após reinício do servidor.
  - Tempo de reciclagem configurável por `recycler.processSeconds`.
- Frequência
  - `/frequency set` exibe apenas imagens da pasta `assets/doomsdayessentials/textures/frequencia/`.
  - Removida dependência de texturas antigas (antes listadas em `textures/misc`).
- Foragido (HUD)
  - Escala da logo ajustável via config `combat.wantedIconScale` (0.5–3.0, padrão 1.0).
- Zonas Neutras (inventário e corpse)
  - Morrer em zona `NEUTRAL` fora de combate mantém o inventário no respawn.
  - Não gera corpo do mod `corpse` e limpa drops no chão.

## Balanceamentos
- Zonas Neutras
  - Mortes PvP em zona neutra continuam marcando o agressor como “foragido”.
  - Curios (se presentes) também são preservados/restaurados quando aplicável.

## Correções
- Corpse indesejado em zonas neutras fora de combate
  - Remoção automática de entidades próximas do mod `corpse` e limpeza de drops.
- Frequência
  - Imagens antigas não são mais carregadas; apenas os arquivos em `textures/frequencia` são usados para os símbolos/overlays.

## Desempenho e Técnica
- Recicladora
  - Remoção do `BlockEntityTicker` do bloco da recicladora para eliminar trabalho por tick.
  - Agendamento em milissegundos com finalização precisa e persistente.
- HUD/Render
  - Render de “foragido” usa escala configurável sem alterar o asset original.

## Comandos
- `/frequency set <0..100> [nosound] [noimage]`
  - Define nível de Frequência (intensidade de imagens/sons e efeitos de câmera/shader).
  - `nosound` desativa sons; `noimage` desativa imagens.
- Comandos de organização (conforme development.md)
  - `/organizacao alianca <nome>`, `/organizacao aceitar <nome>`, `/organizacao quebraralianca <nome>`
  - `/organizacao grupos`, `/organizacao info <tag>`
  - `/organizacao admin deletar <nome>` (restrito)

## Configurações Novas/Ajustadas
- Combate
  - `combat.fugitiveDurationSeconds`: duração do status “foragido”.
  - `combat.enterOnAttackOnly`: entrar em combate apenas ao atacar.
  - `combat.wantedIconScale`: escala da logo “foragido” no HUD (novo).
- Recicladora
  - `recycler.processSeconds`: tempo real para reciclar 1 item (novo).
- Mercado/Zumbis
  - `market.disableDebugOpenTrades`: remove logs de debug ao abrir trades.
  - `zombies.stripArmor.aggressive`: remover/impedir armadura em zumbis agressivamente.
- Organização
  - `organization.maxAlliances`: limite de alianças por organização.
  - `organization.vault.breakOnlyInsideTerritory`: só quebra/coleta dentro do território.
  - `organization.vault.blockBreakDuringRaid`: bloqueia quebra durante invasão.

## Testes
- Unitários citados no development.md
  - `ManagedAreaTimeWindowTest` (janelas de horário com wrap-around).
  - `GuildAllianceTests` (alianças no modelo).
- Build
  - `./gradlew build` concluído com sucesso.

## Notas de Migração
- Verifique `doomsdayessentials-essentials.toml` para ajustar `combat.wantedIconScale` e `recycler.processSeconds` conforme seu servidor.
- Caso use o mod `corpse`, comportamento em zonas neutras foi alterado para não gerar corpo quando o jogador não está em combate.

