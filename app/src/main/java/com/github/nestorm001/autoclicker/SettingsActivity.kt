package com.github.nestorm001.autoclicker

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var config: ClickConfig
    
    // Target views
    private lateinit var target1: View
    private lateinit var target2: View
    private lateinit var target3: View
    private lateinit var target4: View
    
    // Input fields
    private lateinit var tapMinInput: EditText
    private lateinit var tapMaxInput: EditText
    private lateinit var delayMinInput: EditText
    private lateinit var delayMaxInput: EditText
    private lateinit var runMinInput: EditText
    private lateinit var runMaxInput: EditText
    private lateinit var pauseMinInput: EditText
    private lateinit var pauseMaxInput: EditText
    
    // Toggles
    private lateinit var jitterToggle: Switch
    private lateinit var pressureToggle: Switch
    private lateinit var driftToggle: Switch
    
    // Target coordinates
    private val targetCoords = mutableListOf(
        Pair(270f, 585f),
        Pair(810f, 585f),
        Pair(270f, 1755f),
        Pair(810f, 1755f)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        config = ConfigManager.load(this)
        
        initViews()
        loadConfig()
        setupTargetListeners()
    }
    
    private fun initViews() {
        target1 = findViewById(R.id.target1)
        target2 = findViewById(R.id.target2)
        target3 = findViewById(R.id.target3)
        target4 = findViewById(R.id.target4)
        
        tapMinInput = findViewById(R.id.tap_min_input)
        tapMaxInput = findViewById(R.id.tap_max_input)
        delayMinInput = findViewById(R.id.delay_min_input)
        delayMaxInput = findViewById(R.id.delay_max_input)
        runMinInput = findViewById(R.id.run_min_input)
        runMaxInput = findViewById(R.id.run_max_input)
        pauseMinInput = findViewById(R.id.pause_min_input)
        pauseMaxInput = findViewById(R.id.pause_max_input)
        
        jitterToggle = findViewById(R.id.jitter_toggle)
        pressureToggle = findViewById(R.id.pressure_toggle)
        driftToggle = findViewById(R.id.drift_toggle)
        
        findViewById<Button>(R.id.save_button).setOnClickListener { saveConfig() }
        findViewById<Button>(R.id.close_button).setOnClickListener { finish() }
    }
    
    private fun loadConfig() {
        targetCoords.clear()
        targetCoords.addAll(config.targets)
        
        // Position targets on screen
        target1.post { target1.x = config.targets[0].first; target1.y = config.targets[0].second }
        target2.post { target2.x = config.targets[1].first; target2.y = config.targets[1].second }
        target3.post { target3.x = config.targets[2].first; target3.y = config.targets[2].second }
        target4.post { target4.x = config.targets[3].first; target4.y = config.targets[3].second }
        
        tapMinInput.setText(config.tapMin.toString())
        tapMaxInput.setText(config.tapMax.toString())
        delayMinInput.setText(config.delayMin.toString())
        delayMaxInput.setText(config.delayMax.toString())
        runMinInput.setText(config.autoCycleRunMin.toString())
        runMaxInput.setText(config.autoCycleRunMax.toString())
        pauseMinInput.setText(config.autoCyclePauseMin.toString())
        pauseMaxInput.setText(config.autoCyclePauseMax.toString())
        
        jitterToggle.isChecked = config.enableJitter
        pressureToggle.isChecked = config.enablePressureVariance
        driftToggle.isChecked = config.enableDrift
    }
    
    private fun setupTargetListeners() {
        setupDraggable(target1, 0)
        setupDraggable(target2, 1)
        setupDraggable(target3, 2)
        setupDraggable(target4, 3)
    }
    
    private fun setupDraggable(view: View, index: Int) {
        var dX = 0f
        var dY = 0f
        
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    // Save final position
                    targetCoords[index] = Pair(v.x, v.y)
                    
                    // Show coordinate toast
                    Toast.makeText(
                        this,
                        "Target ${index + 1}: (${v.x.toInt()}, ${v.y.toInt()})",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun saveConfig() {
        try {
            val newConfig = ClickConfig(
                targets = targetCoords.toList(),
                tapMin = tapMinInput.text.toString().toIntOrNull() ?: 5,
                tapMax = tapMaxInput.text.toString().toIntOrNull() ?: 10,
                delayMin = delayMinInput.text.toString().toIntOrNull() ?: 100,
                delayMax = delayMaxInput.text.toString().toIntOrNull() ?: 300,
                autoCycleRunMin = runMinInput.text.toString().toIntOrNull() ?: 30,
                autoCycleRunMax = runMaxInput.text.toString().toIntOrNull() ?: 90,
                autoCyclePauseMin = pauseMinInput.text.toString().toIntOrNull() ?: 10,
                autoCyclePauseMax = pauseMaxInput.text.toString().toIntOrNull() ?: 30,
                enableJitter = jitterToggle.isChecked,
                enablePressureVariance = pressureToggle.isChecked,
                enableDrift = driftToggle.isChecked
            )
            
            ConfigManager.save(this, newConfig)
            
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}