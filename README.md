# Immersive Northstar

Port of [Northstar](https://github.com/lightning2401/northstar) (branch `1.20.1`, `0.1cb`) by **lightning2401** to **Create 6.0.x** on Minecraft 1.20.1 / Forge 47.

Original mod: MIT License (see `LICENSE.txt`). This repository is an unofficial community port; mod id stays `northstar` so existing data/assets keep working.

**Status: work in progress.** It compiles against Create 6.0.8 and builds a jar; in-game verification is ongoing. See [`PORT_REPORT.md`](PORT_REPORT.md) for what was changed, what is disabled and the TODO list.

## Build
```
./gradlew build        # jar in build/libs
```
Requires JDK 17. Dependencies come from `maven.createmod.net` (Create, Ponder/Catnip, Flywheel), tterrag, blamejared (JEI), GeckoLib.

## Targets
| | version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Create | 6.0.8 (build 291) |
| Flywheel | 1.0.5 |
| Ponder (incl. Catnip) | 1.0.91 |
| JEI | 15.20.0.135 |
| GeckoLib | 4.4.6 |
