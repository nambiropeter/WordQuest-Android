package com.mamatiquest.app.services

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.mamatiquest.app.BuildConfig
import com.mamatiquest.app.models.GameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob, kept deliberately out of the way of play: an opt-in rewarded ad (extra
 * hint), native ads drawn as the app's own cards on menu screens (see
 * NativeAdCard), and one interstitial after every 10th solo game of each mode —
 * shown only once the player leaves the result screen, never mid-game.
 * The Play listing targets all ages (Families policy), so every request is
 * child-directed and capped at G-rated content, and AD_ID is stripped in the manifest.
 * Debug builds use Google's sample ad units so test taps never hit real inventory.
 * iOS counterpart: `Services/Ads/AdsManager.swift`.
 */
object AdsManager {
    private val INTERSTITIAL_ID =
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-7633404067070211/9808457199"
    private val NATIVE_ID =
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-7633404067070211/2224557209"
    private val REWARDED_ID =
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else "ca-app-pub-7633404067070211/4643585412"

    private val initStarted = AtomicBoolean(false)
    private val _isReady = MutableStateFlow(false)
    /** True once consent allows requests and the SDK is initialized. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True where the law (e.g. GDPR) requires a way to revisit the consent choice; drives Settings' row. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val _rewardedAvailable = MutableStateFlow(false)
    val rewardedAvailable: StateFlow<Boolean> = _rewardedAvailable.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false

    /** Solo games of one mode between interstitials (word search and trivia count separately). */
    private const val GAMES_PER_INTERSTITIAL = 10
    private const val PREFS = "ads"
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    /**
     * Gathers GDPR/US-state consent through Google's UMP form (shown only where
     * required, and only if a message is configured in AdMob), then starts the SDK.
     */
    fun start(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(true).build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    _privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (consent.canRequestAds()) initialize(activity)
                }
            },
            { if (consent.canRequestAds()) initialize(activity) },
        )
        // Consent from a previous session lets ads start without waiting on the network.
        if (consent.canRequestAds()) initialize(activity)
    }

    /** Re-opens Google's consent form so the player can change their ad privacy choice. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
    }

    private fun initialize(activity: Activity) {
        if (!initStarted.compareAndSet(false, true)) return
        val appContext = activity.applicationContext
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        MobileAds.initialize(appContext) {
            _isReady.value = true
            loadRewarded(appContext)
            loadInterstitial(appContext)
        }
    }

    private fun loadRewarded(context: Context) {
        if (rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(context, REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedLoading = false
                rewardedAd = ad
                _rewardedAvailable.value = true
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedLoading = false
                rewardedAd = null
                _rewardedAvailable.value = false
            }
        })
    }

    private fun loadInterstitial(context: Context) {
        if (interstitialAd != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(context, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialLoading = false
                interstitialAd = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialLoading = false
                interstitialAd = null
            }
        })
    }

    /**
     * Call when the player leaves a finished solo game of [mode]. Every 10th game
     * of that mode shows an interstitial first; [then] (the navigation they tapped)
     * runs once it closes, or straight away otherwise. If no ad is loaded on the
     * 10th game, the count stays due so the next game tries again.
     */
    fun afterSoloGame(activity: Activity, mode: GameMode, then: () -> Unit) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = when (mode) {
            GameMode.TRIVIA -> "triviaGamesSinceInterstitial"
            GameMode.WORD_SEARCH -> "wordSearchGamesSinceInterstitial"
        }
        val played = prefs.getInt(key, 0) + 1
        val ad = interstitialAd
        if (played < GAMES_PER_INTERSTITIAL || ad == null) {
            prefs.edit().putInt(key, played).apply()
            if (ad == null && _isReady.value) loadInterstitial(activity.applicationContext)
            then()
            return
        }
        prefs.edit().putInt(key, 0).apply()
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                then()
                loadInterstitial(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                then()
                loadInterstitial(activity.applicationContext)
            }
        }
        ad.show(activity)
    }

    /** Loads one native ad for a menu card; [onLoaded] isn't called if nothing fills. */
    fun loadNative(context: Context, onLoaded: (NativeAd) -> Unit) {
        AdLoader.Builder(context, NATIVE_ID)
            .forNativeAd(onLoaded)
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /**
     * Shows the rewarded ad if one is loaded. [onReward] runs only if the player
     * watched it through; [onFinished] runs once the ad is gone either way.
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onFinished: () -> Unit = {}) {
        val ad = rewardedAd ?: run {
            loadRewarded(activity.applicationContext)
            onFinished()
            return
        }
        rewardedAd = null
        _rewardedAvailable.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onFinished()
                loadRewarded(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onFinished()
                loadRewarded(activity.applicationContext)
            }
        }
        ad.show(activity) { onReward() }
    }
}
