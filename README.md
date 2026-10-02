# WordQuest — Android

A native Android port of the iOS WordQuest app (`../wordsearch_ios`), built with
Kotlin and Jetpack Compose — Compose is the native Android counterpart to
SwiftUI, so this is a from-scratch reimplementation, not a wrapper or a
cross-platform framework. It targets full feature parity with the iOS app and
matches its visual design (colors, gradients, layout, copy) as closely as
Android's own design language allows.

## Architecture

```
app/src/main/kotlin/com/wordquest/app/
  models/        Data classes mirroring iOS's Models/ (themes, puzzles, trivia, multiplayer wire messages, progress)
  data/          Content loading + level generation (ContentStore, LevelCatalog, SeededGenerator, WordSearchGenerator, ProgressStore, HouseAdProvider)
  services/      Platform integrations (Settings, Haptics, Sound, Play Games, Nearby Connections, trivia notification)
  viewmodels/    Android ViewModel + Compose state, mirroring iOS's ViewModels/ (WordSearch, Trivia, Multiplayer)
  ui/            Compose screens, navigation graph, shared components, and the theme/color/icon-mapping system
app/src/main/assets/content/   themes.json + words_*.json + trivia_*.json, copied byte-for-byte from the iOS bundle
```

State management: Jetpack `ViewModel` + Compose `mutableStateOf`/`StateFlow`,
the direct analogue of iOS's `ObservableObject` + `@Published`. Navigation:
Navigation-Compose with type-safe `@Serializable` routes, mirroring iOS's
`AppRoute` enum. Persistence: DataStore Preferences (settings, progress),
matching iOS's UserDefaults keys and defaults one-for-one.

## Feature parity checklist

| Feature | Status | Notes |
|---|---|---|
| Word Search gameplay (grid generation, drag-to-select, hints, scoring) | ✅ Done | Ported algorithm, not just UI — same seeded generation shape as iOS |
| Trivia gameplay (timer, streak, 50:50, scoring) | ✅ Done | |
| Home / Theme Select / Level Map / Settings screens | ✅ Done | Visuals matched (exact hex colors/gradients); fonts are Compose default, not iOS's `.rounded` system design |
| Level progress, unlock rules, star/coin accounting | ✅ Done | DataStore-only — no iCloud-equivalent cross-device sync attempted |
| Local multiplayer (Nearby Connections) | ✅ Done, untested on real hardware | Host-authoritative protocol ported faithfully from iOS's MultipeerConnectivity version; `Strategy.P2P_STAR` mirrors the same host/guest topology. Compiles and packages cleanly but was never run across two physical devices — Nearby Connections' BLE/Wi-Fi Direct discovery doesn't work reliably on emulators, so real-device testing is still needed |
| Ads / monetization | ❌ Removed by design | This build ships ad-free — no AdMob, no UMP consent flow. The "Suggested for you" card on Home is first-party only (recommends an unexplored theme or local multiplayer), never a paid placement |
| Play Games Services (Game Center analogue): sign-in, leaderboards, achievements | ✅ Done, inert until configured | Calls are real and wired in but silently no-op without setup; Play Console must be configured (App ID + matching leaderboard/achievement IDs) before anything appears (see Setup) |
| Live Activity analogue (Dynamic Island / Lock Screen) for solo trivia | ✅ Done via notification | No Android API is a direct equivalent; uses an ongoing `Notification` with a system-rendered chronometer countdown (`setChronometerCountDown`), the closest analogue to iOS's `Text(timerInterval:)`. Needs the `POST_NOTIFICATIONS` permission, requested at launch on API 33+ |
| Adaptive app icon | ✅ Done, approximate | XML `<inset>` safe-zone crop of the iOS icon art + solid purple background; not a hand-graded adaptive icon |
| Sound effects | ⚠️ Placeholder | Uses `ToneGenerator` generic tones, not iOS's actual sound assets — real SFX files are a nice-to-have, not blocking |
| "Rounded" typography | ⚠️ Not done | Compose default type scale; a bundled/Google Font matching iOS's rounded system font would close this gap |
| iCloud-equivalent progress sync | ❌ Not attempted | Play Games "Saved Games" is the candidate Android analogue; DataStore is local-only for now |

## Setup required before this is store-ready

None of the following block building or running the app today. This build
ships with no ads and no monetization — nothing to configure there.

1. **Play Games Services v2** (optional): in Play Console, enable Play Games
   Services for this app, link it to an Android OAuth client (your package
   name + the SHA-1 fingerprint of your signing keystore), then paste the
   resulting App ID into `play_games_app_id` in
   `app/src/main/res/values/play_games.xml` — the `APP_ID` meta-data is already
   declared in `AndroidManifest.xml` and points at that string, so this is the
   only code change needed. (It has to stay a string resource: a bare numeric
   `android:value` lands in the meta-data `Bundle` as an `int`, and the SDK
   reads the key with `Bundle.getString()`, which would return null.) Then
   create:
   - Leaderboards with IDs: `wq_word_search_total_score`, `wq_trivia_total_score`, `wq_total_stars`
   - Achievements with IDs: `wq_first_win`, `wq_perfect_trivia`, `wq_multiplayer_champion` (standard/unlockable),
     and `wq_hundred_levels` as an **incremental** achievement with **100 steps**
     (the code reports it via `percentComplete` on a 0–100 scale, same as iOS's `GKAchievement.percentComplete`).
   - These IDs are Android-side identifiers chosen independently of iOS's
     Game Center IDs (`wq_wordsearch_total_score`, etc.) — the two platforms'
     backends are entirely separate, so there's no requirement that the
     strings match, only that Android's code matches Android's Play Console config.
   - Until this is configured, sign-in silently fails and every call is a
     safe no-op (see `PlayGamesReporter`) — the app works fine without it.
     This was verified against the SDK bytecode, not just assumed: the only
     APP_ID read is in `PlayGamesInitProvider`'s app-shortcuts path, where the
     `Long.parseLong` of the empty default sits inside a caught
     `NumberFormatException`. A missing or empty App ID cannot crash launch.
     If you decide to ship without Play Games entirely, drop the
     `play-services-games-v2` dependency and point `WordQuestApplication` at
     the `NoOpGameServicesReporter` that already exists.

2. **Local multiplayer real-device test**: `SERVICE_ID` (`com.mamatiquest.app.trivia`)
   is already unique to this app, so no external registration is needed — just
   verify hosting/joining/gameplay across two real Android devices (or one
   real device + emulator, though BLE discovery from an emulator is unreliable).

3. **Signing**: this project has no `signingConfig` for the `release` build
   type — use Android Studio's **Build > Generate Signed App Bundle / APK**
   wizard with your own upload keystore (Play Console requires this for a
   new app; never commit the keystore or its passwords to git).

## Building

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # or any JDK 17+
./gradlew :app:assembleDebug    # debug APK, for local testing
./gradlew :app:bundleRelease    # release AAB (R8 + resource shrinking), for Play Store upload
```

Verified: both build successfully. `assembleDebug` produces
`app/build/outputs/apk/debug/app-debug.apk`. The R8-minified release build
was additionally smoke-tested end-to-end on an emulator (Home, Settings,
Word Search gameplay, Trivia gameplay) with no crashes.
