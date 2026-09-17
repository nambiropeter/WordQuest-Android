package com.wordquest.app.services

import android.app.Activity
import android.util.Log
import com.google.android.gms.games.PlayGames
import java.lang.ref.WeakReference

/**
 * Android counterpart to iOS's `GameCenterManager` — every Play Games
 * Services v2 call in the app funnels through this singleton, and every call
 * is a safe no-op when the player isn't signed in, exactly like the Swift
 * original's `guard isAuthenticated` checks.
 *
 * Play Games v2's clients (`AchievementsClient`, `LeaderboardsClient`) need a
 * live `Activity`, not just any `Context`, so [attach] must be called from
 * `MainActivity.onCreate` before any score/achievement calls can do anything.
 * Leaderboard and achievement IDs below must be created with matching
 * identifiers in Play Console before real data appears there — see the
 * project README for the exact setup steps.
 */
object PlayGamesReporter : GameServicesReporter {
    private const val TAG = "PlayGamesReporter"

    @Volatile private var activityRef: WeakReference<Activity>? = null
    @Volatile var isAuthenticated: Boolean = false
        private set

    /** Call once from `MainActivity.onCreate`. Triggers sign-in immediately. */
    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        val signInClient = PlayGames.getGamesSignInClient(activity)
        signInClient.isAuthenticated.addOnCompleteListener { task ->
            isAuthenticated = task.isSuccessful && task.result.isAuthenticated
            if (!isAuthenticated) {
                signInClient.signIn().addOnCompleteListener { signInTask ->
                    isAuthenticated = signInTask.isSuccessful && signInTask.result.isAuthenticated
                }
            }
        }
    }

    override fun submitScore(score: Int, leaderboardId: String) {
        val activity = activityRef?.get() ?: return
        if (!isAuthenticated || score <= 0) return
        runCatching {
            PlayGames.getLeaderboardsClient(activity).submitScore(leaderboardId, score.toLong())
        }.onFailure { Log.w(TAG, "Leaderboard submission failed: ${it.message}") }
    }

    /**
     * [percentComplete] mirrors iOS's `GKAchievement.percentComplete` (0-100).
     * Play Console achievements configured as "incremental" with 100 total
     * steps map onto this directly via `setSteps`; a "standard" (all-or-
     * nothing) achievement should just be unlocked once percentComplete
     * reaches 100 — both calls are safe to make regardless of which type the
     * achievement was actually configured as.
     */
    override fun reportAchievement(achievementId: String, percentComplete: Double) {
        val activity = activityRef?.get() ?: return
        if (!isAuthenticated) return
        runCatching {
            val client = PlayGames.getAchievementsClient(activity)
            if (percentComplete >= 100.0) {
                client.unlock(achievementId)
            } else {
                client.setSteps(achievementId, percentComplete.toInt())
            }
        }.onFailure { Log.w(TAG, "Achievement report failed: ${it.message}") }
    }

    /** Shows the built-in Play Games achievements UI, the closest analogue to `GKAccessPoint`. */
    fun showAchievements(activity: Activity) {
        if (!isAuthenticated) return
        runCatching {
            PlayGames.getAchievementsClient(activity).achievementsIntent
                .addOnSuccessListener { intent -> activity.startActivityForResult(intent, ACHIEVEMENTS_REQUEST_CODE) }
        }
    }

    private const val ACHIEVEMENTS_REQUEST_CODE = 9001
}
