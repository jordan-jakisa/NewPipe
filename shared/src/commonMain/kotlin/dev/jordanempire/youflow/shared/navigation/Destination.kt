/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import dev.jordanempire.youflow.shared.model.License

/**
 * Destinations for navigation in compose
 */
@Serializable
sealed interface Destination : NavKey {

    @Serializable
    data object AppearanceSettings : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object About : Destination
}

