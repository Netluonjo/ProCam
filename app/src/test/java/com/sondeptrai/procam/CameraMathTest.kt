package com.sondeptrai.procam

import android.util.Rational
import com.sondeptrai.procam.core.camera.CameraHardwareInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraMathTest {

    @Test
    fun testShutterSpeedFormatting() {
        val info = CameraHardwareInfo()

        // 1/8000s in nanoseconds = 125,000 ns
        assertEquals("1/8000", info.formatShutterSpeed(125_000L))

        // 1/1000s in nanoseconds = 1,000,000 ns
        assertEquals("1/1000", info.formatShutterSpeed(1_000_000L))

        // 1/100s in nanoseconds = 10,000,000 ns
        assertEquals("1/100", info.formatShutterSpeed(10_000_000L))

        // 1/2s in nanoseconds = 500,000,000 ns
        assertEquals("1/2", info.formatShutterSpeed(500_000_000L))

        // 2 seconds in nanoseconds = 2,000,000,000 ns
        assertEquals("2.0\"", info.formatShutterSpeed(2_000_000_000L))
    }

    @Test
    fun testEvFormatting() {
        val info = CameraHardwareInfo(aeCompensationStep = Rational(1, 3))

        assertEquals("0.0", info.formatEv(0))
        assertEquals("+1.0", info.formatEv(3))
        assertEquals("-1.0", info.formatEv(-3))
    }
}
