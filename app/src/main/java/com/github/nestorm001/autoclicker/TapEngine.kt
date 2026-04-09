package com.github.nestorm001.autoclicker

import kotlin.math.sqrt
import kotlin.random.Random

object TapEngine {
    
    fun getRandomTapCount(min: Int, max: Int): Int {
        return Random.nextInt(min, max + 1)
    }
    
    fun getRandomDelay(min: Int, max: Int): Long {
        val mean = (min + max) / 2.0
        val stdDev = (max - min) / 6.0
        
        var delay: Double
        do {
            delay = gaussianRandom(mean, stdDev)
        } while (delay < min || delay > max)
        
        return delay.toLong()
    }
    
    fun applyJitter(x: Float, y: Float, enabled: Boolean): Pair<Float, Float> {
        if (!enabled) return Pair(x, y)
        
        val jitterX = Random.nextInt(-5, 6).toFloat()
        val jitterY = Random.nextInt(-5, 6).toFloat()
        
        return Pair(x + jitterX, y + jitterY)
    }
    
    fun getRandomTapDuration(enabled: Boolean): Long {
        if (!enabled) return 50L
        
        return Random.nextLong(50, 151)
    }
    
    fun applyDrift(x: Float, y: Float, tapIndex: Int, enabled: Boolean): Pair<Float, Float> {
        if (!enabled) return Pair(x, y)
        
        val driftAmount = (tapIndex / 10f)
        val angle = Random.nextDouble(0.0, 2 * Math.PI)
        
        val driftX = (driftAmount * Math.cos(angle)).toFloat()
        val driftY = (driftAmount * Math.sin(angle)).toFloat()
        
        return Pair(x + driftX, y + driftY)
    }
    
    private fun gaussianRandom(mean: Double, stdDev: Double): Double {
        val u1 = Random.nextDouble()
        val u2 = Random.nextDouble()
        val z0 = sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
        return z0 * stdDev + mean
    }
    
    fun getRandomDuration(min: Int, max: Int): Int {
        return Random.nextInt(min, max + 1)
    }
}