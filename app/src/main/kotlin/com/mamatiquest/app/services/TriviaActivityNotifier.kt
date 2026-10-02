package com.mamatiquest.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mamatiquest.app.R
import com.mamatiquest.app.models.GameTheme

/**
 * Android counterpart to iOS's `TriviaActivityManager` (which drives a
 * Dynamic Island / Lock Screen Live Activity). Android has no direct
 * equivalent API, so this uses the closest analogue: a single ongoing,
 * updating [Notification] with a live system-rendered countdown
 * (`setUsesChronometer` + `setChronometerCountDown`, the same trick iOS uses
 * with `Text(timerInterval:)` — the system ticks the display itself with no
 * extra per-second work from the app). Every call is a safe no-op without
 * the `POST_NOTIFICATIONS` permission (API 33+) or when there's no active
 * notification to act on, mirroring the Swift original's availability guards.
 */
object TriviaActivityNotifier {
    private const val CHANNEL_ID = "trivia_round"
    private const val NOTIFICATION_ID = 4201
    private val mainHandler = Handler(Looper.getMainLooper())

    // Bumped on every start() so a delayed cancel Runnable from a round that already
    // ended can't cancel a brand-new round's live notification if one starts within
    // the delay window.
    private var generation = 0

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trivia round in progress",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows your live score and time remaining during a solo trivia round."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun start(
        context: Context,
        theme: GameTheme,
        level: Int,
        totalQuestions: Int,
        questionDeadlineMillis: Long,
    ) {
        if (!canNotify(context)) return
        generation++
        mainHandler.removeCallbacksAndMessages(null)
        ensureChannel(context)
        post(context, theme, level, questionIndex = 0, totalQuestions = totalQuestions, score = 0, streak = 0, questionDeadlineMillis = questionDeadlineMillis)
    }

    fun update(
        context: Context,
        theme: GameTheme,
        level: Int,
        questionIndex: Int,
        totalQuestions: Int,
        score: Int,
        streak: Int,
        questionDeadlineMillis: Long,
    ) {
        if (!canNotify(context)) return
        post(context, theme, level, questionIndex, totalQuestions, score, streak, questionDeadlineMillis)
    }

    fun end(context: Context, theme: GameTheme, level: Int, finalScore: Int) {
        if (!canNotify(context)) return
        val notification = builder(context)
            .setContentTitle("${theme.name} · Level $level complete!")
            .setContentText("Final score: $finalScore")
            .setUsesChronometer(false)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        val expectedGeneration = generation
        mainHandler.postDelayed({ if (generation == expectedGeneration) cancel(context) }, 4000)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun post(
        context: Context,
        theme: GameTheme,
        level: Int,
        questionIndex: Int,
        totalQuestions: Int,
        score: Int,
        streak: Int,
        questionDeadlineMillis: Long,
    ) {
        val streakSuffix = if (streak > 1) " · 🔥$streak" else ""
        val notification = builder(context)
            .setContentTitle("${theme.name} · Level $level")
            .setContentText("Question ${questionIndex + 1} of $totalQuestions · Score $score$streakSuffix")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(questionDeadlineMillis)
            .setProgress(totalQuestions, questionIndex, false)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun builder(context: Context) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_trivia)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
}
