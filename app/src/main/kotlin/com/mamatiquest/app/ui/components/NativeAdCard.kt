package com.mamatiquest.app.ui.components

import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT as MATCH
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT as WRAP
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.mamatiquest.app.services.AdsManager
import com.mamatiquest.app.ui.theme.WordQuestPurple

/**
 * A native ad drawn as one of the app's own cards — same shape, spacing and
 * colors as Home's "Suggested" card — with a clear "Ad" label (required by
 * AdMob policy). Takes no space until an ad has actually loaded.
 * iOS counterpart: `NativeAdCard` in `Services/Ads/NativeAdCard.swift`.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ready by AdsManager.isReady.collectAsState()
    if (!ready) return

    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var disposed = false
        AdsManager.loadNative(context) { loaded -> if (disposed) loaded.destroy() else ad = loaded }
        onDispose {
            disposed = true
            ad?.destroy()
        }
    }
    val loaded = ad ?: return

    val scheme = MaterialTheme.colorScheme
    val colors = CardColors(
        background = scheme.surfaceVariant.copy(alpha = 0.5f).compositeOver(scheme.background).toArgb(),
        title = scheme.onSurface.toArgb(),
        body = scheme.onSurfaceVariant.toArgb(),
        badgeBackground = scheme.surface.copy(alpha = 0.7f).toArgb(),
        accent = WordQuestPurple.toArgb(),
        onAccent = Color.White.toArgb(),
    )

    AndroidView(
        factory = { ctx -> NativeCard(ctx, colors).root },
        update = { (it.tag as NativeCard).bind(loaded) },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

private data class CardColors(
    val background: Int,
    val title: Int,
    val body: Int,
    val badgeBackground: Int,
    val accent: Int,
    val onAccent: Int,
)

/**
 * Plain Views rather than Compose: AdMob tracks impressions and clicks through
 * the asset Views registered on [NativeAdView], so each asset has to be a real View.
 */
private class NativeCard(context: Context, colors: CardColors) {
    val root = NativeAdView(context).also { it.tag = this }
    private val resources = context.resources
    private val icon = ImageView(context)
    private val headline = TextView(context)
    private val body = TextView(context)
    private val cta = TextView(context)

    init {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(colors.background, dp(18).toFloat())
        }

        icon.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) =
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12).toFloat())
            }
        }
        content.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(14) })

        val badge = TextView(context).apply {
            text = "Ad"
            setTextColor(colors.body)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(1), dp(6), dp(1))
            background = rounded(colors.badgeBackground, dp(50).toFloat())
        }
        headline.apply {
            setTextColor(colors.title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(badge, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(6) })
            addView(headline, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        body.apply {
            setTextColor(colors.body)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleRow)
            addView(body)
        }
        content.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(10) })

        cta.apply {
            setTextColor(colors.onAccent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            setPadding(dp(14), dp(7), dp(14), dp(7))
            background = rounded(colors.accent, dp(50).toFloat())
        }
        content.addView(cta)

        root.addView(content, FrameLayout.LayoutParams(MATCH, WRAP))

        root.iconView = icon
        root.headlineView = headline
        root.bodyView = body
        root.callToActionView = cta
    }

    fun bind(ad: NativeAd) {
        headline.text = ad.headline
        body.text = ad.body
        body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        cta.text = ad.callToAction
        cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
        val drawable = ad.icon?.drawable
        icon.setImageDrawable(drawable)
        icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
        root.setNativeAd(ad)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }
}
