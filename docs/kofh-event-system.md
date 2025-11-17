# KOFH Territory Event System – Technical Overview

This document describes the architecture and operation of the KOFH territory capture event system.

Goals:
- Robust, server-driven capture events.
- Clear separation between core logic and configuration.
- Stable networking for client HUD and markers.
- Intuitive admin interface.

Architecture:
- Core manager: `TerritoryEventManager` maintains active events and runs a 1-second tick to process capture state.
- Configuration: `TerritoryEventConfig` centralizes constants (e.g., max concurrent events, broadcast interval). Future work can wire this to Forge config.
- Networking:
  - `PacketHandler` registers S2C packets including `TerritoryProgressPacket` (HUD) and `TerritoryMarkerPacket` (world marker).
  - All sends use a helper `safeSend(...)` to avoid runtime exceptions disrupting the tick loop and to provide error logging.
- Client:
  - `TerritoryMarkerClient` renders world markers when status changes.

Event lifecycle:
1. Admin starts via `/event start <area> <durationMinutes> <minPlayers>`.
2. The area status marker broadcasts as contested.
3. Each second, guild player counts inside the area are evaluated:
   - If exactly one guild meets the required players, progress accumulates.
   - If more than one guild meets the requirement or nobody, progress resets.
4. Progress HUD is sent to all clients; periodic chat announcements summarize status.
5. Upon reaching the duration threshold, the guild captures the area:
   - The area becomes SAFE.
   - The resource generator ownership is updated.
   - A final marker broadcast is sent.

Admin interface:
- `/event` root simplifies event management while `/territory event` remains as a legacy alias.
- `/territory generator` commands are unchanged.

Error handling and stability:
- Network sends are guarded with `safeSend` and exceptions are logged via `EssentialsMod.LOGGER`.
- Per-event tick processing is wrapped in try/catch to ensure one faulty area does not disrupt the whole system.

Testing:
- Verify event start/stop, progress accumulation/reset under contested conditions, and final capture transition.
- Confirm both `/event` and `/territory event` variants behave identically.
- Test under load with multiple players and concurrent events (bounded by `MAX_CONCURRENT_EVENTS`).

