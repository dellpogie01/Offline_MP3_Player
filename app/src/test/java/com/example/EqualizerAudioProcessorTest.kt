package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.data.local.EqualizerEntity
import com.example.service.EqualizerAudioProcessor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class EqualizerAudioProcessorTest {
    private val rate = 48_000

    @Test
    fun disabledEffectsPreservePcmExactly() {
        val processor = processor()
        val input = ShortArray(10_000) { (it * 7919).toShort() }
        assertArrayEquals(input, process(processor, input))
    }

    @Test
    fun everyGraphicBandCutsItsFrequencyAndBoostsItRelativeToOtherFrequencies() {
        val frequencies = listOf(60.0, 230.0, 910.0, 3600.0, 14000.0)
        frequencies.forEachIndexed { index, frequency ->
            val bands = ShortArray(5)
            bands[index] = -1200
            val cut = settings(bands)
            val cutDb = gainDb(cut, frequency)
            assertEquals("Band $frequency Hz must cut by 12 dB", -12.0, cutDb, 0.4)
            bands[index] = 600
            val boost = settings(bands)
            val reference = if (frequency < 4000) 18000.0 else 100.0
            assertTrue("Band $frequency Hz must change the tone, not just volume",
                gainDb(boost, frequency) - gainDb(boost, reference) > 5.0)
        }
    }

    @Test
    fun bassStrengthChangesLiveWithoutRecreatingTheProcessorAndWorksIndependently() {
        val processor = processor()
        val input = tone(60.0)
        val flat = rms(process(processor, input))
        processor.updateSettings(EqualizerEntity(bassBoostEnabled = true, bassBoostStrength = 300))
        val low = rms(process(processor, input))
        // Normalized bass boosts preserve low-end headroom and attenuate treble instead of clipping.
        val lowTreble = rms(process(processor, tone(5000.0)))
        processor.updateSettings(EqualizerEntity(bassBoostEnabled = true, bassBoostStrength = 1000))
        val high = rms(process(processor, input))
        val highTreble = rms(process(processor, tone(5000.0)))
        assertTrue(20 * log10(high / highTreble) - 20 * log10(low / lowTreble) > 6.0)
        processor.updateSettings(EqualizerEntity())
        assertEquals("Disabling must restore flat audio", flat, rms(process(processor, input)), 1.0)
    }

    @Test
    fun stereoChannelsStayIndependentAndSettingsSurviveFlushAndFormatChange() {
        val processor = processor(2)
        processor.updateSettings(EqualizerEntity(enabled = true, band1 = -1200))
        val mono = tone(60.0)
        val stereo = ShortArray(mono.size * 2) { if (it % 2 == 0) mono[it / 2] else 0 }
        val result = process(processor, stereo)
        assertTrue(result.indices.filter { it % 2 == 1 }.all { result[it] == 0.toShort() })
        processor.configure(AudioProcessor.AudioFormat(44_100, 1, C.ENCODING_PCM_16BIT))
        processor.flush()
        val changedFormat = tone(60.0, 44_100)
        val gain = 20 * log10(rms(process(processor, changedFormat)) / rms(changedFormat))
        assertEquals(-12.0, gain, 0.4)
    }

    @Test
    fun combinedMaximumEqAndBassUseHeadroomInsteadOfClippingSteadyAudio() {
        val processor = processor()
        processor.updateSettings(EqualizerEntity(enabled = true, band1 = 1500, band2 = 1500,
            band3 = 1500, band4 = 1500, band5 = 1500, bassBoostEnabled = true, bassBoostStrength = 1000))
        val input = ShortArray(rate * 2) { frame ->
            (listOf(60.0, 230.0, 910.0, 3600.0, 14000.0).sumOf {
                sin(frame * 2 * PI * it / rate)
            } * 6000).toInt().toShort()
        }
        val steady = process(processor, input).drop(rate)
        assertTrue(steady.any { abs(it.toInt()) > 1000 })
        assertTrue("No hard-clipped samples", steady.all { it != Short.MAX_VALUE && it != Short.MIN_VALUE })
    }

    private fun settings(bands: ShortArray) = EqualizerEntity(enabled = true,
        band1 = bands[0], band2 = bands[1], band3 = bands[2], band4 = bands[3], band5 = bands[4])

    private fun processor(channels: Int = 1) = EqualizerAudioProcessor().apply {
        configure(AudioProcessor.AudioFormat(rate, channels, C.ENCODING_PCM_16BIT))
        flush()
    }

    private fun gainDb(settings: EqualizerEntity, frequency: Double): Double {
        val input = tone(frequency)
        val processor = processor()
        processor.updateSettings(settings)
        return 20 * log10(rms(process(processor, input)) / rms(input))
    }

    private fun tone(frequency: Double, rate: Int = this.rate) = ShortArray(rate * 2) {
        (sin(it * 2 * PI * frequency / rate) * 10_000).roundToInt().toShort()
    }

    private fun process(processor: EqualizerAudioProcessor, input: ShortArray): ShortArray {
        val buffer = ByteBuffer.allocateDirect(input.size * 2).order(ByteOrder.nativeOrder())
        input.forEach { buffer.putShort(it) }
        buffer.flip()
        processor.queueInput(buffer)
        assertFalse("The entire PCM buffer must be consumed", buffer.hasRemaining())
        val output = processor.output.order(ByteOrder.nativeOrder())
        return ShortArray(output.remaining() / 2) { output.short }
    }

    private fun rms(samples: ShortArray): Double {
        val steady = samples.drop(samples.size / 2)
        return sqrt(steady.sumOf { it.toDouble() * it } / steady.size)
    }
}
