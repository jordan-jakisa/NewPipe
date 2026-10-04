package dev.jordanempire.youflow.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingLinkTest {
    @Test
    fun shortLinkIsAStream() {
        val link = parseIncoming("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(link is IncomingLink.Stream)
    }

    @Test
    fun watchLinkInsideSharedTextIsAStream() {
        val link = parseIncoming("Look at this https://www.youtube.com/watch?v=dQw4w9WgXcQ amazing")
        assertEquals(IncomingLink.Stream("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), link)
    }

    @Test
    fun channelLink() {
        assertTrue(parseIncoming("https://www.youtube.com/channel/UC4R8DWoMoI7CAwX8_LjQHig") is IncomingLink.Channel)
    }

    @Test
    fun handleLink() {
        assertTrue(parseIncoming("https://www.youtube.com/@JordanMatter") is IncomingLink.Channel)
    }

    @Test
    fun playlistLink() {
        assertTrue(parseIncoming("https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI") is IncomingLink.Playlist)
    }

    @Test
    fun plainTextBecomesASearch() {
        assertEquals(IncomingLink.Search("lofi hip hop"), parseIncoming("lofi hip hop"))
    }

    @Test
    fun nonYouTubeLinkBecomesASearch() {
        assertTrue(parseIncoming("https://example.com/video/1") is IncomingLink.Search)
    }
}
