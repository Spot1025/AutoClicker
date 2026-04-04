package com.autoclicker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ClickConfig(
    val targets: List<Pair<Float, Float>>, // 4 tap coordinates
    val tapMin: Int,
    val tapMax: Int,
    val delayMin: Int, // milliseconds
    val delayMax: Int,
    val autoCycleRunMin: Int, // seconds
    val autoCycleRunMax: Int,
    val autoCyclePauseMin: Int,
    val autoCyclePauseMax: Int,
    val enableJitter: Boolean,
    val enablePressureVariance: Boolean,
    val enableDrift: Boolean
)

object ConfigManager {
    
    private const val PREFS_NAME = "autoclicker_config"
    private const val KEY_CONFIG = "config_json"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun save(context: Context, config: ClickConfig) {
        val json = JSONObject().apply {
            val targetsArray = JSONArray()
            config.targets.forEach { (x, y) ->
                targetsArray.put(JSONObject().apply {
                    put("x", x)
                    put("y", y)
                })
            }
            put("targets", targetsArray)
            put("tapMin", config.tapMin)
            put("tapMax", config.tapMax)
            put("delayMin", config.delayMin)
            put("delayMax", config.delayMax)
            put("autoCycleRunMin", config.autoCycleRunMin)
            put("autoCycleRunMax", config.autoCycleRunMax)
            put("autoCyclePauseMin", config.autoCyclePauseMin)
            put("autoCyclePauseMax", config.autoCyclePauseMax)
            put("enableJitter", config.enableJitter)
            put("enablePressureVariance", config.enablePressureVariance)
            put("enableDrift", config.enableDrift)
        }
        
        getPrefs(context).edit().putString(KEY_CONFIG, json.toString()).apply()
    }
    
    fun load(context: Context): ClickConfig {
        val jsonString = getPrefs(context).getString(KEY_CONFIG, null)
        
        return if (jsonString != null) {
            val json = JSONObject(jsonString)
            val targetsArray = json.getJSONArray("targets")
            val targets = mutableListOf<Pair<Float, Float>>()
            
            for (i in 0 until targetsArray.length()) {
                val target = targetsArray.getJSONObject(i)
                targets.add(Pair(
                    target.getDouble("x").toFloat(),
                    target.getDouble("y").toFloat()
                ))
            }
            
            ClickConfig(
                targets = targets,
                tapMin = json.getInt("tapMin"),
                tapMax = json.getInt("tapMax"),
                delayMin = json.getInt("delayMin"),
                delayMax = json.getInt("delayMax"),
                autoCycleRunMin = json.getInt("autoCycleRunMin"),
                autoCycleRunMax = json.getInt("autoCycleRunMax"),
                autoCyclePauseMin = json.getInt("autoCyclePauseMin"),
                autoCyclePauseMax = json.getInt("autoCyclePauseMax"),
                enableJitter = json.getBoolean("enableJitter"),
                enablePressureVariance = json.getBoolean("enablePressureVariance"),
                enableDrift = json.getBoolean("enableDrift")
            )
        } else {
            // Default config
            ClickConfig(
                targets = listOf(
                    Pair(270f, 585f),
                    Pair(810f, 585f),
                    Pair(270f, 1755f),
                    Pair(810f, 1755f)
                ),
                tapMin = 5,
                tapMax = 10,
                delayMin = 100,
                delayMax = 300,
                autoCycleRunMin = 30,
                autoCycleRunMax = 90,
                autoCyclePauseMin = 10,
                autoCyclePauseMax = 30,
                enableJitter = true,
                enablePressureVariance = true,
                enableDrift = false
            )
        }
    }
}