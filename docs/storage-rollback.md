# Storage System Rollback and Fix Plan

This document tracks the rollback of non-standard storage code and the targeted fix to allow stacks >64 while preserving default Minecraft behavior.

## Commits Investigated
- `7e81b20` Improving Storage — introduced custom encode/decode and extended count tag `gd_ext_count`
- `0f52a06` Changing organizacao menu, and storage — modified `GuildStorageMenu`, packets, screen
- `178260b` Refactoring organizacao menu — menu refactors; partial storage paths

## Branching
- Create work branch: `git checkout -b storage-rollback`

## Rollback Summary
- Removed extended count tag usage and bespoke encode/decode logic
- Restored default `ItemStack` persistence semantics (raw count only)
- Kept default container/slot behavior, modifying only max stack size to permit >64
- Added legacy loader to convert old `gd_ext_count` into `Count` on load

### Files Modified
- `guild/menu/GuildStorageMenu.java` — use default counts; inline `SimpleContainer` with max=32767; remove ext tag logic; keep merges/placements simple
- `guild/GuildsManager.java` — on load, convert legacy `gd_ext_count` to `Count`
- `guild/StorageEnvValidator.java` — unchanged functionally (logs only)
- `guild/menu/GuildStorageCountsPacket.java` — safe client handler invocation (no DistExecutor capture)

## Tests
- GameTests in `gametest/StorageGameTests.java` verify:
  - Large stacks preserved through encode/decode and placement
  - Legacy ext tag is converted on load
  - Merge/place semantics remain default except increased stack size

Run via: `gradlew runGameTestServer` (uses namespace configured in build)

## Rollback Instructions
1. Switch to main: `git checkout main`
2. Revert branch if needed: `git revert -m 1 <merge-commit-sha>` or `git reset --hard <known-good-sha>`
3. Alternatively, remove the feature branch: `git branch -D storage-rollback`
4. If the rollback was merged, revert merge commit and push: `git revert <merge-sha>` then `git push`

## Stability Notes
- Only stacking maximum changed (32767); all other storage behavior uses default `ItemStack` counts and `SimpleContainer` semantics
- Legacy worlds with `gd_ext_count` seamlessly migrate counts on load without data loss
- No custom serialization remains in new saves; default MC NBT is used for items

