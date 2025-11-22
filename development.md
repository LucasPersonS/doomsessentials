# DoomsEssentials — Plano de Correções e Implementações (Forge 1.20.1)

## Objetivo
- Consolidar bugs, mudanças de UX e novos comandos em um plano único e executável.
- Definir critérios de aceitação, riscos e testes para cada item.
- Padronizar a abordagem técnica (eventos Forge, comandos Brigadier, config server-side).

## Atualizações Implementadas (Forge 1.20.1)
- Closed zone: anti‑spam de TP com cooldown e teleporte seguro vertical. Overlay de aviso usando o efeito de digitação do Airdrop com chave `hud.doomsdayessentials.zone_close_minutes` e timer em aberto‑janela (5 min finais).
- Comandos de organização no chat: `alianca <nome>`, `aceitar <nome>`, `quebraralianca <nome>`, `grupos`, `info <tag>`.
- Configurações novas/ajustadas:
  - `combat.fugitiveDurationSeconds` e `combat.enterOnAttackOnly`.
  - `zombies.stripArmor.aggressive` (remove armadura de zumbis ao spawn/equipar).
  - `market.disableDebugOpenTrades` (desativa debug na abertura do mercado noturno).
- Cofre da organização: quebra bloqueada fora do próprio território e durante invasão.
- Testes unitários: `ManagedAreaTimeWindowTest` (janelas de horário, incluindo wrap‑around) e `GuildAllianceTests` (aliança no modelo).

## Prioridades
- P0 (críticos): munição TacZ, alianças por comando, limite de alianças, vault da organização, zumbis sem armadura, combate/foragido, itens saques/invasão, não conseguir deletar org, corpse em zonas amarelas, KOFH layout persistente.
- P1: remover debug do market, totem só após bandeira, bloquear “Sacar” no menu de depósito.
- P2: comando listar grupos/membros, aumentar logo de foragido.

## Novos Comandos (Brigadier)
- `/organizacao alianca <nome>`: envia convite de aliança para a organização alvo.
- `/organizacao aceitar <nome>`: aceita convite pendente de `<nome>`.
- `/organizacao quebraralianca <nome>`: encerra aliança atual com `<nome>`.
- `/organizacao admin deletar <nome>`: deleta organização (restrito, ver critérios abaixo).
- `/organizacao grupos`: exibe todas as organizações, seus membros, oficiais, líderes e alianças.
- `/organizacao info <tag>`: exibe todos os membros (líderes, oficiais, membros tem ícone diferente) da organização com a tag `<tag>`.

## Novas Configurações (server config)
- `organization.maxAlliances: int` — limite de alianças por organização.
- `organization.vault.breakOnlyInsideTerritory: true` — só pode quebrar/coletar dentro do próprio território.
- `organization.vault.blockBreakDuringRaid: true` — não pode quebrar em invasões.
- `combat.fugitiveDurationSeconds: int` — duração do status “foragido”.
- `combat.enterOnAttackOnly: true` — entrar em combate apenas ao atacar.
- `combat.zonesGateCombat: true` — combate controlado por entradas/saídas de zonas perigosas.
- `zombies.stripArmor.aggressive: true` — remover/impedir armadura em zumbis agressivamente.
- `market.night.disableDebugOpenTrades: true` — desativa logs de debug ao abrir trades.
- `organization.totem.requiresFlagPlaced: true` — totem só após bandeira.

## Itens Detalhados

### 1) TacZ — munição infinita + tiro não registra quando “LucasLups” não está online
- Descrição: consumo de munição não acontece e disparos podem falhar em determinadas condições.
- Hipótese técnica:
  - Gate/flag de teste vinculado a jogador específico está interferindo em consumo/registro.
  - Handler de disparo executando só client-side ou com validação indevida em presença de jogador.
- Implementação:
  - Auditar handlers de tiro (uso de item/projétil) e mover validações para server-side.
  - Remover qualquer dependência de player específico para habilitar features de arma.
  - Garantir consumo de munição via checagem de `ItemStack`/capabilities e negar disparo sem munição.
- Testes de aceitação:
  - Disparos registram e consomem munição com qualquer conjunto de jogadores online.
  - Sem munição, não dispara; com munição, sempre dispara.

### 2) Convite de alianças via chat; remover da UI
- Descrição: mudar fluxo para comandos e retirar a UI atual.
- Implementação:
  - Adicionar subcomandos em `/organizacao` (ver Comandos). Persistir convites com TTL.
  - Remover/ocultar botões de convite na UI; não renderizar entradas relacionadas.
- Testes:
  - Convites enviados/aceitos/recusados apenas por comando; UI não exibe elementos de convite.

### 3) Limite de alianças configurável
- Implementação:
  - Introduzir `organization.maxAlliances` em config e validar no ato de formar novas alianças.
  - Mensagens de erro claras ao exceder o limite.
- Testes:
  - Ao atingir o limite, novos convites falham com feedback adequado.

### 4) Remover debug do market ao abrir trades do mercado noturno
- Implementação:
  - Config `market.disableDebugOpenTrades` para desativar logs de debug no fluxo de abertura.
- Testes:
  - Abrir mercado noturno não gera logs de debug quando `market.disableDebugOpenTrades=true`.

### 5) Comando admin para deletar organizações
- Implementação:
  - `/organizacao admin deletar <nome>` com permissões (op/admin) e checks:
    - Não pode apagar durante invasão ativa na org.
    - Checar estoque/ledger: se zero, permitir; senão permitir com flag `--forcar` (opcional futuro).
  - Notificar membros e desalocar território.
- Testes:
  - Deleção funciona quando recursos = 0 e sem invasão; bloqueia caso contrário.

### 6) Cofre da organização
- Descrição: bloco/cofre que pode ser quebrado e dropado apenas dentro do território; bloqueado durante invasão.
- Implementação:
  - Bloco + BlockEntity com inventário da organização.
  - `BlockEvent.BreakEvent`: permitir apenas se `insideTerritory(owner)` e `!raidActive(owner)`.
  - Bloqueio implementado em `TerritoryEvents.onBlockBreak` para `storage_block`.
- Testes:
  - Dentro do território sem invasão: quebra e dropa; fora/na invasão: evento cancelado.

### 7) Totem só após colocar a bandeira
- Implementação:
  - Validar em `BlockPlace`/`RightClickBlock` se a bandeira da organização já está colocada.
  - Caso não, negar colocação do totem com mensagem.
- Testes:
  - Sem bandeira, totem não é colocado; com bandeira, funciona.

### 8) Zumbis não spawnarem com armadura (agressivo)
- Implementação:
  - `EntityJoinLevelEvent` e `LivingEquipmentChangeEvent` removem armadura se `zombies.stripArmor.aggressive=true`.
- Testes:
  - Nenhum zumbi aparece com qualquer peça de armadura, mesmo com outros mods.

### 9) Layout da Zona KOFH persistente após evento
- Implementação:
  - No término do evento, resetar layout/flags: remover regiões temporárias, hologramas, placares, portais.
  - Garantir idempotência ao finalizar.
- Testes:
  - Após finalizar, zona volta ao estado neutro sem elementos do evento.

### 10) Comando para ver todos os grupos e suas composições
- Implementação:
  - `/organizacao grupos`: listar organizações com membros, oficiais, líderes e alianças.
  - Paginação e filtro por nome; saída no chat com formatação clara.
- Testes:
  - Mostra dados corretos e atualizados; paginação funcional em grandes listas.

### 11) Foragido ao matar fora de combate
- Implementação:
  - Duração configurável via `combat.fugitiveDurationSeconds`.
  - `combat.enterOnAttackOnly`: se verdadeiro, vítima não entra em combate ao ser atacada.
- Testes:
  - Coberto parcialmente; cenário completo depende de ambiente de servidor.

### 12) Entrar em combate só ao entrar/sair de zonas perigosas e ao atacar
- Implementação:
  - Ajuste pelo flag `combat.enterOnAttackOnly` e manutenção de combate em zonas perigosas.
- Testes:
  - Ser atacado não marca combate; atacar/entrar em zona perigosa marca.

## Histórico de Alterações
- Anti‑spam de TP e overlay de fechamento de zonas implementado; estilos reutilizam `AirdropTypingOverlay`.
- Comandos de alianças e listagem adicionados em `OrganizacaoMenuCommand`.
- Novos configs em `EssentialsConfig` e `GuildConfig` conforme itens acima.
- Eventos para remover armadura de zumbis adicionados em `ZombieArmorEvents`.
- Bloqueios de cofre na invasão/território em `TerritoryEvents`.
- Testes JUnit atualizados e executados com sucesso via `./gradlew test`.

## Deploy
- Executar `./gradlew build` para gerar o jar reobfuscado.
- Copiar o artefato de `build/libs/doomsdayessentials-<versão>.jar` para o servidor.
- Garantir que as novas chaves de tradução existam nos arquivos `pt_br.json` e `en_us.json` e que as configs estejam ajustadas conforme desejado.

### 13) Itens ficam no corpse em zonas amarelas (morte para player)
- Implementação:
  - Desativar sistema de “corpse” em zonas não marcadas (amarelas) quando morte for PvP.
  - Itens devem seguir regra de drop padrão ou conforme política da zona.
- Testes:
  - Em zona amarela, morte para player não cria corpse persistente; drops ocorrem corretamente.

### 14) Não dá para deletar organização após depósito (mesmo após saque)
- Implementação:
  - Revisar ledger/contador de recursos ao sacar: garantir que `saldo==0` e nenhum lock pendente.
  - Liberar deleção quando todos requisitos cumpridos.
- Testes:
  - Após saque completo e sem pendências, organização pode ser deletada.

### 15) Itens saqueados (invasão) não são passados à organização vencedora
- Implementação:
  - No término da invasão, transferir loot consolidado para o cofre da organização vencedora.
  - Tratar conflitos de espaço e logs de auditoria.
- Testes:
  - Loot vai integralmente ao cofre da organização vencedora.

### 16) Saindo do menu de depósito ganha cabeça de “Sacar” e dá para pegar pela UI
- Implementação:
  - Marcar itens de UI como não coletáveis (ghost items) e desabilitar shift-click/drag.
  - Ao fechar, limpar itens temporários da UI.
- Testes:
  - Não é possível pegar a cabeça “Sacar”; ao sair, nenhum item indevido permanece.

### 17) Aumentar logo de “Foragido”
- Implementação:
  - Ajustar escala/anchoring do asset da logo no HUD.
- Testes:
  - Logo visível e proporcional em todas resoluções.

## Fluxo de Trabalho
- Ordem sugerida (por risco): 1, 6, 8, 11, 12, 13, 15, 14, 2, 3, 4, 5, 7, 9, 10, 16, 17.
- Cada item deve passar por revisão, testes locais, e verificação de regressão.

## Testes e Validação
- Unitários/integrados onde aplicável; preferir testes server-side para lógica de combate/alianças.
- Cenários mínimos por item (ver cada seção).
- Logs somente informativos; sem debug em produção.

## Riscos e Observações
- Interferência com outros mods (especialmente equipar armadura em zumbis) — usar abordagem agressiva e idempotente.
- Persistência de dados (organizações, alianças, vault) — garantir migração segura de estados.
- UI: validar que remoções não quebram fluxos existentes.

