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
  services/      Platform integrations (Settings, Haptics, Sound, Ads, Play Games, Nearby Connections, ad consent, trivia notification)
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
| AdMob native ads on Home | ✅ Done, ships on Google's public **test** IDs | Swap for real AdMob IDs before release (see Setup below) |
| Ad consent (UMP, the ATT analogue) | ✅ Done | Behavioral difference from iOS: UMP only shows a consent form where required by law (EEA/UK); elsewhere `canRequestAds()` is true immediately with no prompt at all, unlike ATT's mandatory system dialog everywhere |
| Play Games Services (Game Center analogue): sign-in, leaderboards, achievements | ✅ Done, inert until configured | Calls are real and wired in; Play Console must be configured with matching leaderboard/achievement IDs before anything appears (see Setup) |
| Live Activity analogue (Dynamic Island / Lock Screen) for solo trivia | ✅ Done via notification | No Android API is a direct equivalent; uses an ongoing `Notification` with a system-rendered chronometer countdown (`setChronometerCountDown`), the closest analogue to iOS's `Text(timerInterval:)`. Needs the `POST_NOTIFICATIONS` permission, requested at launch on API 33+ |
| Adaptive app icon | ✅ Done, approximate | XML `<inset>` safe-zone crop of the iOS icon art + solid purple background; not a hand-graded adaptive icon |
| Sound effects | ⚠️ Placeholder | Uses `ToneGenerator` generic tones, not iOS's actual sound assets — real SFX files are a nice-to-have, not blocking |
| "Rounded" typography | ⚠️ Not done | Compose default type scale; a bundled/Google Font matching iOS's rounded system font would close this gap |
| iCloud-equivalent progress sync | ❌ Not attempted | Play Games "Saved Games" is the candidate Android analogue; DataStore is local-only for now |

## Setup required before this is store-ready

None of the following block building or running the app today (everything
above runs on Google's public test/sandbox IDs) — they're required before
ads, leaderboards, or achievements show real data in production:

1. **AdMob**: create a real app in the [AdMob console](https://apps.admob.com),
   then replace:
   - `ca-app-pub-3940256099942544~3347511713` in `AndroidManifest.xml`'s
     `com.google.android.gms.ads.APPLICATION_ID` meta-data, with your real App ID.
   - `AdsManager.NATIVE_AD_UNIT_ID` in `services/AdsManager.kt`, with a real
     native ad unit ID.
   - If you plan to serve ads in the EEA/UK/US, configure a consent message
     under AdMob's **Privacy & messaging** (Funding Choices) — `TrackingManager`
     already calls `UserMessagingPlatform.loadAndShowConsentFormIfRequired`,
     it just has nothing to show until a message is configured there.

2. **Play Games Services v2**: in Play Console, enable Play Games Services for
   this app, link it to an Android OAuth client (your package name +
   the SHA-1 fingerprint of your signing keystore), then create:
   - Leaderboards with IDs: `wq_word_search_total_score`, `wq_trivia_total_score`, `wq_total_stars`
   - Achievements with IDs: `wq_first_win`, `wq_perfect_trivia`, `wq_multiplayer_champion` (standard/unlockable),
     and `wq_hundred_levels` as an **incremental** achievement with **100 steps**
     (the code reports it via `percentComplete` on a 0–100 scale, same as iOS's `GKAchievement.percentComplete`).
   - These IDs are Android-side identifiers chosen independently of iOS's
     Game Center IDs (`wq_wordsearch_total_score`, etc.) — the two platforms'
     backends are entirely separate, so there's no requirement that the
     strings match, only that Android's code matches Android's Play Console config.

3. **Local multiplayer real-device test**: `SERVICE_ID` (`com.wordquest.app.trivia`)
   is already unique to this app, so no external registration is needed — just
   verify hosting/joining/gameplay across two real Android devices (or one
   real device + emulator, though BLE discovery from an emulator is unreliable).

4. **Signing**: swap the debug signing config for a real release keystore
   before publishing.

## Building

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # or any JDK 17+
./gradlew :app:assembleDebug
```

Verified: `BUILD SUCCESSFUL`, producing `app/build/outputs/apk/debug/app-debug.apk`.
