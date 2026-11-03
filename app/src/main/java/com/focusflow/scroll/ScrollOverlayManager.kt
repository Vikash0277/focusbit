package com.focusflow.scroll

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Manages the persistent, ultra-minimalist floating scroll counter.
 * Displays exclusively the live scroll count number (e.g. "42").
 * Positioned by default at the TOP-MOST MIDDLE of the screen.
 * Stays on screen persistently without disappearing.
 * Supports custom background colors and fluid touch dragging.
 */
class ScrollOverlayManager(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: LinearLayout? = null
    private var pillTextView: TextView? = null
    private var isAdded = false

    private var lastCount: Int = 0
    private var lastThreshold: Int = 50

    private val prefs = service.getSharedPreferences("focusflow_overlay_pos", Context.MODE_PRIVATE)

    private val hideRunnable = Runnable {
        val view = overlayView ?: return@Runnable
        val duration = ScrollTrackerManager.getOverlayDisplayDuration()
        if (duration <= 0) return@Runnable

        view.animate()
            .setListener(null)
            .cancel()
        view.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    private fun scheduleAutoHideIfConfigured() {
        mainHandler.removeCallbacks(hideRunnable)
        val duration = ScrollTrackerManager.getOverlayDisplayDuration()
        if (duration > 0) {
            mainHandler.postDelayed(hideRunnable, duration * 1000L)
        }
    }

    init {
        // Register callbacks with ScrollTrackerManager for real-time live updates
        ScrollTrackerManager.onResetPosition = { resetPositionToTopMiddle() }
        ScrollTrackerManager.onRefreshTheme = { refreshThemeColor() }
        ScrollTrackerManager.onDurationChanged = { seconds -> handleDurationChanged(seconds) }

        mainHandler.post {
            createOverlay()
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            service.resources.displayMetrics
        ).toInt()
    }

    private fun createOverlay() {
        if (overlayView != null) return

        val isEnabled = ScrollTrackerManager.isOverlayEnabled()

        val container = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(14f), dpToPx(5f), dpToPx(14f), dpToPx(5f))
            minimumWidth = dpToPx(48f)
            minimumHeight = dpToPx(32f)
            visibility = if (isEnabled) View.VISIBLE else View.GONE
            alpha = if (isEnabled) 1f else 0f
            elevation = dpToPx(8f).toFloat()
        }

        val textView = TextView(service).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.02f
            text = "$lastCount"
        }

        container.addView(textView)

        // Apply chosen color theme styling immediately
        applyStyling(container, lastCount, lastThreshold)

        // Default position: TOP-MOST MIDDLE (horizontal center, just below status bar)
        val savedXOffset = prefs.getInt("overlay_x_offset", 0)
        val savedY = prefs.getInt("overlay_y", dpToPx(38f))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = savedXOffset
            y = savedY
        }

        // Enable smooth dragging while preserving top-middle anchor
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false

        container.setOnTouchListener { view, event ->
            val layoutParams = view.layoutParams as? WindowManager.LayoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Do not auto-hide while user is actively touching or dragging
                    mainHandler.removeCallbacks(hideRunnable)
                    view.animate().setListener(null).cancel()
                    view.alpha = 1f
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    mainHandler.removeCallbacks(hideRunnable)
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 4 || abs(dy) > 4) {
                        isMoved = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(view, layoutParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    if (isMoved) {
                        prefs.edit()
                            .putInt("overlay_x_offset", layoutParams.x)
                            .putInt("overlay_y", layoutParams.y)
                            .apply()
                    }
                    // Restart auto-hide timer once touch is released
                    scheduleAutoHideIfConfigured()
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(container, params)
            overlayView = container
            pillTextView = textView
            isAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates the persistent badge with ONLY the scroll count number.
     * Smoothly fades in if hidden, cancels existing hide timers, and schedules auto-hide.
     */
    fun updateScroll(count: Int, threshold: Int) {
        lastCount = count
        lastThreshold = threshold

        if (!ScrollTrackerManager.isOverlayEnabled()) {
            hideImmediate()
            return
        }

        mainHandler.post {
            mainHandler.removeCallbacks(hideRunnable)

            if (overlayView == null) {
                createOverlay()
            }

            val view = overlayView ?: return@post
            val tv = pillTextView ?: return@post

            applyStyling(view, count, threshold)
            tv.text = "$count"

            view.animate().setListener(null).cancel()
            if (view.visibility != View.VISIBLE || view.alpha < 1f) {
                view.visibility = View.VISIBLE
                view.animate().alpha(1f).setDuration(150).start()
            }

            scheduleAutoHideIfConfigured()
        }
    }

    /**
     * Shows the badge when entering an app and schedules auto-hide if configured.
     */
    fun showPersistent(count: Int, threshold: Int) {
        lastCount = count
        lastThreshold = threshold

        if (!ScrollTrackerManager.isOverlayEnabled()) {
            hideImmediate()
            return
        }

        mainHandler.post {
            mainHandler.removeCallbacks(hideRunnable)

            if (overlayView == null) {
                createOverlay()
            }

            val view = overlayView ?: return@post
            val tv = pillTextView ?: return@post

            applyStyling(view, count, threshold)
            tv.text = "$count"

            view.animate().setListener(null).cancel()
            if (view.visibility != View.VISIBLE || view.alpha < 1f) {
                view.visibility = View.VISIBLE
                view.animate().alpha(1f).setDuration(150).start()
            }

            scheduleAutoHideIfConfigured()
        }
    }

    /**
     * Responds immediately when user changes the Display Duration in settings.
     */
    fun handleDurationChanged(seconds: Int) {
        mainHandler.post {
            mainHandler.removeCallbacks(hideRunnable)
            val view = overlayView ?: return@post
            if (view.visibility == View.VISIBLE) {
                if (seconds == 0) {
                    view.animate().setListener(null).cancel()
                    view.alpha = 1f
                } else {
                    mainHandler.postDelayed(hideRunnable, seconds * 1000L)
                }
            }
        }
    }

    /**
     * Refreshes the color styling immediately when user changes the color in settings.
     * Does NOT force the view visible if it was hidden (e.g. on home screen).
     */
    fun refreshThemeColor(count: Int = lastCount, threshold: Int = lastThreshold) {
        lastCount = count
        lastThreshold = threshold

        mainHandler.post {
            val view = overlayView ?: return@post
            val tv = pillTextView ?: return@post

            applyStyling(view, count, threshold)
            tv.text = "$count"
        }
    }

    /**
     * Resets position strictly back to top-most middle (center, y = 38dp).
     */
    fun resetPositionToTopMiddle() {
        mainHandler.post {
            val defaultY = dpToPx(38f)
            prefs.edit().putInt("overlay_x_offset", 0).putInt("overlay_y", defaultY).apply()
            val view = overlayView ?: return@post
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return@post
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = 0
            params.y = defaultY
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyStyling(view: View, count: Int, threshold: Int) {
        val isOverThreshold = threshold > 0 && count >= threshold
        val isNearThreshold = threshold > 0 && count >= (threshold * 0.8f)
        val theme = ScrollTrackerManager.getOverlayTheme()

        val backgroundDrawable = GradientDrawable().apply {
            cornerRadius = dpToPx(50f).toFloat()

            val baseBg = try {
                Color.parseColor(theme.bgHex)
            } catch (e: Exception) {
                Color.parseColor("#EE12111E")
            }

            val baseBorder = try {
                Color.parseColor(theme.borderHex)
            } catch (e: Exception) {
                Color.parseColor("#8B5CF6")
            }

            // Always use the user's chosen background color
            setColor(baseBg)

            when {
                isOverThreshold -> {
                    // Alert Crimson Red border on doomscroll
                    setStroke(dpToPx(2.2f), Color.parseColor("#EF4444"))
                }
                isNearThreshold -> {
                    // Warning Warm Amber border
                    setStroke(dpToPx(1.8f), Color.parseColor("#F59E0B"))
                }
                else -> {
                    // Crisp theme border
                    setStroke(dpToPx(1.5f), baseBorder)
                }
            }
        }

        view.background = backgroundDrawable
    }

    /**
     * Hides the overlay immediately when returning to launcher or disabled.
     */
    fun hideImmediate() {
        mainHandler.removeCallbacks(hideRunnable)
        mainHandler.post {
            overlayView?.animate()?.cancel()
            overlayView?.visibility = View.GONE
            overlayView?.alpha = 0f
        }
    }

    fun destroy() {
        mainHandler.removeCallbacks(hideRunnable)
        ScrollTrackerManager.onResetPosition = null
        ScrollTrackerManager.onRefreshTheme = null
        ScrollTrackerManager.onDurationChanged = null

        if (isAdded && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            pillTextView = null
            isAdded = false
        }
    }
}
