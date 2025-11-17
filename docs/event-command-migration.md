# Migration Guide: /territory event -> /event

This guide explains the transition from the legacy territory event commands to the simplified `/event` command.

What changed:
- New root command `/event` provides: `start`, `status`, `players`, and `stop` subcommands.
- Legacy `/territory event` remains available for full backward compatibility.

Examples:
- Start: `/event start <area> <durationMinutes> <minPlayers>`
- Status (area): `/event status <area>`
- Status (summary): `/event status`
- Update required players: `/event players <area> <minPlayers>`
- Stop one: `/event stop <area>`
- Stop all: `/event stop`

Notes:
- Permission level 2 is required for all `/event` commands.
- The legacy `/territory generator` commands are unchanged.

Server operators can adopt `/event` immediately; existing scripts and admin practices using `/territory event` will continue to work unchanged during the transition.

