# Changelog

## [Unreleased] – 2025-11-20

- Adicionado sistema de timer de prisão com sincronização servidor→cliente.
- Criado HUD de contagem regressiva de prisão no cliente.
- Novo comando: ` /prisao tempo ` para mostrar tempo restante (self e alvo).
- Novo comando admin: ` /prisao jail <alvo> <area> <segundos> ` e ` /prisao liberar <alvo> `.
- Sistema de Foragidos aprimorado:
  - Persistência com duração (`wantedUntil`) e limpeza automática ao expirar.
  - Novo comando admin ` /foragidos list ` mostrando nome, tempo restante e localização aproximada.
  - ` /foragidos add <alvo> <minutos> ` e ` /foragidos remove <alvo> `.
- Correção de teleporte infinito durante invasão:
  - Membros podem entrar no próprio território mesmo sob guerra.
  - Feedback ao entrar em território próprio sob invasão.
  - Mensagens claras ao ser bloqueado em território inimigo.
- Correções no sistema de saque ao destruir bandeira/cofre:
  - Transferência determinística dos recursos por ID via `plunderDetailed`.
  - Logs de saque para invasor e invadido em `GuildsManager`.
  - Confirmação visual (Title + linhas) listando itens saqueados para ambas as guildas.

### Comandos
- Unificação: ` /prisao prender/liberar ` adicionados; ` /area prender/soltar/liberar ` mantidos como alias DEPRECADO com aviso.

### Compatibilidade e estabilidade
- Mantida compatibilidade com sistemas existentes (Combat/Wanted/Storage/Generators).
- Tratamento de erros básico em persistência (prisão e wanted list).
