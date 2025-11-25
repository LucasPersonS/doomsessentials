# DoomsEssentials — Patch Notes (V.1.0)
Data: 22/11/2025

## O que muda para você
- Zonas Neutras (amarelas)
  - Se você morrer fora de combate dentro de uma zona neutra, seus itens serão mantidos no respawn.
  - Não será gerado “corpse” no local da morte e nada cairá no chão nesses casos.
  - Agressor continua podendo virar “foragido” ao matar em zona neutra.

- Frequência (efeitos visuais e sons)
  - As imagens exibidas pela Frequência foram atualizadas

- Logo de “Foragido”
  - O ícone acima da cabeça dos jogadores “foragidos” está mais visível e pode aparecer maior 

- Recicladora
  - Processa em tempo real e não depende de ticks; experiência mais fluida.
  
# ZONAS PERIGOSAS

- Foi adicionado um horário específico onde as zonas abrem, e fecham
- Foi adicionado um timer para quando a zona está perto de fechar, para alertar os jogadores.
- Foi adicionado um indicador visual para quando uma zona está fechada e você tenta entrar
- Foi corrigido o bug de crashar ao entrar na zona perigosa

## Comandos úteis
Foram criados comandos úteis para ver todas organizações existentes no servidor, por comandos próprios.

- `/organizacao alianca <nome>`: enviar convite de aliança.
- `/organizacao aceitar <nome>`: aceitar convite.
- `/organizacao quebraralianca <nome>`: encerrar aliança.
- `/organizacao grupos`: ver organizações, membros e alianças.
- `/organizacao info <tag>`: ver membros e hierarquia de uma organização.

## Organização

- Foi corrigido o bug que dava pra pegar a cabeça de um jogador pela HUD de Organizacao.
- Foi corrigido um bug onde o organizacao invadir não funcionava como o esperado > Não repassava os itens da defensora ao atacante ao quebrar a bandeira

## Regras de combate (resumo)
- “entrar em combate apenas ao atacar”, ser atacado não coloca você automaticamente em combate mais.
- Em zona neutra, se você não estiver em combate, a morte preserva seu inventário.
- Ao matar um jogador em zona neutra, atualmente você recebe a tag de foragido e é adicionado ao sistema de bounty hunters

## Qualidade de vida
- Menos quedas de desempenho em tarefas contínuas (como reciclagem).
- HUD com ícones e mensagens mais claros em combate/zonas.

# Sliding

- O sliding agora está menor, não está tão agressivo quanto antes.
- Foi corrigido um bug onde sua cabeça ficava rodando ao slidar.
- Foi corrigido um bug que você fica preso na animação de sliding infinito
- Foi corrigido um bug da tela flickar após dar sliding