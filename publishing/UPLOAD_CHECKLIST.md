# Upload checklist - Villager Connect Four 0.1.0

Work top to bottom. **Do not upload until every box in step 0 is checked.**

## Status as of 2026-07-26 - both stores submitted, awaiting moderation

Media was captured and both listings were published on 2026-07-26.

- **Modrinth** - project `villagerconnectfour`. Visibility **Public**, state **Under review**
  (Modrinth moderates new projects; it is not "instant publish" as an earlier draft of this
  file claimed). Seven gallery images with captions, `01` set as banner. The `0.1.0` jar was
  already in place.
- **CurseForge** - project `villager-connect-four`, ID **1626494**. Seven gallery images,
  and `villagerconnectfour-0.1.0.jar` uploaded as `Villager Connect Four 0.1.0` (Release,
  26.2, Fabric, Java 25, Server+Client). That upload is what put the project into
  moderation, so it now reads **Processing**.
- **Fabric API** is set as a *Required Dependency* under **Settings > Default Relations**
  (`/minecraft/mc-mods/villager-connect-four/settings/default-relations`). That page stays
  editable during moderation, unlike the per-file one. Note it is a *default*: it attaches to
  files uploaded from now on, and the public Dependencies tab - which lists file-level
  relations - is still empty for the 0.1.0 file. Attach it to that file too once the project
  is approved.

### Still outstanding on CurseForge

**Seven em dashes are stuck in file changelogs.** `/project/<id>/files/<fileId>/edit` 404s
while a file is *Under Manual Review* and returns 200 once the project is approved, so these
cannot be fixed until moderation clears:

| File | Count | Where |
|---|---|---|
| Connect Four 0.1.0 (8515012) | 5 | the `0.1.0 - Initial release` heading, and the trade-screen, winning-side, board-size and absolute-scores bullets |
| Chess (8514361) | 1 | the trade-screen bullet |
| Checkers (8514247) | 1 | the trade-screen bullet |

Everything else on both stores - names, summaries, descriptions, gallery captions, and every
Modrinth changelog - is em dash free.
- GitHub repo `johnnyscriptt/villager-connect-four` is public, so the store links resolve.

## 0. Gate: QA + media session

- [x] `./gradlew build` green - 88 tests across 11 classes, jar in `build\libs`
- [x] Dev client boots with the mod loaded; config written and read back
      (`board 7x6, villager moves first` in the log)
- [x] **Media captured** - seven screenshots in `screenshots\`, two clips and the cut
      trailer in `trailer\`
- [x] `publishing\icon-512.png` - a 512x512 version of the mod's own
      `assets/villagerconnectfour/icon.png`. Replace both files together if the art is redone.
- [~] **QA session** - partially run. Passed: button label at GUI scales 1/2/3/4, the
      villager's opening drop (centre column vs a Master, yellow, without player input),
      the thinking state, ghost disc tracking (hidden on the villager's turn and over a full
      column), the fall animation, a Novice ignoring an open three and an unblocked double
      threat, the winning-four highlight with the dimmed board and Close button, the
      lose banner correctly *not* showing the solved-game hint at low level, full-column
      refusal (column flashes red, no disc, turn does not pass), Resign, Esc-close, and
      reopening a game with the same villager.
      **Not run:** sound cues, a drawn board, the solved-game hint against a level 5, the
      walk-away / kill-villager / disconnect abandon paths, the `first-player` and
      `board-size` config paths, the difficulty ladder by feel, and the cross-mod
      button-stacking check.
- [ ] Trailer uploaded to YouTube - optional but strongly recommended. The cut file is
      `trailer\villager-connect-four-trailer.mp4` (40s). Neither store gallery accepts video,
      so the trailer is not on either listing yet.

**Watch for the sibling mods' known trap:** Villager Checkers shipped once with its button
rendering as "Play Checker" because the label overran a hard-coded 70px width. The fix is
already carried over here (`VillagerConnectFourClient` sizes the button to its label and clamps
it to the screen edge), but *verify it at GUI scales 1–4* - "Connect Four" is a wide label.

## 1. The file you upload

- **`build\libs\villagerconnectfour-0.1.0.jar`** - this one and only this one.
- Never upload `villagerconnectfour-0.1.0-sources.jar`.
- Sanity check: the jar contains the MIT `LICENSE`, the icon, and contact URLs in
  `fabric.mod.json` (verified at build time).
- If you rebuild after a QA fix, re-check the filename - a version bump changes it and the
  stale jar is the easy mistake to upload.

## 2. Shared form values (both sites)

| Field | Value |
|---|---|
| Name | Villager Connect Four |
| Slug | `villagerconnectfour` |
| Summary | see `MODRINTH.md` / `CURSEFORGE.md` (same line) |
| Icon | `publishing\icon-512.png` |
| License | MIT |
| Loader | Fabric (Loader 0.19.3+) |
| Game version | 26.2 |
| Java version | 25 |
| Environment | Client **and** server - both required |
| Dependency | **Fabric API - Required** (built against `0.155.2+26.2`) |
| Version number | 0.1.0 |
| Release channel | **Release** |
| Changelog | paste the `## [0.1.0] - 2026-07-26` section from `CHANGELOG.md` |

## 3. Modrinth (do this first - instant publish)

1. modrinth.com → **Create a project** → fill name/slug/summary, visibility **Public** (or
   Draft until media is ready).
2. Description tab → paste the body from `MODRINTH.md`.
3. Settings: License **MIT**; Categories **Game Mechanics, Minigame, Social**; Environment
   **client & server: required**.
4. Links: Source `https://github.com/johnnyscriptt/villager-connect-four`, Issues `.../issues` -
   *these 404 while the repo is private; either make the repo public first or leave them blank.*
5. **Versions → Create version**: upload the jar, version `0.1.0`, channel Release, MC `26.2`,
   loader Fabric, add **Fabric API** as a required dependency, paste the changelog.
6. Gallery: upload the six screenshots with the captions from `MODRINTH.md`; set
   `01-connect-four-button.png` as **featured**.
7. Put the YouTube trailer link on its own line in the description - Modrinth auto-embeds it.

## 4. CurseForge (manual approval - hours to days)

1. legacy.curseforge.com → **Start a Project** → Minecraft → **Mods**. Name, slug, summary,
   icon as above.
2. Description → paste from `CURSEFORGE.md`, re-apply formatting in the editor.
3. Categories: **Miscellaneous** (primary) + Mobs. License: **MIT License** from the dropdown.
4. Source/Issues URLs: same GitHub caveat as above.
5. **Upload file**: the jar; display name `Villager Connect Four 0.1.0`; channel **Release**;
   game version **26.2**; loader **Fabric**; Java **25**; paste the changelog (Markdown type).
6. Relations tab on the file → **Fabric API** as *Required Dependency*.
7. Gallery images + embed the trailer, then submit for approval. Images can be added while
   waiting.

## 5. Repo

- [ ] `git init` done, initial conventional commit made
- [ ] Repo created under **johnnyscriptt** (personal), **private** by default
- [ ] Only make it public if the store links to it - otherwise the source/issue links 404

## 6. After both are live

- [ ] Open each public page in a private window: icon renders, tables render, file downloads,
      dependency shows Fabric API
- [ ] If the Modrinth slug ended up different from `villagerconnectfour`, update `homepage` in
      `src\main\resources\fabric.mod.json` for the next release
- [ ] Tag the release: `git tag v0.1.0 && git push --tags`

## Known nits (fine to ship, fix in 0.1.1)

- CurseForge "server pack" prompts don't apply to a mod - skip them.
- One villager can be in a chess game *and* a checkers game *and* a Connect Four game at the
  same time (each mod keeps its own session map). Noted in the README; harmless.
- A Master villager going first genuinely cannot be beaten. That is the design, it is explained
  in the README, on both store pages and in the end banner in-game - but expect it as a review
  comment anyway, and answer with `first-player=player` rather than by weakening the Master.
- Both sites let you edit everything (icon, description, gallery) after publishing except the
  uploaded file itself - file mistakes need a new version.
