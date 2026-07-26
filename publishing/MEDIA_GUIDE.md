# Media guide + in-game QA — Villager Connect Four 0.1.0

> ## Status: not yet QA'd in game
>
> The mod builds clean (88 tests, 11 classes) and the dev client boots with it loaded, the
> config written and read back. **Nothing below has been played through yet.** Every VERIFY
> item is outstanding, and no screenshot or clip has been captured.

Every scene has a **VERIFY** part (QA) and a **CAPTURE** part (marketing). Play through in
order — if a VERIFY item fails, stop, fix, rebuild, and restart the session. **Nothing gets
uploaded until every VERIFY passes.**

---

## 1. Setup

Carried over from the Checkers session, where each of these cost real time to work out.

1. **Launch:** `.\gradlew.bat runClient` from the project root.
2. **World:** Creative with cheats. **26.2 renamed the game rules** — the old camelCase names
   are rejected outright:

   | 26.2 | pre-26.2 |
   |---|---|
   | `/gamerule advance_time false` | `doDaylightCycle` |
   | `/gamerule advance_weather false` | `doWeatherCycle` |
   | `/gamerule send_command_feedback false` | `sendCommandFeedback` |

   Type `/gamerule ` and read the suggestion list if one is rejected. Set
   `send_command_feedback false` before any capture — it stops command output sitting in the
   chat overlay for ~10s between shots. Then `/time set noon`.
3. **Villagers:** summon one per level.
   ```
   /summon minecraft:villager 274 64 -3138 {VillagerData:{profession:"minecraft:librarian",level:1,type:"minecraft:plains"},NoAI:1b,Invulnerable:1b,Tags:["shot"]}
   ```
   `NoAI:1b` holds the villager still and holds its rotation, so framing stays put between
   takes. `Invulnerable:1b` means a stray click can't hurt it. Face it at the camera with
   `/data merge entity @e[tag=shot,limit=1] {Rotation:[90f,0f]}` (yaw 90 = west).
4. **Video settings:** maximized window (client area **1920×1009**), **GUI scale 4**,
   particles minimal, F3 hidden. Screenshots: **F2** → `run\screenshots\`.
   ⚠ Set the scale **in game** (Options → Video Settings → GUI Scale), not by editing
   `options.txt` — Minecraft rewrites that file on exit and resets it to `guiScale:0`.
5. **Empty your hand** (`/clear`). In creative a left-click breaks a block and a right-click
   places one; with a block in hand a mis-aimed click quietly redecorates the village.
6. **Kill the tutorial toasts.** A fresh profile shows "Move with W, A, S and D" and then
   "Look around" in the top-right corner, and they sit there through every screenshot. Trigger
   and let them expire before shooting.
7. **Confirm the config before capturing.** `first-player=villager` and `board-size=7x6` — the
   defaults, but the Checkers session was nearly ruined by capturing under a config that had
   been left on a non-default value from earlier testing. Check the log line
   (`board 7x6, villager moves first`), not the file.

---

## 2. Scenes

### 2A. The button — ① `01-connect-four-button.png`

**VERIFY**
- The trade screen shows a **Connect Four** button to the right of the panel.
- The label is **not truncated** at GUI scales 1, 2, 3 and 4. This is the specific failure
  Villager Checkers shipped with ("Play Checker") — the fix is carried over, but "Connect
  Four" is a wide label and this is the scene that proves it.
- Clicking closes the trade screen and opens the board.

**CAPTURE** — trade screen with the button, villager visible, clean backdrop.

### 2B. The villager opens — ② `02-villager-drops-first.png`

**VERIFY** — *the single most important check in this document.*
- The board opens and the villager **has already dropped, or is visibly thinking**, without
  the player touching anything.
- The status line reads "*name* is thinking…" and the villager panel shows the animated dots.
- Its disc is **yellow**; the player's ghost disc is **red**.
- Against a Master, the first drop is the **centre column** (the opening book).

This is the one place the mod departs from both sibling mods, and the one thing most likely
to be silently wrong.

**CAPTURE** — the board just after the villager's opening drop.

### 2C. Ghost disc — ③ `03-ghost-disc.png`

**VERIFY**
- Moving the cursor across the board moves a translucent disc along the headroom row.
- The whole hovered column is tinted.
- The ghost is the **player's** colour, and does not appear when it is not their turn.
- Hovering a **full** column shows no ghost.

**CAPTURE** — cursor over a middle column, ghost hanging above it.

### 2D. A disc falling — ④ `04-disc-falling.png`

**VERIFY**
- The disc accelerates rather than sliding at constant speed, and squashes very slightly on
  landing.
- It lands in the **lowest free cell**, not on top of the stack.
- Clicking during a fall is never swallowed and never desyncs the board.
- `WOOD_PLACE` plays on each drop.

**CAPTURE** — mid-fall. Needs a burst; F2 while the disc is in the air.

### 2E. A double threat — ⑤ `05-double-threat.png`

**VERIFY**
- Set up two open threats against a **Novice** and confirm it fails to block either.
- Against a **Journeyman**, confirm it blocks and does not play into the cell below a threat.

**CAPTURE** — the board one move before the win, both threats visible.

### 2F. The winning four — ⑥ `06-winning-four.png`

**VERIFY**
- The four winning discs are **outlined**, every other disc **dims**.
- The banner reads "You win!" and the action button becomes **Close**.
- `UI_TOAST_CHALLENGE_COMPLETE` plays on a player win.
- The villager plays `VILLAGER_NO` when it loses, `VILLAGER_CELEBRATE` when it wins,
  `VILLAGER_YES` on a draw.

**CAPTURE** — the win banner with the outlined four behind it.

### 2G. Full column refusal

**VERIFY**
- Filling a column and clicking it flashes the column red and plays `NOTE_BLOCK_BASS`.
- No packet is sent and the turn does not pass.

### 2H. Draw

**VERIFY**
- Fill all 42 cells with no line: the banner reads "Draw" and the villager plays
  `VILLAGER_YES`.

### 2I. The solved-game hint

**VERIFY**
- Lose to a **level 5** villager with `first-player=villager`: the end banner shows the extra
  line pointing at `first-player=player`.
- The hint does **not** appear when the player moved first, or against lower levels.

---

## 3. Session and abandon paths

**VERIFY each:**
- Walk more than 16 blocks from the villager → "Game abandoned".
- Kill the villager mid-game → abandoned.
- Disconnect and rejoin → the session is gone, no leak.
- Close the board with Esc mid-game → resigns; the villager celebrates.
- The **Resign** button does the same.
- Open a game, close it, and open another with the same villager → works, no "already
  playing" message.
- A second player challenging the same villager → "This villager is already playing
  Connect Four!".

---

## 4. Config paths

**VERIFY each, restarting the client between changes** (settings are read once at startup):
- `first-player=player` → the player drops first, the player is **yellow**, the villager is
  red, and the board opens waiting for the player rather than thinking.
- `board-size=5x4` → a smaller board renders correctly, scales, and plays to a conclusion.
- `board-size=9x7` → rejected in the log with a warning, falls back to 7x6.
- `board-size=banana` → same.

---

## 5. Difficulty ladder by feel

Play at least one game against each level and confirm the character, not just the strength:

| Level | What to look for |
|---|---|
| 1 Novice | Ignores an open three of yours; fails to take its own win |
| 2 Apprentice | Takes wins and blocks yours, but fills the cell under your winning square |
| 3 Journeyman | Stops doing that; sets up double threats |
| 4 Expert | Prefers threats on rows that will fall to it; declines to give you yours |
| 5 Master | Opens centre instantly, then never gives anything back |

---

## 6. Cross-mod check

**VERIFY** with the Chess and Checkers jars also installed:
- All three buttons appear, **stacked, not overlapping**.
- Each opens its own game.
- One villager can hold a game in each simultaneously without either breaking.
