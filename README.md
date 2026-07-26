# Villager Connect Four

A Fabric mod for Minecraft 26.2: play Connect Four against villagers. Open any villager's
trade screen and hit **Connect Four**. The villager's trading level sets its strength:

| Villager level | Connect Four strength |
|---|---|
| Novice | Drops more or less at random — won't take a win, won't block yours |
| Apprentice | Takes the win in front of it and blocks yours, and nothing further |
| Journeyman | Never plays into a loss; makes and spots double threats |
| Expert | Understands odd/even threat parity — builds the threats that will actually be reached |
| Master | Has solved the game |

**The villager drops first, and plays yellow.** That is deliberate, and it is the most
important thing to know about this mod.

## Why the villager goes first

Connect Four is *solved*. On the standard 7×6 board the player who drops first can force a
win, opening in the centre column; it has been proved by exhaustive computation, not merely
observed. Whoever moves first is playing a won game.

So handing the villager the first move is what gives the ladder a real top end: a Master is
not "very strong", it is **unbeatable**, and no amount of skill will change that. The sibling
Villager Chess and Villager Checkers mods give the player the first move, because in those
games it is a small edge. Here it decides the game outright, which is exactly why it is worth
inverting.

If you would rather have the winning side yourself, see [Turn order](#turn-order) below. Your
colour follows the turn order — whoever drops first plays yellow — so flipping the setting
makes you yellow.

Click a column, or anywhere above it, to drop. A ghost disc hangs over the column you are
pointing at; a full column flashes red rather than doing nothing.

Walking away (>16 blocks), the villager dying, or disconnecting abandons the game; closing
the board resigns it. Just for fun — no stakes, no rewards.

## Rules

Standard Connect Four: a 7×6 grid, discs fall to the lowest free cell of the column you pick,
and the first player to line up four — horizontally, vertically or diagonally — wins. If all
42 cells fill with no line, it is a draw.

That is the whole rulebook. There is no repetition rule and no move counter, because nothing
ever leaves the board: every move adds a disc, so no position can ever occur twice and the
game cannot run long. A reader coming from the Checkers sibling will look for its threefold
and forty-move rules; they have no analogue here.

## Configuration

`config/villagerconnectfour.properties` (written on first run, with all of this explained
inline):

```properties
first-player=villager   # or: player
board-size=7x6
```

### Turn order

`first-player=player` gives you the first move and therefore the theoretically winning side.
Play accurately enough and even a Master cannot stop you. It is the sanctioned way past the
top of the ladder, and the honest one — the alternative would be to make the Master play
worse than it can.

### Board size

`board-size` accepts anything from `4x4` up to the limit imposed by the representation:
width × (height + 1) must fit in 64 bits, so `8x7` is the largest square-ish board and `9x7`
is refused. Other sizes are not just a change of scale — they change the game's value. 7×6 is
a first-player win; several smaller boards, including 4×4 and 5×4, are draws with perfect
play, so a Master going first on one of those can be held. The ladder was tuned on 7×6.

Both settings are read once at startup, so a board already open never changes underneath you.

Deliberately *not* configurable: search depth, time budget, or a direct difficulty override.
The ladder is meant to be five opponents that think differently rather than one opponent with
a dial, and exposing the knobs would flatten it back into a number.

## Tech notes

- **Connect Four core** (`connectfour/`): pure Java, no Minecraft imports. Two bitboards in a
  single `long`, laid out one column per `height + 1` bits. The wasted bit per column is a
  sentinel that is always empty, and it is what lets four-in-a-row be found by and-ing the
  board with shifted copies of itself with no edge masking at all — without it, discs either
  side of a column boundary read as a vertical win.
- **Board state travels as the move sequence** — the columns played, as digits, `"4453"`.
  Shorter than any grid encoding, carries the whole history for free, and cannot describe an
  illegal position, which removes an entire class of validation.
- **Engine** (`connectfour/engine/`): negamax + alpha-beta + iterative deepening. The
  difficulty ladder is built on a *threat horizon* — what each rung can work out about the
  cells that complete a line — so every level thinks differently rather than just deeper. The
  Expert's parity weighting and the Master's exact solve are genuinely different mechanisms,
  not larger numbers.
- **Scores are measured in discs on the board**, not plies from the search root. Because every
  move adds exactly one disc, that makes a score an absolute fact about a position, so the
  transposition table stays valid across moves and whole games and needs no mate-score
  adjustment on the way in or out. One table is shared by every game on the server, which is
  how a Master reaches exact play part-way through rather than starting cold each move.
- **Server-authoritative**: clients send a column and nothing else; the server owns all game
  state and validates against its own board.
- The board is drawn with no art at all — discs, holes and highlights are rendered
  procedurally, so they stay crisp at every board size and the mod ships no textures.

## Build & run

```
./gradlew test        # core + engine unit tests
./gradlew runClient   # dev client
./gradlew build       # mod jar in build/libs/
```

Requires Java 25. Install the jar with Fabric Loader ≥ 0.19.3 + Fabric API for 26.2.

### About the tests

Perft, the move-generation check the Checkers sibling leans on, is vacuous here: there are
never more than seven moves and the count from any position is just the number of columns
with room, so asserting it would restate the code rather than check it. The suite is built on
oracles that are independent of the code they test:

- Every one of the 69 four-in-a-row lines is enumerated and checked, plus explicit anti-tests
  for the two phantom wins a layout without the sentinel row would report.
- The solver is checked against a **second, deliberately naive solver written inside the
  test** — a plain grid and an obvious four-direction scan — on every early position of 4×4
  and 5×4 boards, which are small enough to settle completely. Two independent implementations
  agreeing is the real correctness argument, and it depends on no published number.
- Mirroring left-to-right is an exact symmetry, so it is asserted over thousands of random
  games; almost any indexing error breaks it.
- Positions are counted two independent ways — by transposition key and by a written-out grid
  — and must agree at every ply.
- The transposition table must never change an answer: the same corpus is solved with it on
  and off and the scores compared.

## Playing alongside the other Villager Games mods

The trade-screen button stacks below any sibling mod's button rather than overlapping it.
Note that a single villager can be in a chess game *and* a checkers game *and* a Connect Four
game at the same time — each mod tracks its own sessions. Harmless, just not coordinated.
