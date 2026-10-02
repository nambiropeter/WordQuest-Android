# Trivia unique-question work — progress & handoff

**Goal:** every trivia theme has enough *unique* questions per difficulty tier that no
question repeats within a difficulty band: **easy ≥ 500, medium ≥ 700, hard ≥ 800**
(LevelCatalog: bands 0-2 easy × 5 q/level, 3-6 medium up to 7 q/level, 7-9 hard up to 8 q/level).
Android and iOS files must stay byte-identical.

**Files:** `app/src/main/assets/content/trivia_<theme>.json` (Android) mirrored to
`../wordsearch_ios/WordQuest/Resources/Content/trivia_<theme>.json` (iOS).

## How to continue (new chat)
1. `python3 tools/trivia/status.py` — shows what's left (table below is refreshed automatically).
2. Write a batch file, one question per line:
   `difficulty|question|correct answer|wrong 1|wrong 2|wrong 3`
3. `python3 tools/trivia/add.py <theme> <batchfile>` — rejects exact and near-duplicates,
   appends, mirrors to iOS, and updates this file. (`--dry` to preview.)
4. `python3 tools/trivia/rm.py <theme> "<exact question text>" ...` removes bad entries.
5. Work order: animals ✔ → music ✔ → food ✔ → geography ✔ → history ✔ → movies ✔ → science ✔ → sports ✔. Phase 1 complete.
   Weight effort to hard/medium; check facts carefully, avoid ambiguous answers.

## Phase 2 — reworded-duplicate cleanup (after all tiers are full)
`add.py` blocks exact repeats and rewordings that share an answer. Older content still has
some reworded repeats (same fact, different wording, e.g. "largest US state by area" vs
"by land area"). Run `python3 tools/trivia/audit.py <theme>`: it lists same-answer pairs
with high word overlap. Many are false positives (e.g. "On which continent is Japan / Vietnam").
Review by hand, `rm.py` the true repeats, then top tiers back up with `add.py`.
Phase 2 progress: COMPLETE for all 8 themes (remaining audit.py hits are distinct facts that share an answer, e.g. different athletes who play tennis — leave them).

## Status
<!-- STATUS -->
_Last updated: 2026-10-01 14:50_

| theme | easy | medium | hard | state |
|---|---|---|---|---|
| animals | 500/500 | 700/700 | 800/800 | DONE |
| food | 500/500 | 701/700 | 800/800 | DONE |
| geography | 502/500 | 700/700 | 800/800 | DONE |
| history | 500/500 | 700/700 | 800/800 | DONE |
| movies | 500/500 | 701/700 | 800/800 | DONE |
| music | 500/500 | 831/700 | 823/800 | DONE |
| science | 500/500 | 700/700 | 800/800 | DONE |
| sports | 500/500 | 701/700 | 800/800 | DONE |
<!-- STATUS -->
