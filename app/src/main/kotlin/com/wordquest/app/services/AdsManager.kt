package com.wordquest.app.services

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android counterpart to iOS's `AdsManager`. Loads a single native ad for the
 * "suggested for you" slot on Home — same design choice as iOS: native ads
 * hand over raw content (headline, body, icon, call-to-action) and the app
 * builds the surrounding card itself, rather than dropping in a traditional
 * banner/interstitial.
 *
 * Ships wired to Google's public test native-ad unit ID, safe to build and
 * run today with no AdMob account. Swap [NATIVE_AD_UNIT_ID] for a real ad
 * unit ID from your own AdMob account before release — test ads are not
 * eligible for real payouts and Google will flag production traffic on test
 * IDs.
 */
object AdsManager {
    private const val TAG = "AdsManager"
    const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd.asStateFlow()

    private var adLoader: AdLoader? = null

    /**
     * [personalized] should reflect the player's actual consent status (see
     * [TrackingManager]) — when false, the request is explicitly marked
     * non-personalized ("npa=1") rather than silently guessing.
     */
    fun loadAd(context: Context, personalized: Boolean) {
        val loader = AdLoader.Builder(context.applicationContext, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                _nativeAd.value?.destroy()
                _nativeAd.value = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                }
            })
            .build()
        adLoader = loader

        val extras = Bundle().apply { putString("npa", if (personalized) "0" else "1") }
        val request = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
        loader.loadAd(request)
    }
}
