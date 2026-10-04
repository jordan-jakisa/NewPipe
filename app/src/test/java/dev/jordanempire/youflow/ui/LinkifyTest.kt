package dev.jordanempire.youflow.ui

import androidx.compose.ui.graphics.Color
import dev.jordanempire.youflow.ui.watch.linkify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkifyTest {
    private fun seeks(text: String) = linkify(text, Color.Red).getStringAnnotations("seek", 0, text.length).map { it.item.toLong() }

    @Test
    fun minutesAndSeconds() = assertEquals(listOf(90_000L), seeks("jump to 1:30 now"))

    @Test
    fun hoursMinutesSeconds() = assertEquals(listOf(3_723_000L), seeks("outro at 1:02:03"))

    @Test
    fun severalTimestamps() = assertEquals(listOf(0L, 65_000L), seeks("0:00 intro\n1:05 verse"))

    @Test
    fun clockTimesInsideLongerNumbersAreIgnored() = assertTrue(seeks("version 10:5:99 build").isEmpty())

    @Test
    fun urlsBecomeLinks() {
        val text = "Shop: https://example.com/a?b=1 thanks"
        val urls = linkify(text, Color.Red).getStringAnnotations("url", 0, text.length)
        assertEquals(listOf("https://example.com/a?b=1"), urls.map { it.item })
    }
}
