package com.sunpawtechnologies.scrollless.tracking.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import kotlin.math.abs
import kotlin.math.roundToInt

data class CounterContent(val count: Int, val limit: Int, val status: LimitStatus)

/**
 * The small floating Reels counter shown over Instagram.
 *
 * It's a TYPE_ACCESSIBILITY_OVERLAY window owned by the accessibility service, so it needs no
 * "display over other apps" permission. The window wraps the pill tightly and is not
 * focusable, so every touch outside the pill goes straight to Instagram. Drag to move it;
 * long-press (or the accessibility action) hides it until Instagram is next opened.
 *
 * Plain Views instead of Compose: no lifecycle owner is needed outside an Activity, and it's
 * lighter for an always-on window. Main thread only.
 */
class ReelsCounterOverlay(
    private val service: AccessibilityService,
    private val listener: Listener,
) {
    interface Listener {
        fun onMoved(position: OverlayPosition)
        fun onHiddenForSession()
    }

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var pill: LinearLayout? = null
    private var icon: ImageView? = null
    private var label: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var position: OverlayPosition = DEFAULT_POSITION
    private var lastContent: CounterContent? = null

    val isShowing: Boolean get() = pill != null

    fun show(content: CounterContent, savedPosition: OverlayPosition?) {
        position = savedPosition ?: DEFAULT_POSITION
        if (pill == null) attach()
        if (content != lastContent) render(content)
        applyPosition()
    }

    fun hide() {
        val view = pill ?: return
        handler.removeCallbacksAndMessages(null)
        runCatching { windowManager.removeView(view) }
            .onFailure { TrackingLog.i("overlay_remove_failed", "error" to it.javaClass.simpleName) }
        pill = null
        icon = null
        label = null
        params = null
        lastContent = null
        TrackingLog.d("overlay_hidden")
    }

    /** Call on rotation or screen-size changes: re-applies the saved relative position. */
    fun onScreenChanged() {
        if (pill != null) applyPosition()
    }

    private fun attach() {
        val view = createPill()
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        try {
            windowManager.addView(view, layoutParams)
            pill = view
            params = layoutParams
            TrackingLog.d("overlay_shown")
        } catch (e: RuntimeException) {
            // BadTokenException etc.: the service is being torn down. Showing nothing is safe.
            TrackingLog.i("overlay_add_failed", "error" to e.javaClass.simpleName)
        }
    }

    private fun render(content: CounterContent) {
        val view = pill ?: return
        lastContent = content
        val (accent, iconRes) = when (content.status) {
            LimitStatus.WITHIN -> R.color.overlay_accent to R.drawable.ic_play_arrow
            LimitStatus.APPROACHING -> R.color.overlay_warning to R.drawable.ic_warning
            LimitStatus.REACHED -> R.color.overlay_critical to R.drawable.ic_error
        }
        val accentColor = ContextCompat.getColor(service, accent)
        (view.background as GradientDrawable).setStroke(dp(1.5f), accentColor)
        icon?.setImageResource(iconRes)
        icon?.imageTintList = ColorStateList.valueOf(accentColor)
        val text = when (content.status) {
            LimitStatus.REACHED -> service.getString(R.string.overlay_text_reached, content.count, content.limit)
            else -> service.getString(R.string.overlay_text, content.count, content.limit)
        }
        label?.text = text
        view.contentDescription = service.getString(R.string.overlay_content_description, content.count, content.limit)
        // The window wraps its content, so let it re-measure after the text changes.
        params?.let { runCatching { windowManager.updateViewLayout(view, it) } }
    }

    private fun applyPosition() {
        val view = pill ?: return
        val layoutParams = params ?: return
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val area = usableArea()
        val maxX = (area.width() - view.measuredWidth).coerceAtLeast(0)
        val maxY = (area.height() - view.measuredHeight).coerceAtLeast(0)
        val x = area.left + (position.xFraction * maxX).roundToInt()
        val y = area.top + (position.yFraction * maxY).roundToInt()
        if (layoutParams.x != x || layoutParams.y != y) {
            layoutParams.x = x
            layoutParams.y = y
            runCatching { windowManager.updateViewLayout(view, layoutParams) }
        }
    }

    /** The screen minus status/navigation bars, so the pill can't sit under system UI. */
    private fun usableArea(): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val bounds = metrics.bounds
            return Rect(insets.left, insets.top, bounds.width() - insets.right, bounds.height() - insets.bottom)
        }
        val display = service.resources.displayMetrics
        return Rect(0, dp(24f), display.widthPixels, display.heightPixels - dp(48f))
    }

    private fun toFraction(x: Int, y: Int): OverlayPosition {
        val view = pill ?: return position
        val area = usableArea()
        val maxX = (area.width() - view.width).coerceAtLeast(1)
        val maxY = (area.height() - view.height).coerceAtLeast(1)
        return OverlayPosition(
            xFraction = ((x - area.left).toFloat() / maxX).coerceIn(0f, 1f),
            yFraction = ((y - area.top).toFloat() / maxY).coerceIn(0f, 1f),
        )
    }

    @SuppressLint("ClickableViewAccessibility") // Drag/long-press have an accessibility action equivalent.
    private fun createPill(): LinearLayout {
        val iconView = ImageView(service).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16f), dp(16f)).apply { marginEnd = dp(6f) }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val textView = TextView(service).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(service, R.color.overlay_text))
            maxLines = 1
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        icon = iconView
        label = textView
        return LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(32f)
            setPadding(dp(10f), dp(6f), dp(12f), dp(6f))
            background = GradientDrawable().apply {
                cornerRadius = dp(16f).toFloat()
                setColor(ContextCompat.getColor(service, R.color.overlay_background))
            }
            elevation = dp(4f).toFloat()
            addView(iconView)
            addView(textView)
            setOnTouchListener(DragTouchListener())
            ViewCompat.addAccessibilityAction(this, service.getString(R.string.overlay_action_hide)) { _, _ ->
                hideForSession()
                true
            }
        }
    }

    private fun hideForSession() {
        TrackingLog.d("overlay_hidden_for_session")
        hide()
        listener.onHiddenForSession()
    }

    private inner class DragTouchListener : View.OnTouchListener {
        private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false
        private val longPress = Runnable {
            pill?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            hideForSession()
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val layoutParams = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = layoutParams.x
                    startY = layoutParams.y
                    dragging = false
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                        handler.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        val area = usableArea()
                        layoutParams.x = (startX + dx.roundToInt()).coerceIn(area.left, (area.right - view.width).coerceAtLeast(area.left))
                        layoutParams.y = (startY + dy.roundToInt()).coerceIn(area.top, (area.bottom - view.height).coerceAtLeast(area.top))
                        runCatching { windowManager.updateViewLayout(view, layoutParams) }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    if (dragging) {
                        position = toFraction(layoutParams.x, layoutParams.y)
                        TrackingLog.d("overlay_moved", "x" to position.xFraction, "y" to position.yFraction)
                        listener.onMoved(position)
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        view.performClick()
                    }
                    dragging = false
                }
            }
            return true
        }
    }

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, service.resources.displayMetrics).roundToInt()

    companion object {
        /** Top-left, below Instagram's top bar: clear of the Reels action buttons (right) and caption (bottom). */
        val DEFAULT_POSITION = OverlayPosition(xFraction = 0.04f, yFraction = 0.1f)
    }
}
