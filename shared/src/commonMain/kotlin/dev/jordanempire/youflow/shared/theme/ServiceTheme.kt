/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.shared.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode

val youTubeLightScheme = lightColorScheme(
    primaryContainer = Color(0xFFE53935),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val youTubeDarkScheme = darkColorScheme(
    primaryContainer = Color(0xFF992722),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

/**
 * The only supported service in the app and minor information about it for UI decisions.
 * @property serviceId ID of the service as defined in NewPipeExtractor
 * @property serviceName Name of the service as defined in NewPipeExtractor
 * @property lightScheme Light color scheme to reflect the brand
 * @property darkScheme Dark color scheme to reflect the brand
 * @property isSchemeColorDensityLight Whether this brand's color schemes are of lighter density.
 */
enum class Service(
    val serviceId: Int,
    val serviceName: String,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val isSchemeColorDensityLight: Boolean = false
) {
    YOUTUBE(
        serviceId = 0,
        serviceName = "YouTube",
        lightScheme = youTubeLightScheme,
        darkScheme = youTubeDarkScheme
    )
}

/**
 * Currently active service. The app only supports YouTube, so this never changes and never fails.
 */
fun currentService(): Service = Service.YOUTUBE

/**
 * Currently active service's color that can be used to represent it.
 * Fallbacks to YouTube on preview.
 */
@Composable
fun currentServiceScheme(
    isPreview: Boolean = LocalInspectionMode.current,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    service: Service = if (isPreview) Service.YOUTUBE else currentService()
): ColorScheme {
    return when {
        useDarkTheme -> service.darkScheme
        else -> service.lightScheme
    }
}

/**
 * Top app bar colors to represent the currently active service.
 * Fallbacks to YouTube on preview.
 */
@Composable
fun currentServiceTopAppBarColors(
    serviceScheme: ColorScheme = currentServiceScheme()
): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = serviceScheme.primaryContainer,
        scrolledContainerColor = serviceScheme.primaryContainer,
        navigationIconContentColor = serviceScheme.onPrimaryContainer,
        titleContentColor = serviceScheme.onPrimaryContainer,
        subtitleContentColor = serviceScheme.onPrimaryContainer,
        actionIconContentColor = serviceScheme.onPrimaryContainer
    )
}
