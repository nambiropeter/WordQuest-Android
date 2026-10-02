package com.mamatiquest.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the SF Symbol names carried over verbatim from iOS content (themes.json,
 * GameMode.icon, HouseAd.icon, HomeView's inline symbol names) to the closest
 * Material Icons Extended equivalent, so both apps read as the same game at a
 * glance even though the icon sets are completely different per platform.
 *
 * | SF Symbol                            | Material icon           | Used for               |
 * |---------------------------------------|--------------------------|------------------------|
 * | pawprint.fill                         | Pets                      | Animals theme           |
 * | globe.americas.fill                   | Public                    | Geography theme         |
 * | film.fill                             | Movie                     | Movies & TV theme       |
 * | atom                                  | Science                  | Science theme (no exact atom glyph in Material) |
 * | sportscourt.fill                      | SportsBasketball          | Sports theme            |
 * | fork.knife                            | Restaurant                | Food & Cooking theme    |
 * | building.columns.fill                 | AccountBalance            | History theme           |
 * | music.note                            | MusicNote                 | Music theme             |
 * | square.grid.3x3.fill                  | GridView                  | Word Search mode        |
 * | questionmark.circle.fill              | AutoMirrored.Help         | Trivia mode (Material's Help glyph isn't circled, closest available) |
 * | antenna.radiowaves.left.and.right      | SettingsInputAntenna      | Local multiplayer       |
 * | gearshape.fill                        | Settings                  | Settings entry          |
 * | star.fill                             | Star                      | Stars stat chip         |
 * | circle.hexagongrid.fill               | MonetizationOn            | Coins stat chip         |
 * | checkmark.seal.fill                   | Verified                  | Levels-completed chip   |
 * | chevron.right                         | ChevronRight              | Card disclosure arrow   |
 * | arrow.right                           | AutoMirrored.ArrowForward | "Play" theme card arrow |
 */
fun sfSymbolToIcon(name: String): ImageVector = when (name) {
    "pawprint.fill" -> Icons.Filled.Pets
    "globe.americas.fill" -> Icons.Filled.Public
    "film.fill" -> Icons.Filled.Movie
    "atom" -> Icons.Filled.Science
    "sportscourt.fill" -> Icons.Filled.SportsBasketball
    "fork.knife" -> Icons.Filled.Restaurant
    "building.columns.fill" -> Icons.Filled.AccountBalance
    "music.note" -> Icons.Filled.MusicNote
    "square.grid.3x3.fill" -> Icons.Filled.GridView
    "questionmark.circle.fill" -> Icons.AutoMirrored.Filled.Help
    "antenna.radiowaves.left.and.right" -> Icons.Filled.SettingsInputAntenna
    "gearshape.fill" -> Icons.Filled.Settings
    "star.fill" -> Icons.Filled.Star
    "circle.hexagongrid.fill" -> Icons.Filled.MonetizationOn
    "checkmark.seal.fill" -> Icons.Filled.Verified
    "chevron.right" -> Icons.Filled.ChevronRight
    "arrow.right" -> Icons.AutoMirrored.Filled.ArrowForward
    else -> Icons.Filled.Apps
}
