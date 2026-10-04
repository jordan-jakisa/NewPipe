package dev.jordanempire.youflow.ui

import dev.jordanempire.youflow.ui.util.formatCount
import dev.jordanempire.youflow.ui.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun durationUnderAnHour() = assertEquals("4:07", formatDuration(247))

    @Test
    fun durationOverAnHour() = assertEquals("1:02:03", formatDuration(3723))

    @Test
    fun durationZeroIsEmpty() = assertEquals("", formatDuration(0))

    @Test
    fun smallCountsAreExact() = assertEquals("999 views", formatCount(999, "views"))

    @Test
    fun thousandsAreCompact() = assertEquals("25.4K views", formatCount(25_400, "views"))

    @Test
    fun wholeThousandsDropTheDecimal() = assertEquals("2K subscribers", formatCount(2_000, "subscribers"))

    @Test
    fun millionsAndBillions() {
        assertEquals("1.2M views", formatCount(1_200_000, "views"))
        assertEquals("38.2M views", formatCount(38_200_000, "views"))
        assertEquals("3.1B views", formatCount(3_100_000_000, "views"))
    }

    @Test
    fun hundredsOfThousandsHaveNoDecimal() = assertEquals("148K views", formatCount(148_000, "views"))
}
