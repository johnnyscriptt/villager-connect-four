# Changelog

All notable changes to Villager Connect Four are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/); versions follow [SemVer](https://semver.org/).

## [0.1.2] - 2026-10-22

### Fixed
- Discs can be dropped again. On Minecraft 26.3 the board ignored every mouse click, so no disc could be played.

## [0.1.1] - 2026-09-30

### Changed
- Updated to Minecraft 26.3 (Fabric Loader 0.19.5, Fabric API 0.161.0+26.3). No gameplay changes.

## [0.1.0] - 2026-07-26

Initial release.

### Added
- **Connect Four** button on every villager's trade screen - challenge any villager to a full game.
- **The villager drops first, and plays yellow.** Connect Four is a solved game: on the standard 7×6 board whoever moves first can force a win. Giving the villager that side is what makes the top of the difficulty ladder a genuine wall rather than merely a strong opponent, and it is the one place this mod deliberately departs from its Villager Chess and Villager Checkers siblings.
- Difficulty scales with the villager's trading level, and each level thinks differently rather than just deeper: Novice drops almost at random → Apprentice takes the win in front of it and blocks yours → Journeyman never plays into a loss and works with double threats → Expert understands odd/even threat parity → Master has solved the game.
- `first-player=player` in `config/villagerconnectfour.properties` hands you the first move and therefore the theoretically winning side - the sanctioned way past an unbeatable Master. Your colour follows the turn order, so this also makes you yellow.
- `board-size` accepts boards from `4x4` up to `8x7`. Sizes other than 7×6 change the game's value rather than just its scale - several smaller boards are draws with perfect play, so a Master can be held on them.
- Column-based input with a ghost disc hanging over the column under the cursor, discs that fall and settle, and a full column that flashes red with a dull note rather than silently ignoring the click.
- The winning four are outlined and the rest of the board dims, so a win is visible rather than inferred.
- Losing to a Master while it has the first move explains itself: the end banner points at the `first-player` setting instead of leaving the defeat looking arbitrary.
- Abandon handling: walking more than 16 blocks away, the villager dying, or disconnecting abandons the game; closing the board resigns.
- From-scratch pure-Java engine (negamax + alpha-beta + iterative deepening) running on a background thread, built on two bitboards packed into a single `long` with a sentinel row per column so four-in-a-row is found by shift-and with no edge masking.
- Scores are measured in discs on the board rather than plies from the search root, which makes them absolute facts about a position - so one transposition table is shared by every game on the server and a Master's knowledge compounds across a game instead of starting cold each move.
- Fully server-authoritative: the server owns all game state; clients send a column and nothing else.
- Board, discs, holes and highlights are drawn procedurally rather than from a sprite sheet, so the board stays crisp at every size and the mod ships no textures.
- The trade-screen button stacks below other Villager Games mods' buttons instead of overlapping them, and sizes itself to its label.
