# Villager Connect Four

A Fabric mod for Minecraft 26.3: play Connect Four against villagers from the trade screen.
One of the **Villager Games** family, alongside `../Chess` and `../Checkers`. The sibling
mods are the reference for house style - when in doubt, look at how Checkers does it.

## Platform

**Windows.** This is a Java/Gradle project, so it is not the usual web/iOS split - but the
build, the dev client and the capture tooling all run here. PowerShell is the shell; keep
`.ps1` string literals ASCII (em dashes break parsing on PS 5.1).

## Stack

| | |
|---|---|
| Minecraft | 26.3 |
| Loader | Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3 |
| Loom | 1.18-SNAPSHOT |
| Java | **25** |
| Tests | JUnit 5 (junit-bom 5.11.4) |
| Mappings | Mojmap |

## Commands

```
.\gradlew.bat test        # core + engine unit tests
.\gradlew.bat runClient   # dev client
.\gradlew.bat build       # mod jar in build\libs\
```

Gradle must be run with `-p` from this directory, or from it directly - the parent folder is
not a Gradle project.

## Architecture

No blocks, items or entities. The whole mod is a mixin accessor, five network payloads, a
server-side session manager, and one `Screen`.

- `connectfour/` - **pure Java, no Minecraft imports.** Board, Geometry, GameStatus,
  MoveSequence. This is what makes the rules and the engine unit-testable, and it must stay
  import-clean. Loom's `splitEnvironmentSourceSets()` means `src/test` compiles against
  `src/main` only, so anything testable has to live in `src/main`.
- `connectfour/engine/` - search, evaluation, difficulty ladder, transposition table.
- `game/` - session state and the server-authoritative manager.
- `net/` - payloads. **`ConnectFourOutcome`'s ordinal travels on the wire**, so its constant
  order is frozen; new outcomes go on the end.
- `src/client/` - rendering only.

## Domain rules that are easy to get wrong

- **The villager drops first, and plays yellow.** Connect Four is solved and the first player
  wins on 7×6, so this is what makes a Master a real wall. It is the one deliberate departure
  from Chess and Checkers, which both give the player the first move. `ConnectFourOpenS2C`
  therefore has to carry `playerToMove` - the client must not assume it is the player's turn
  just because a board appeared - and `startGame` has to schedule the opening drop itself,
  because nothing else will ask for it.
- **Colour tracks move order, not the player.** Yellow always moves first. Which colour the
  player holds depends on the `first-player` config key. The parity argument in `Evaluator`
  depends on that invariant.
- **Scores are counted in discs on the board**, not plies from the search root. That is what
  makes them absolute and lets one transposition table be shared across games with no
  mate-score adjustment. Do not "fix" this back to root-relative ply.
- The sentinel row in `Geometry` is load-bearing. Without it, discs either side of a column
  boundary read as a vertical win.

## Testing

TDD as per the global rules. Note that **perft is meaningless here** - there are never more
than seven moves - so the suite is built on independent oracles instead: an exhaustive
enumeration of all 69 lines, a second naive solver written inside the test for small boards,
mirror symmetry, two independent position encodings, and transposition-on vs -off. Prefer
adding to those over adding assertions with magic numbers.

Sampling tests must share one `Random` stream. `new Random(seed)` with consecutive seeds
returns strongly correlated first draws, which once made a "does it play randomly" test
return the same column 200 times.
