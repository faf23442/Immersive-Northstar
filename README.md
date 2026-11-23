# Immersive Northstar

Port of [Northstar](https://github.com/lightning2401/northstar) (branch `1.20.1`, `0.1cb`) by **lightning2401** to **Create 6.0.x** on Minecraft 1.20.1 / Forge 47.

Original mod: MIT License (see `LICENSE.txt`). This repository is an unofficial community port; mod id stays `northstar` so existing data/assets keep working.

**Status: work in progress.** Version `0.1cb-1.20.1-create6.2` builds against Create 6.0.8. All 21 server regression tests pass with Immersive Engineering, including legacy rocket cargo, save/reload, launch, dimension transfer and landing. The earlier client machine fixture was checked with Flywheel and fallback rendering. See [`PORT_REPORT.md`](PORT_REPORT.md) for the verification scope and remaining gameplay checks.

## Build
```
./gradlew build        # jar in build/libs
```
Requires JDK 17. Dependencies come from `maven.createmod.net` (Create, Ponder/Catnip, Flywheel), tterrag, blamejared (JEI), GeckoLib.

## Development checks
```
./gradlew runGameTestServer                  # server-side port regression tests
./gradlew runGameTestServer -PcompatMods     # also load Immersive Engineering 10.2.0-183
./gradlew runClient -PcompatMods -PgameTests # client with the same tests and machine fixture
```
In a creative test world with commands enabled, `/test run machineblockentitiesload`
places the machine fixture for visual inspection. Use `/flywheel backend flywheel:off` to check fallback rendering
and `/flywheel backend DEFAULT` to restore the default backend.
For a short rocket flight and landing scene, start the client with
`./gradlew runClient -PcompatMods -PgameTests -PvisualRocket`, then run
`/test run visualrocketflight` in a disposable creative test world. The scene moves the player
onto the rocket and starts a short flight; use it only in a test world.
Test classes and structures are kept in `src/gameTest` and are excluded from the release jar.
The test server uses `run-gametest/`, separate from the client's `run/` directory.
MixinExtras is supplied separately for development because Create's slim jar omits bundled dependencies.

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
