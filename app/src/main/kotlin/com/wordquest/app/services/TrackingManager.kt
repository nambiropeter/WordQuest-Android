package com.wordquest.app.services

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Android counterpart to iOS's `TrackingManager` (App Tracking Transparency).
 * Android has no ATT equivalent; the nearest analogue is Google's User
 * Messaging Platform (UMP) consent flow, which — like ATT — gates whether
 * ads may be personalized, and which the SDK only actually shows a form for
 * where consent law requires it (EEA/UK); everywhere else `canRequestAds()`
 * is simply true immediately with no prompt, which is a real behavioral
 * difference from ATT's need to show every user a system dialog.
 */
object TrackingManager {
    var canRequestAds: Boolean = false
        private set
    var isPersonalizationAllowed: Boolean = false
        private set

    private lateinit var consentInformation: ConsentInformation

    fun refreshStatus(activity: Activity) {
        if (!::consentInformation.isInitialized) {
            consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        }
        canRequestAds = consentInformation.canRequestAds()
        isPersonalizationAllowed = consentInformation.privacyOptionsRequirementStatus !=
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED || canRequestAds
    }

    /** Requests consent info and shows the UMP form only where the SDK decides it's required. */
    fun requestIfNeeded(activity: Activity, onDone: () -> Unit = {}) {
        val params = ConsentRequestParameters.Builder().build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    canRequestAds = info.canRequestAds()
                    onDone()
                }
            },
            {
                canRequestAds = info.canRequestAds()
                onDone()
            },
        )
    }

    /** Reopens the privacy options form so the player can change their choice later, mirroring Settings' toggle. */
    fun showPrivacyOptionsForm(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            canRequestAds = consentInformation.canRequestAds()
            onDone()
        }
    }
}
