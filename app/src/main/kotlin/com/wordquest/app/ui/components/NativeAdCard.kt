package com.wordquest.app.ui.components

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Android counterpart to iOS's `NativeAdContainerView` (a `UIViewRepresentable`
 * wrapping `NativeAdView`). Google's native ad SDK requires a real Android
 * View for click/impression tracking — Compose can't render one directly —
 * so this wraps a hand-built `NativeAdView` via `AndroidView`, styled to
 * match the app's own card language rather than Google's default template.
 * The small "Ad" badge stays: a native ad still has to be identifiable as one.
 */
@Composable
fun NativeAdCard(nativeAd: NativeAd, tintColor: Color, modifier: Modifier = Modifier) {
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val onSurfaceVariant = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor),
        factory = { context ->
            val density = context.resources.displayMetrics.density
            fun dp(value: Int) = (value * density).toInt()

            val adView = NativeAdView(context)

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val headline = TextView(context).apply {
                setTypeface(typeface, Typeface.BOLD)
                textSize = 14f
                maxLines = 1
            }
            val body = TextView(context).apply {
                textSize = 12f
                maxLines = 2
                setTextColor(onSurfaceVariant.toArgb())
            }
            val textColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                    marginEnd = dp(10)
                }
                addView(headline)
                addView(body)
            }
            val cta = Button(context).apply {
                textSize = 11f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                isAllCaps = false
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(icon)
                addView(textColumn)
                addView(cta)
            }
            val outer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(row)
            }
            adView.addView(outer)
            adView.iconView = icon
            adView.headlineView = headline
            adView.bodyView = body
            adView.callToActionView = cta
            adView
        },
        update = { adView ->
            (adView.headlineView as TextView).text = nativeAd.headline
            (adView.bodyView as TextView).apply {
                text = nativeAd.body
                visibility = if (nativeAd.body == null) android.view.View.GONE else android.view.View.VISIBLE
            }
            (adView.callToActionView as Button).apply {
                text = nativeAd.callToAction
                visibility = if (nativeAd.callToAction == null) android.view.View.GONE else android.view.View.VISIBLE
                setBackgroundColor(tintColor.toArgb())
                setTextColor(Color.White.toArgb())
            }
            (adView.iconView as ImageView).apply {
                nativeAd.icon?.drawable?.let { setImageDrawable(it) }
                visibility = if (nativeAd.icon == null) android.view.View.GONE else android.view.View.VISIBLE
            }
            // Associated last, after every asset view is populated — required
            // for the SDK to register clicks/impressions correctly.
            adView.setNativeAd(nativeAd)
        },
    )
}
