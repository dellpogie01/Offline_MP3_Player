package com.example.service

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.example.data.local.EqualizerEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

/** Processes decoded PCM in the player's sink, independent of device audio-effect support. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class EqualizerAudioProcessor : BaseAudioProcessor() {
    private val settings = AtomicReference(EqualizerEntity())
    private var applied: EqualizerEntity? = null
    private var filters = emptyArray<Biquad>()
    private val gains = DoubleArray(6)
    private val targets = DoubleArray(6)
    private var preamp = 1.0
    private var preampDb = 0.0
    private var targetPreampDb = 0.0
    private var frame = 0

    fun updateSettings(value: EqualizerEntity) { settings.set(value) }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        // Stay active even when flat: sliders can take effect without rebuilding the player.
        return inputAudioFormat
    }

    override fun onFlush() {
        filters = Array(6) { Biquad(inputAudioFormat.channelCount.coerceAtLeast(1)) }
        gains.fill(0.0)
        applied = null
        preamp = 1.0
        preampDb = 0.0
        frame = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining())
        inputBuffer.order(ByteOrder.nativeOrder())
        val sampleRate = inputAudioFormat.sampleRate
        val channels = inputAudioFormat.channelCount
        val current = settings.get()
        if (current != applied) {
            val bands = shortArrayOf(current.band1, current.band2, current.band3, current.band4, current.band5)
            for (i in 0..4) targets[i] = if (current.enabled && FREQUENCIES[i] < sampleRate * 0.49) {
                bands[i].toDouble().coerceIn(-1500.0, 1500.0) / 100.0
            } else 0.0
            targets[5] = if (current.bassBoostEnabled) current.bassBoostStrength.toDouble().coerceIn(0.0, 1000.0) * 0.012 else 0.0
            val response = Array(6) { coefficients(it, targets[it], sampleRate) }
            // Estimate the combined peak response, rather than summing all positive slider gains.
            val peakDb = (0..128).maxOf { index ->
                val frequency = 10.0 * (sampleRate * 0.049).pow(index / 128.0)
                response.sumOf { it.responseDb(2 * PI * frequency / sampleRate) }
            }.coerceAtLeast(0.0)
            targetPreampDb = if (peakDb > 0.01) -(peakDb + 0.5) else 0.0
            applied = current
        }
        val smoothing = 1.0 - exp(-64.0 / (sampleRate * 0.02))
        while (inputBuffer.remaining() >= channels * 2) {
            if (frame++ % 64 == 0) {
                for (i in gains.indices) {
                    gains[i] += (targets[i] - gains[i]) * smoothing
                    if (abs(gains[i] - targets[i]) < 0.0001) gains[i] = targets[i]
                    filters[i].coefficients = coefficients(i, gains[i], sampleRate)
                }
                preampDb += (targetPreampDb - preampDb) * smoothing
                preamp = 10.0.pow(preampDb / 20)
            }
            repeat(channels) { channel ->
                var sample = inputBuffer.short.toDouble() * preamp
                for (filter in filters) sample = filter.process(sample, channel)
                output.putShort(sample.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
        }
        output.flip()
    }

    private fun coefficients(index: Int, gain: Double, rate: Int): Coefficients {
        val frequency = if (index == 5) 150.0 else FREQUENCIES[index]
        if (abs(gain) < 0.00001 || frequency >= rate * 0.49) return Coefficients()
        val a = 10.0.pow(gain / 40)
        val omega = 2 * PI * frequency / rate
        val cosine = cos(omega)
        if (index < 5) {
            val alpha = sin(omega) / 2 // Peaking EQ, Q = 1.
            val a0 = 1 + alpha / a
            return Coefficients((1 + alpha * a) / a0, -2 * cosine / a0,
                (1 - alpha * a) / a0, -2 * cosine / a0, (1 - alpha / a) / a0)
        }
        // RBJ low-shelf filter, slope S = 1; strength spans 0..12 dB below 150 Hz.
        val beta = sqrt(2 * a) * sin(omega)
        val a0 = (a + 1) + (a - 1) * cosine + beta
        return Coefficients(a * ((a + 1) - (a - 1) * cosine + beta) / a0,
            2 * a * ((a - 1) - (a + 1) * cosine) / a0,
            a * ((a + 1) - (a - 1) * cosine - beta) / a0,
            -2 * ((a - 1) + (a + 1) * cosine) / a0,
            ((a + 1) + (a - 1) * cosine - beta) / a0)
    }

    private data class Coefficients(
        val b0: Double = 1.0, val b1: Double = 0.0, val b2: Double = 0.0,
        val a1: Double = 0.0, val a2: Double = 0.0
    ) {
        fun responseDb(omega: Double): Double {
            val real = b0 + b1 * cos(omega) + b2 * cos(2 * omega)
            val imag = -b1 * sin(omega) - b2 * sin(2 * omega)
            val denominatorReal = 1 + a1 * cos(omega) + a2 * cos(2 * omega)
            val denominatorImag = -a1 * sin(omega) - a2 * sin(2 * omega)
            return 10 * log10((real * real + imag * imag) /
                (denominatorReal * denominatorReal + denominatorImag * denominatorImag))
        }
    }

    private class Biquad(channels: Int) {
        var coefficients = Coefficients()
        private val z1 = DoubleArray(channels)
        private val z2 = DoubleArray(channels)
        fun process(input: Double, channel: Int): Double {
            val c = coefficients
            val output = c.b0 * input + z1[channel]
            z1[channel] = c.b1 * input - c.a1 * output + z2[channel]
            z2[channel] = c.b2 * input - c.a2 * output
            return output
        }
    }

    companion object {
        private val FREQUENCIES = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
    }
}
