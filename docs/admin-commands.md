# Admin Commands – Territory Events and Generators

Event management (new root):
- `/event start <area> <durationMinutes> <minPlayers>` — start capture event.
- `/event status <area>` — show status for a specific area.
- `/event status` — list all active events.
- `/event players <area> <minPlayers>` — update required simultaneous players.
- `/event stop <area>` — stop a single event.
- `/event stop` — stop all events.

Legacy alias:
- `/territory event ...` — legacy path remains fully supported and mirrors `/event`.

Generator management (unchanged):
- `/territory generator info <area>` — open generator info GUI (in-game), or prints summary when run from console.
- `/territory generator set <area> <field> <value>` — set core fields (loot/itemsPerHour/storageCap/owner).
- `/territory generator additem <area> <item> <perHour>` — add an item to the generator.
- `/territory generator delitem <area> <item>` — remove an item from the generator.
- `/territory generator reload` — reload generator data from disk/config.

Permissions:
- All commands require admin level (permission level 2).

Feedback:
- Commands provide success/failure messages, periodic broadcast updates during events, and end-of-event titles to all players.

