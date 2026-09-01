package com.example.wavetune.ui.screens.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wavetune.playback.EqPreset
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.theme.PinkAccent
import com.example.wavetune.ui.theme.PurpleAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

@Composable
fun EqualizerScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eqState by viewModel.eqState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen_container")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("eq_back_btn")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Equalizer & Sound Effects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Switch(
                checked = eqState.isEnabled,
                onCheckedChange = { viewModel.toggleEqEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent),
                modifier = Modifier.testTag("eq_enable_switch")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Presets
            item {
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EqPreset.values()) { preset ->
                        FilterChip(
                            selected = eqState.selectedPreset == preset,
                            onClick = { viewModel.setEqPreset(preset) },
                            label = { Text(preset.displayName) },
                            enabled = eqState.isEnabled,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent
                            )
                        )
                    }
                }
            }

            // Frequency Bands Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "5-Band Graphic Equalizer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        eqState.bands.forEach { band ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = band.centerFreqFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val dB = band.currentLevel / 100
                                    Text(
                                        text = "${if (dB > 0) "+$dB" else "$dB"} dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (dB != 0) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Slider(
                                    value = band.currentLevel.toFloat(),
                                    onValueChange = {
                                        viewModel.setEqBandLevel(band.id, it.toInt().toShort())
                                    },
                                    valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                                    enabled = eqState.isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyanAccent,
                                        activeTrackColor = CyanAccent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Audio Enhancement Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Audio Enhancements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )

                        // Bass Boost
                        EnhancementSlider(
                            label = "Bass Boost",
                            value = eqState.bassStrength,
                            max = 1000,
                            enabled = eqState.isEnabled,
                            onValueChange = { viewModel.setBassStrength(it) }
                        )

                        // 3D Virtualizer / Surround
                        EnhancementSlider(
                            label = "3D Virtualizer Surround",
                            value = eqState.virtualizerStrength,
                            max = 1000,
                            enabled = eqState.isEnabled,
                            onValueChange = { viewModel.setVirtualizerStrength(it) }
                        )

                        // Treble Boost
                        EnhancementSlider(
                            label = "Treble Booster",
                            value = eqState.trebleStrength,
                            max = 1000,
                            enabled = eqState.isEnabled,
                            onValueChange = { viewModel.setTrebleStrength(it) }
                        )

                        // Vocal Clarity
                        EnhancementSlider(
                            label = "Vocal Clarity Enhancer",
                            value = eqState.vocalStrength,
                            max = 1000,
                            enabled = eqState.isEnabled,
                            onValueChange = { viewModel.setVocalStrength(it) }
                        )
                    }
                }
            }

            // Balance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stereo Audio Balance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val balanceLabel = when {
                                eqState.balance < -0.1f -> "Left ${(eqState.balance * -100).toInt()}%"
                                eqState.balance > 0.1f -> "Right ${(eqState.balance * 100).toInt()}%"
                                else -> "Center"
                            }
                            Text(
                                text = balanceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }

                        Slider(
                            value = eqState.balance,
                            onValueChange = { viewModel.setBalance(it) },
                            valueRange = -1.0f..1.0f,
                            enabled = eqState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = PinkAccent,
                                activeTrackColor = PinkAccent
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Left (L)", style = MaterialTheme.typography.labelSmall)
                            Text("Center", style = MaterialTheme.typography.labelSmall)
                            Text("Right (R)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun EnhancementSlider(
    label: String,
    value: Int,
    max: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${(value.toFloat() / max * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..max.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent
            )
        )
    }
}
