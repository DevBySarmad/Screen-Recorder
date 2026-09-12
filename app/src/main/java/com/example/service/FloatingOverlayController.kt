package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class FloatingOverlayController(
  private val context: Context,
  private val onPauseResumeClicked: () -> Unit,
  private val onStopClicked: () -> Unit
) {
  private val windowManager: WindowManager =
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private var overlayView: View? = null
  private var timerTextView: TextView? = null
  private var pauseResumeImageView: ImageView? = null
  private var isPaused: Boolean = false

  fun show() {
    if (!Settings.canDrawOverlays(context)) return
    if (overlayView != null) return

    val paramsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      paramsType,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = 50
      y = 150
    }

    val dp = { value: Float ->
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()
    }

    // Root layout: rounded floating pill
    val root = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      setPadding(dp(12f), dp(8f), dp(12f), dp(8f))

      val bg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(24f).toFloat()
        setColor(Color.parseColor("#EE0F172A")) // Slate 900 with slight opacity
        setStroke(dp(1.5f), Color.parseColor("#33FFFFFF"))
      }
      background = bg
      elevation = dp(8f).toFloat()
    }

    // Pulsing/Red dot indicator
    val redDot = View(context).apply {
      val dotBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#EF4444")) // Vibrant red
      }
      background = dotBg
      layoutParams = LinearLayout.LayoutParams(dp(10f), dp(10f)).apply {
        rightMargin = dp(8f)
      }
    }
    root.addView(redDot)

    // Timer text
    val timerText = TextView(context).apply {
      text = "00:00"
      setTextColor(Color.WHITE)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      paint.isFakeBoldText = true
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        rightMargin = dp(12f)
      }
    }
    timerTextView = timerText
    root.addView(timerText)

    // Pause/Resume button
    val pauseBtn = ImageView(context).apply {
      setImageResource(android.R.drawable.ic_media_pause)
      setColorFilter(Color.WHITE)
      setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
      layoutParams = LinearLayout.LayoutParams(dp(32f), dp(32f)).apply {
        rightMargin = dp(6f)
      }
      setOnClickListener {
        onPauseResumeClicked()
      }
    }
    pauseResumeImageView = pauseBtn
    root.addView(pauseBtn)

    // Stop button
    val stopBtn = ImageView(context).apply {
      // Create a small stop square inside a red circle
      val stopBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#DC2626"))
      }
      background = stopBg
      setImageResource(android.R.drawable.ic_notification_clear_all)
      setColorFilter(Color.WHITE)
      setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
      layoutParams = LinearLayout.LayoutParams(dp(32f), dp(32f))
      setOnClickListener {
        onStopClicked()
      }
    }
    root.addView(stopBtn)

    // Drag-and-drop touch listener
    var initialX = 0
    var initialY = 0
    var initialTouchX = 0f
    var initialTouchY = 0f
    var isMoving = false

    root.setOnTouchListener { view, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          initialX = params.x
          initialY = params.y
          initialTouchX = event.rawX
          initialTouchY = event.rawY
          isMoving = false
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = (event.rawX - initialTouchX).toInt()
          val dy = (event.rawY - initialTouchY).toInt()
          if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
            isMoving = true
          }
          params.x = initialX + dx
          params.y = initialY + dy
          try {
            windowManager.updateViewLayout(root, params)
          } catch (_: Exception) {}
          true
        }
        MotionEvent.ACTION_UP -> {
          if (!isMoving) {
            view.performClick()
          }
          true
        }
        else -> false
      }
    }

    try {
      windowManager.addView(root, params)
      overlayView = root
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun updateTimer(seconds: Long) {
    val mins = seconds / 60
    val secs = seconds % 60
    val formatted = String.format("%02d:%02d", mins, secs)
    timerTextView?.post {
      timerTextView?.text = formatted
    }
  }

  fun updatePauseState(paused: Boolean) {
    isPaused = paused
    pauseResumeImageView?.post {
      if (paused) {
        pauseResumeImageView?.setImageResource(android.R.drawable.ic_media_play)
      } else {
        pauseResumeImageView?.setImageResource(android.R.drawable.ic_media_pause)
      }
    }
  }

  fun hide() {
    overlayView?.let { view ->
      try {
        windowManager.removeView(view)
      } catch (_: Exception) {}
      overlayView = null
      timerTextView = null
      pauseResumeImageView = null
    }
  }
}
