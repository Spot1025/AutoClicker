package com.github.nestorm001.autoclicker

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.abs

class FloatingBubbleService : Service() {
    
    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var bubbleIcon: ImageView? = null
    private var bubbleText: TextView? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private var lastTapTime = 0L
    private var tapCount = 0
    private val doubleTapThreshold = 300L
    
    private var isRunning = false
    private var isAutoCycle = false
    
    companion object {
        const val ACTION_START_MANUAL = "START_MANUAL"
        const val ACTION_START_AUTO = "START_AUTO"
        const val ACTION_STOP = "STOP"
        const val ACTION_UPDATE_STATUS = "UPDATE_STATUS"
        const val EXTRA_TAP_COUNT = "tap_count"
        const val EXTRA_TOTAL_TAPS = "total_taps"
        const val EXTRA_TIME_REMAINING = "time_remaining"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = inflater.inflate(R.layout.floating_bubble, null)
        
        bubbleIcon = bubbleView?.findViewById(R.id.bubble_icon)
        bubbleText = bubbleView?.findViewById(R.id.bubble_text)
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        windowManager?.addView(bubbleView, params)
        
        setupTouchListener(params)
        updateBubbleState()
    }
    
    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        var longPressHandler: Runnable? = null
        
        bubbleView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    
                    longPressHandler = Runnable {
                        openSettings()
                    }
                    v.postDelayed(longPressHandler, 500)
                    
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    
                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        longPressHandler?.let { v.removeCallbacks(it) }
                        tapCount = 0
                    }
                    
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    
                    windowManager?.updateViewLayout(bubbleView, params)
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    longPressHandler?.let { v.removeCallbacks(it) }
                    
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    
                    if (abs(deltaX) < 10 && abs(deltaY) < 10) {
                        handleTap()
                    }
                    
                    snapToEdge(params)
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun handleTap() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastTapTime < doubleTapThreshold) {
            toggleAutoCycle()
            tapCount = 0
        } else {
            tapCount = 1
            bubbleView?.postDelayed({
                if (tapCount == 1) {
                    toggleManual()
                }
                tapCount = 0
            }, doubleTapThreshold)
        }
        
        lastTapTime = currentTime
    }
    
    private fun toggleManual() {
        if (isAutoCycle) return
        
        isRunning = !isRunning
        
        val intent = Intent(this, AutoClickAccessibilityService::class.java)
        intent.action = if (isRunning) ACTION_START_MANUAL else ACTION_STOP
        startService(intent)
        
        updateBubbleState()
    }
    
    private fun toggleAutoCycle() {
        isAutoCycle = !isAutoCycle
        isRunning = isAutoCycle
        
        val intent = Intent(this, AutoClickAccessibilityService::class.java)
        intent.action = if (isAutoCycle) ACTION_START_AUTO else ACTION_STOP
        startService(intent)
        
        updateBubbleState()
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
    
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        params.x = if (params.x < screenWidth / 2) 0 else screenWidth
        windowManager?.updateViewLayout(bubbleView, params)
    }
    
    private fun updateBubbleState() {
        when {
            isAutoCycle -> {
                bubbleView?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                bubbleText?.text = "AUTO"
            }
            isRunning -> {
                bubbleView?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                bubbleText?.text = "RUN"
            }
            else -> {
                bubbleView?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                bubbleText?.text = "⊕"
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_UPDATE_STATUS -> {
                    val tapCount = it.getIntExtra(EXTRA_TAP_COUNT, 0)
                    val totalTaps = it.getIntExtra(EXTRA_TOTAL_TAPS, 0)
                    val timeRemaining = it.getIntExtra(EXTRA_TIME_REMAINING, 0)
                    
                    if (isAutoCycle && timeRemaining > 0) {
                        bubbleText?.text = "${timeRemaining}s"
                    } else if (isRunning) {
                        bubbleText?.text = "$tapCount/$totalTaps"
                    }
                }
                ACTION_STOP -> {
                    isRunning = false
                    isAutoCycle = false
                    updateBubbleState()
                }
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager?.removeView(it) }
    }
private fun createBubbleView(): View {
    val layout = android.widget.RelativeLayout(this).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            200, // 80dp in pixels
            200
        )
        setBackgroundColor(android.graphics.Color.parseColor("#808080"))
        setPadding(20, 20, 20, 20)
        elevation = 8f
    }
    
    val textView = TextView(this).apply {
        id = R.id.bubble_text
        text = "⊕"
        textSize = 24f
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
    }
    
    val imageView = ImageView(this).apply {
        id = R.id.bubble_icon
        visibility = View.GONE
    }
    
    layout.addView(textView)
    layout.addView(imageView)
    
    return layout
}
}