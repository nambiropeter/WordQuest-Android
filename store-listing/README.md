# Play Console store listing assets

Everything here is ready to paste or upload into the Play Console "Main store
listing" page. Generated 2026-09-24 against the app at versionCode 1.

| Console field | File | Status |
|---|---|---|
| App name | — | `WordQuest` (9 / 30) |
| Short description | `short-description.txt` | 79 / 80 |
| Full description | `full-description.txt` | 1,940 / 4,000 |
| App icon | `graphics/app-icon-512.png` | 512x512 PNG, 29 KB |
| Feature graphic | `graphics/feature-graphic-1024x500.png` | 1024x500 PNG, 59 KB |
| Phone screenshots | `screenshots/01..05` | 5 x 1080x1920 PNG (9:16) |
| Video | — | optional, not supplied |

## Screenshots

Captured from the real app on a Pixel emulator forced to 1080x1920 so the
output is natively 9:16 — a stock Pixel screenshot is 1080x2424 (9:20), which
is outside Play's accepted range. All five are >= 1080 px on the short side,
so the listing clears the "eligible for promotion" bar (>= 4 screenshots,
>= 3 at 16:9 or 9:16 and >= 1080 px).

Upload them in numeric order; the first is what most users see.

1. `01-home.png` — mode picker, theme shortcuts, star/coin counters
2. `02-themes.png` — all eight themes
3. `03-word-search.png` — mid-puzzle, 3 of 5 words traced
4. `04-trivia.png` — question with the 50:50 lifeline and timer
5. `05-multiplayer.png` — local multiplayer host/join

## Description claims — all verified against the source

Nothing in the copy is aspirational; each claim maps to real code:

- 8 themes, 1,000 levels per theme per mode — `themes.json`, `GameMode.LEVEL_COUNT`
- 8x8/5 words up to 14x14/10 words — `LevelCatalog.wordSearchLevel`
- Hints get scarcer with difficulty — `hintsAllowed = max(1, 3 - band/4)`
- 3 stars = under par time with no hint used — `WordSearchViewModel.computeStars`
- Ten tiers of 100 — `LevelProgress.TIER_SIZE` / `TIER_COUNT`
- 5 trivia questions per level, streak, 50:50 — `TriviaViewModel`
- Ongoing round notification — `TriviaActivityNotifier`
- Up to 8 local players, no internet — `NearbyMultiplayerService.MAX_PEERS`
- No ads, no tracking — `AdsManager` / `TrackingManager` deleted
- System/light/dark, sound + haptics toggles — `SettingsStore`

**Deliberately not claimed: leaderboards and achievements.** The Play Games
code is wired up but inert until an App ID is set in
`app/src/main/res/values/play_games.xml`. Advertising them before that is
configured would be a false claim on the listing. Add them to the description
once Play Games Services is live in Console.

## Still required before the listing can be submitted

- Privacy policy URL (required for every app, no exceptions)
- Data safety form — declare the multiplayer display name; there is no
  analytics or ads SDK to disclose
- Content rating questionnaire
- Target audience and content declaration
