/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ServiceThemeTest {

    @Test
    fun testOnlyServiceIsYouTube() {
        assertEquals(listOf(Service.YOUTUBE), Service.entries)
        assertEquals(0, Service.YOUTUBE.serviceId)
        assertEquals("YouTube", Service.YOUTUBE.serviceName)
    }

    @Test
    fun testCurrentServiceIsAlwaysYouTube() {
        assertSame(Service.YOUTUBE, currentService())
    }
}
