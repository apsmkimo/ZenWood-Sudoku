<!-- SMCPKG_SUPPORT>>>Cursor001
# WoodCraft Sudoku

A high-performance, native Android Sudoku application designed for the global market. Built from the ground up using **Kotlin** and **Jetpack Compose**, this project delivers a premium mind-fitness experience wrapped in a serene, traditional paper-and-wood aesthetic.

### 🌟 Key Features
* **Elegant Visuals:** Traditional wood-grain textures paired with soft parchment-style grids for eye comfort during long sessions.
* **Extreme Performance:** Fully optimized with Compose Smart Recomposition and State Hoisting to ensure 60 FPS fluid rendering on mid-to-low-end Android devices.
* **Prepopulated Room DB:** Lightweight storage architecture packing 30,000 uniquely solvable pre-generated levels (Easy, Medium/Hard, Expert) via flattened 81-character string serialization.
* **State Preservation:** Robust temporary session saving to completely restore precise board states and candidate pencil notes even after background memory reclamation.
* **Global Architecture:** Clean, modular structure using MVVM/MVI, ready for internationalization (i18n) and monetization integration.
SMCPKG_SUPPORT<<<Cursor001 -->

# ZenWood-Sudoku

Native Android Sudoku app (`com.apsmkimo.zenwoodsudoku`, version 1.0.0). Kotlin, Jetpack Compose, Room, ViewModel, and StateFlow. The playable path is home → difficulty → next incomplete level → board, notes, undo, and hints.

UI copy lives in `app/src/main/res/values/strings.xml` so it can be translated. The board uses a parchment surface and dark-brown ink; pencil notes are a lighter gray-brown.

## Packages

- `data/local` — `SudokuLevel` entity, DAO, database, streaming importer
- `data/repository` — Room-backed `SudokuRepository`
- `domain/model` — difficulty, level, `saved_progress` codec
- `domain/game` — rules, notes, undo, hints (no Android APIs)
- `domain/repository` — repository contract
- `ui` — Compose home, import, and game screens

## Level catalog and first launch

`app/src/main/assets/sudoku_global_levels.json` is the pre-generated catalog: 30,000 puzzles, 10,000 each for difficulty `1` easy, `2` medium, and `3` master. Fields map directly onto Room: `id`, `difficulty`, `puzzle`, `solution`, `is_completed`, `best_time`, `saved_progress`. Puzzle and solution are 81-character strings, `0` = empty. The device does not generate puzzles.

Import runs only when `zenwood_meta` does not already record `levels_import_v1` with 30,000 rows. A partial table is cleared and loaded again, so a failed attempt does not leave a mix of old and new rows. `JsonReader` streams the asset on `Dispatchers.IO`. Rows are inserted in batches of 1,000 inside one Room transaction, and each batch list is cleared. The main thread only shows a progress screen. Peak Java memory is one batch of short strings plus SQLite’s page cache, not 30,000 live entities and not a fully parsed JSON tree.

The JSON is about 11 MB in git. That is under GitHub’s file limit; Git LFS is optional later if more catalogs are added. The APK still contains the asset, as requested.

### `saved_progress`

Version 1 JSON, no whitespace:

```json
{"v":1,"elapsedMs":0,"hintsRemaining":10,"selected":-1,"pencil":false,"board":"<81 digits>","notes":[0,0]}
```

`notes` has 81 integers. Bit `(digit - 1)` is set when that pencil mark is on. Given clues always come from `puzzle` and are not overwritten by the snapshot. Each level starts with 10 free hints. At 0, the hint control shows a play icon and calls `GameViewModel.onWatchAdForHint()`, which is a stub for a future rewarded ad and does not reveal a digit.

## Board recomposition

Each cell has its own `StateFlow<CellUi>`. `SudokuCell` takes primitives (`value`, `notesMask`, `isGiven`, `hasError`) and a stable click lambda. Selection is drawn by a separate underlay, so choosing a cell does not rebuild the other cells. Header, timer, pencil mode, and the hint count are collected only by the controls that display them.

## Build

Debug APK: GitHub Actions workflow `.github/workflows/build-apk.yml` (push, pull request, and `workflow_dispatch`). The artifact is attached to the workflow run and kept for 14 days.

```bash
./gradlew assembleDebug
```
