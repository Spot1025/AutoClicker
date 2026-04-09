package com.github.nestorm001.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AutoClickAccessibilityService : AccessibilityService() {
    
    private var isRunning = false
    private var isAutoCycle = false
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for our use case
    }
    
    override fun onInterrupt() {
        stopTapping()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                FloatingBubbleService.ACTION_START_MANUAL -> startManualMode()
                FloatingBubbleService.ACTION_START_AUTO -> startAutoCycleMode()
                FloatingBubbleService.ACTION_STOP -> stopTapping()
            }
        }
        return START_STICKY
    }
    
    private fun startManualMode() {
        if (isRunning) return
        
        isRunning = true
        isAutoCycle = false
        
        currentJob = scope.launch {
            runTapSequence()
            isRunning = false
            updateBubbleStatus(stopped = true)
        }
    }
    
    private fun startAutoCycleMode() {
        if (isRunning) return
        
        isRunning = true
        isAutoCycle = true
        
        currentJob = scope.launch {
            while (isAutoCycle) {
                val config = ConfigManager.load(this@AutoClickAccessibilityService)
                
                // Run phase
                val runDuration = TapEngine.getRandomDuration(
                    config.autoCycleRunMin,
                    config.autoCycleRunMax
                )
                
                val runJob = launch {
                    val endTime = System.currentTimeMillis() + (runDuration * 1000)
                    while (System.currentTimeMillis() < endTime && isAutoCycle) {
                        runTapSequence()
                        delay(500) // Brief pause between sequences
                    }
                }
                
                runJob.join()
                
                if (!isAutoCycle) break
                
                // Pause phase
                val pauseDuration = TapEngine.getRandomDuration(
                    config.autoCyclePauseMin,
                    config.autoCyclePauseMax
                )
                
                for (i in pauseDuration downTo 1) {
                    if (!isAutoCycle) break
                    updateBubbleStatus(timeRemaining = i)
                    delay(1000)
                }
            }
            
            isRunning = false
            updateBubbleStatus(stopped = true)
        }
    }
    
    private suspend fun runTapSequence() {
        val config = ConfigManager.load(this)
        val tapCount = TapEngine.getRandomTapCount(config.tapMin, config.tapMax)
        
        for (i in 0 until tapCount) {
            if (!isRunning) break
            
            val targetIndex = i % 4
            val (baseX, baseY) = config.targets[targetIndex]
            
            // Apply anti-detection features
            var (x, y) = TapEngine.applyJitter(baseX, baseY, config.enableJitter)
            val (driftX, driftY) = TapEngine.applyDrift(x, y, i, config.enableDrift)
            x = driftX
            y = driftY
            
            val duration = TapEngine.getRandomTapDuration(config.enablePressureVariance)
            
            // Perform tap
            performTap(x, y, duration)
            
            // Update bubble status
            updateBubbleStatus(tapCount = i + 1, totalTaps = tapCount)
            
            // Human-like delay
            val delay = TapEngine.getRandomDelay(config.delayMin, config.delayMax)
            delay(delay)
        }
    }
    
    private fun performTap(x: Float, y: Float, duration: Long) {
        val path = Path()
        path.moveTo(x, y)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(path, 0, duration)
        )
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }
    
    private fun updateBubbleStatus(
        tapCount: Int = 0,
        totalTaps: Int = 0,
        timeRemaining: Int = 0,
        stopped: Boolean = false
    ) {
        val intent = Intent(this, FloatingBubbleService::class.java)
        
        if (stopped) {
            intent.action = FloatingBubbleService.ACTION_STOP
        } else {
            intent.action = FloatingBubbleService.ACTION_UPDATE_STATUS
            intent.putExtra(FloatingBubbleService.EXTRA_TAP_COUNT, tapCount)
            intent.putExtra(FloatingBubbleService.EXTRA_TOTAL_TAPS, totalTaps)
            intent.putExtra(FloatingBubbleService.EXTRA_TIME_REMAINING, timeRemaining)
        }
        
        startService(intent)
    }
    
    private fun stopTapping() {
        isRunning = false
        isAutoCycle = false
        currentJob?.cancel()
        currentJob = null
        updateBubbleStatus(stopped = true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTapping()
        scope.cancel()
    }
}