package com.julieasoreng.touchgrass.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.julieasoreng.touchgrass.ui.theme.InterventionRed
import com.julieasoreng.touchgrass.ui.theme.LockScreenBlack
import com.julieasoreng.touchgrass.ui.theme.LockScreenTextMuted

private const val TAG = "OVERLAY_TRIGGER"

/**
 * Owns the single WindowManager overlay window for the "avbryt mens brukeren scroller" test arm.
 * Uses a plain [View] hierarchy, not Compose — a ComposeView added directly via
 * [WindowManager.addView] from a Service (no Activity) needs a hand-rolled LifecycleOwner /
 * ViewModelStoreOwner / SavedStateRegistryOwner wired onto it to avoid crashing on first
 * composition, which is extra failure surface this test artifact doesn't need.
 *
 * OEM note: some OEM skins (Samsung/MIUI in particular) restrict or delay TYPE_APPLICATION_OVERLAY
 * windows from apps they've flagged as background/battery-restricted, independent of whether the
 * "draw over other apps" permission is granted — if the overlay is intermittently late or silently
 * doesn't appear on a specific device, check that OEM's battery/autostart settings for this app
 * before assuming the trigger logic itself is wrong.
 */
class InterventionOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun isShowing(): Boolean = overlayView != null

    /** Must be called on the main thread — [WindowManager.addView] requires a Looper thread. */
    fun show(reasonText: String, onDismiss: () -> Unit) {
        if (overlayView != null) {
            Log.d(TAG, "show: already showing, ignoring duplicate call")
            return
        }
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "show: BLOCKED — \"draw over other apps\" permission not granted")
            return
        }

        val view = buildOverlayView(reasonText) {
            Log.i(TAG, "show: user tapped Dismiss at ${System.currentTimeMillis()}")
            hide(reason = "dismissed_by_user")
            onDismiss()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Deliberately NOT FLAG_NOT_TOUCH_MODAL: that flag only matters for a window smaller
            // than the screen (it lets touches outside its bounds fall through to what's behind).
            // This overlay is MATCH_PARENT full-screen, so there is no "outside" region — omitting
            // the flag means it consumes all touches within itself, which is what actually blocks
            // continued scrolling in the app underneath until the user hits Dismiss.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Draws under the notch/cutout too — social apps often run edge-to-edge in
                // immersive mode, and without this the overlay can leave a gap there. Coverage of
                // fullscreen/immersive content is known to vary by target app and Android version;
                // verify directly against the app used in the test, not assumed from this flag alone.
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager.addView(view, params)
        overlayView = view
        Log.i(TAG, "show: addView() succeeded at ${System.currentTimeMillis()} (reason=$reasonText)")
    }

    /** Distinct log reason from a deliberate Dismiss tap — this is the "found a way around it"
     *  path: the monitored app left foreground (backgrounded, switched away, or force-closed)
     *  while the overlay was still up, without the user ever tapping Dismiss. */
    fun hideBecauseUserLeftMonitoredApp() = hide(reason = "left_app_or_force_closed")

    /** Must be called on the main thread — [WindowManager.removeView] requires a Looper thread. */
    private fun hide(reason: String) {
        val view = overlayView ?: return
        windowManager.removeView(view)
        overlayView = null
        Log.i(TAG, "hide: overlay removed at ${System.currentTimeMillis()} (reason=$reason)")
    }

    private fun buildOverlayView(reasonText: String, onDismiss: () -> Unit): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(LockScreenBlack.toArgb())
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val title = TextView(context).apply {
            text = "Put the phone down."
            setTextColor(InterventionRed.toArgb())
            textSize = 26f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(64, 0, 64, 24)
        }
        val subtitle = TextView(context).apply {
            text = reasonText
            setTextColor(LockScreenTextMuted.toArgb())
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(64, 0, 64, 48)
        }
        val dismissButton = TextView(context).apply {
            text = "Dismiss"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(48, 20, 48, 20)
            isClickable = true
            isFocusable = true
            setOnClickListener { onDismiss() }
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(dismissButton)
        root.addView(
            content,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        return root
    }
}
