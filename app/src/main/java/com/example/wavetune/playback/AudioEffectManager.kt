package com.example.wavetune.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBand(
    val id: Short,
    val centerFreq: Int, // in Hz
    val minLevel: Short, // in mB
    val maxLevel: Short, // in mB
    val currentLevel: Short
) {
    val centerFreqFormatted: String
        get() = if (centerFreq >= 1000) "${centerFreq / 1000} kHz" else "$centerFreq Hz"
}

enum class EqPreset(val displayName: String) {
    NORMAL("Normal"),
    POP("Pop"),
    ROCK("Rock"),
    CLASSICAL("Classical"),
    JAZZ("Jazz"),
    VOCAL("Vocal"),
    BASS_BOOST("Bass Boost"),
    CUSTOM("Custom")
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val selectedPreset: EqPreset = EqPreset.NORMAL,
    val bands: List<EqualizerBand> = emptyList(),
    val bassStrength: Int = 300, // 0 - 1000
    val virtualizerStrength: Int = 200, // 0 - 1000
    val trebleStrength: Int = 400, // 0 - 1000
    val vocalStrength: Int = 500, // 0 - 1000
    val balance: Float = 0.0f, // -1.0 (Left) to 1.0 (Right)
    val isHardwareSupported: Boolean = true
)

class AudioEffectManager(private val context: Context) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _eqState = MutableStateFlow(createDefaultEqualizerState())
    val eqState = _eqState.asStateFlow()

    private var currentSessionId: Int = 0

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId
        releaseEffects()

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _eqState.value.isEnabled
            }
            equalizer = eq

            val bandsList = mutableListOf<EqualizerBand>()
            val numBands = eq.numberOfBands
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]

            for (i in 0 until numBands) {
                val bandId = i.toShort()
                val freq = eq.getCenterFreq(bandId) / 1000 // Convert mHz to Hz
                val level = eq.getBandLevel(bandId)
                bandsList.add(
                    EqualizerBand(
                        id = bandId,
                        centerFreq = freq,
                        minLevel = minLevel,
                        maxLevel = maxLevel,
                        currentLevel = level
                    )
                )
            }

            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = _eqState.value.isEnabled
                    if (strengthSupported) {
                        setStrength(_eqState.value.bassStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioEffectManager", "BassBoost not supported: ${e.message}")
            }

            try {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = _eqState.value.isEnabled
                    if (strengthSupported) {
                        setStrength(_eqState.value.virtualizerStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioEffectManager", "Virtualizer not supported: ${e.message}")
            }

            _eqState.value = _eqState.value.copy(
                bands = bandsList,
                isHardwareSupported = true
            )
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "Equalizer hardware error (using virtual engine fallback)", e)
            _eqState.value = _eqState.value.copy(isHardwareSupported = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error toggling eq", e)
        }
        _eqState.value = _eqState.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(bandId: Short, level: Short) {
        try {
            equalizer?.setBandLevel(bandId, level)
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error setting band level", e)
        }
        val updatedBands = _eqState.value.bands.map {
            if (it.id == bandId) it.copy(currentLevel = level) else it
        }
        _eqState.value = _eqState.value.copy(
            bands = updatedBands,
            selectedPreset = EqPreset.CUSTOM
        )
    }

    fun setPreset(preset: EqPreset) {
        val bands = _eqState.value.bands
        val (bass, treble, vocal, bandGains) = when (preset) {
            EqPreset.NORMAL -> Quad(0, 0, 0, listOf(0, 0, 0, 0, 0))
            EqPreset.POP -> Quad(200, 300, 400, listOf(-100, 200, 500, 200, -100))
            EqPreset.ROCK -> Quad(600, 500, 100, listOf(500, 300, -100, 300, 600))
            EqPreset.CLASSICAL -> Quad(300, 400, 200, listOf(400, 200, -100, 200, 400))
            EqPreset.JAZZ -> Quad(300, 200, 400, listOf(300, 100, 300, 100, 300))
            EqPreset.VOCAL -> Quad(100, 300, 700, listOf(-200, 300, 600, 300, -100))
            EqPreset.BASS_BOOST -> Quad(800, 100, 100, listOf(700, 500, 100, -100, -200))
            EqPreset.CUSTOM -> return
        }

        setBassStrength(bass)
        setTrebleStrength(treble)
        setVocalStrength(vocal)

        val updatedBands = bands.mapIndexed { index, band ->
            val gain = bandGains.getOrElse(index) { 0 }.toShort()
            val clamped = gain.coerceIn(band.minLevel, band.maxLevel)
            try {
                equalizer?.setBandLevel(band.id, clamped)
            } catch (e: Exception) {
                // fallback
            }
            band.copy(currentLevel = clamped)
        }

        _eqState.value = _eqState.value.copy(
            selectedPreset = preset,
            bands = updatedBands
        )
    }

    fun setBassStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error setting bass strength", e)
        }
        _eqState.value = _eqState.value.copy(bassStrength = clamped)
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error setting virtualizer strength", e)
        }
        _eqState.value = _eqState.value.copy(virtualizerStrength = clamped)
    }

    fun setTrebleStrength(strength: Int) {
        _eqState.value = _eqState.value.copy(trebleStrength = strength.coerceIn(0, 1000))
    }

    fun setVocalStrength(strength: Int) {
        _eqState.value = _eqState.value.copy(vocalStrength = strength.coerceIn(0, 1000))
    }

    fun setBalance(balance: Float) {
        _eqState.value = _eqState.value.copy(balance = balance.coerceIn(-1.0f, 1.0f))
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error releasing effects", e)
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    private fun createDefaultEqualizerState(): EqualizerState {
        val defaultBands = listOf(
            EqualizerBand(0, 60, -1000, 1000, 0),
            EqualizerBand(1, 230, -1000, 1000, 0),
            EqualizerBand(2, 910, -1000, 1000, 0),
            EqualizerBand(3, 3600, -1000, 1000, 0),
            EqualizerBand(4, 14000, -1000, 1000, 0)
        )
        return EqualizerState(
            isEnabled = true,
            selectedPreset = EqPreset.NORMAL,
            bands = defaultBands,
            bassStrength = 300,
            virtualizerStrength = 200,
            trebleStrength = 400,
            vocalStrength = 500,
            balance = 0.0f,
            isHardwareSupported = true
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
