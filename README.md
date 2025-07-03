# Doomsday Essentials – Command Reference

Welcome to **Doomsday Essentials**, a set of quality-of-life and role-play features for your server.  This document lists **every command that the mod adds**, grouped by category so you can quickly find what you need.

> • Unless stated otherwise, arguments wrapped in `<angle brackets>` are **required** and those in `[square brackets]` are **optional**.
> • Permission levels follow vanilla Minecraft: `0` = any player, `2` = operators.

---

## 1. Chat Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/g <mensagem>` | 0 | Sends a **global** chat message that can be read from anywhere. |
| `/admin chat` | 2 | Toggles the admin-only chat channel on/off for yourself. |
| `/admin chat <mensagem>` | 2 | Sends one message to the admin chat channel without toggling it. |
| `/doomshelp <mensagem>` | 0 | Sends a request for help to all online operators. A clickable **[TP]** button is appended so staff can teleport to you. |

---

## 2. Combat Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/combat` | 0 | Shows **your** remaining combat-tag timer. |
| `/combat clear <alvos>` | 2 | Clears the combat tag for the specified entities. |
| `/combat tag <jogadores>` | 2 | Forces the specified players into combat. |
| `/combat check <jogador>` | 2 | Displays how many seconds the player still has in combat. |

---

## 3. Area Management (Admins)
Create protected **SAFE** or **DANGER** regions.

| Command | Description |
|---------|-------------|
| `/area create <nome> <tipo> <pos1> <pos2>` | Creates a new area. `<tipo>` is `safe` or `danger`; positions are two opposite corner block coordinates. |
| `/area list` | Lists all defined areas with a clickable **[TP]** shortcut to their centre. |
| `/area delete <nome>` | Starts a timed (30 s) delete request. |
| `/area confirmdelete <nome>` | Confirms and deletes the area. |
| `/area flag <nome> <flag> <valor>` | Sets or changes an area flag (PvP, explosions, heal, entry message, etc.). Use tab-completion for valid flags. |
| `/area expand <nome> <blocos> <direção>` | Expands an area by the given amount. Direction: `up`, `down`, `north`, `south`, `east`, `west`. |

_All `/area` sub-commands require permission level 2._

---

## 4. Injury System (Admins)

| Command | Permission | Description |
|---------|------------|-------------|
| `/injury set <jogador> <nível>` | 2 | Sets a player's injury level (0 – `maxInjuryLevel` from config). |
| `/injury forceheal <jogador>` | 2 | Fully heals injuries, revives, and removes the player from a medical bed. |
| `/injury forceunbed <jogador>` | 2 | Forces the player out of a medical bed without healing. |
| `/injury down <jogador>` | 2 | Instantly downs the player. |
| `/injury revive <jogador>` | 2 | Instantly revives the player if they are downed. |

---

## 5. Profession System
### 5.1 General
| Command | Permission | Description |
|---------|------------|-------------|
| `/profissoes` | 0 | Opens the profession selection UI. |
| `/profissoes loja` | 0 | Opens the in-game profession shop. |
| `/profissoes loja <item>` | 0 | Purchases the specified shop item (aliases are tab-completed). |

### 5.2 Medic (`/medico`)
Only available if you currently hold the **Médico** profession.

| Sub-command | Description |
|-------------|-------------|
| `/medico heal` | Performs an AOE heal centred on you (radius and cooldown set in the config). |
| `/medico help` | Requests medical assistance; online medics receive an SOS notification. |
| `/medico aceitar <jogador>` | Accepts the specified player's SOS and notifies them you are on the way. |
| `/medico bed [jogador]` | Places yourself or another player into a medical bed for injury recovery. |
| `/medico encerrar` | Closes the active SOS (either your own or the one you accepted). |

### 5.3 Tracker (`/rastreador`)
Only available if you currently hold the **Rastreador** profession.

| Sub-command | Description |
|-------------|-------------|
| `/rastreador whitelist add <jogador>` | Allows the tracker's compass & scan abilities to affect the player. |
| `/rastreador whitelist remove <jogador>` | Removes the player from your whitelist. |
| `/rastreador whitelist list` | Displays the players currently on your whitelist. |

---

## 6. Miscellaneous

| Command | Permission | Description |
|---------|------------|-------------|
| `/playsoundessentials <frequencia1|frequencia2> [jogador]` | 2 | Plays a custom sound to yourself or the specified player. |

---

### Need help?
If you have any questions, use `/doomshelp <mensagem>` to page the staff team or join our Discord.

Happy adventuring! :crossed_swords: :medical_symbol: :mag_right: 