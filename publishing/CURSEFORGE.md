# CurseForge listing - Villager Connect Four

Copy-paste material for the CurseForge project. Form values live in `UPLOAD_CHECKLIST.md`.

CurseForge's editor is a rich-text box rather than Markdown, so paste the body below and then
re-apply the headings, the table and the bold runs by hand. Keep the section order - it is
written so the first screenful explains the hook.

---

## Summary (the short one-liner field)

Play Connect Four against villagers! A button on the trade screen starts a full game - their skill scales with trading level, and because the villager drops first, a Master has solved the game and cannot be beaten.

---

## Description (paste everything below this line)

# Villager Connect Four

That Master-level librarian drives a hard bargain. They have also solved Connect Four, and they are going first.

Open any villager's trade screen and hit **Connect Four**. A full board opens right there - no items, no commands, no setup.

## The villager goes first, and that is the point

Connect Four is a **solved game**. On the standard 7×6 board the player who drops first can force a win - that is a proved result, not a rule of thumb. Whoever moves first is playing a won game.

So the villager gets it. A Master villager isn't "very strong": it is **unbeatable**. If you would rather have the winning side, one line of config hands it to you. Your colour follows the turn order, so whoever drops first plays yellow.

## How strong are they?

A villager's **trading level sets its strength**, and each level thinks in a genuinely different way rather than just deeper:

| Villager level | Connect Four strength |
|---|---|
| Novice | Drops almost at random - won't take a win, won't block yours |
| Apprentice | Takes the win in front of it, blocks yours, sees nothing further |
| Journeyman | Never plays into a loss; makes and spots double threats |
| Expert | Understands odd/even threat parity |
| Master | Has solved the game |

The Journeyman-to-Expert step is the interesting one. Every column holds an even number of cells, so in a pure filling race the first player takes the 1st, 3rd and 5th cell of a column and the second takes the 2nd, 4th and 6th. A threat only pays off if its row falls to *its owner* - the same three-in-a-row one row higher can be worth nothing at all. A Journeyman counts threats; an Expert counts the ones that will actually be reached.

## How to play

- **Click a column**, or anywhere above it. A ghost disc hangs over the column you are pointing at.
- A **full column** flashes red with a dull note rather than quietly ignoring you.
- The **winning four are outlined** and the rest of the board dims.
- **Resign** with the button, or just close the board. Walking more than 16 blocks away, the villager dying, or disconnecting abandons the game.
- No stakes, no rewards - just for fun.

## Configuration

`config/villagerconnectfour.properties`, written on first run and documented inline:

- `first-player=player` - you drop first, and get the theoretically winning side.
- `board-size=7x6` - anything from `4x4` to `8x7`. This changes the *value* of the game, not just its scale: several smaller boards are draws with perfect play.

## Under the hood

- A **from-scratch pure-Java engine**. The board is two bitboards in a single 64-bit word, one column per seven bits; the spare bit per column is a sentinel that stays empty, which is what lets four-in-a-row be found by and-ing the board with shifted copies of itself with no edge checks.
- Scores are counted in **discs on the board** rather than plies from the search root, so they are absolute facts about a position. One transposition table is shared by every game on the server, and a Master's knowledge compounds through a game rather than starting cold each move.
- Fully **server-authoritative** - the client sends a column and nothing else.
- The engine thinks on a background thread, so the server never stalls.
- **Zero art**: board, discs, holes and highlights are drawn procedurally and stay crisp at any size.

## Compatibility

Plays nicely with the other Villager Games mods - the trade-screen buttons stack instead of overlapping.

Requires **Fabric API**. Client and server both need the mod.

---

## Gallery captions

1. Lead image - "The Connect Four button on any villager's trade screen"
2. "The villager drops first - it plays yellow, and on 7×6 that side wins"
3. "A ghost disc shows where yours will land"
4. "Two threats at once - one of them is getting through"
5. "The winning four, called out"
6. "A Master villager thinking… it has solved this"
