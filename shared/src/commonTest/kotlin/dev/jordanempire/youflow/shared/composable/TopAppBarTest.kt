/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.shared.composable

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.jordanempire.youflow.shared.extensions.withKoin
import dev.jordanempire.youflow.shared.generated.resources.Res
import dev.jordanempire.youflow.shared.generated.resources.navigate_back
import dev.jordanempire.youflow.shared.generated.resources.title_activity_about
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class TopAppBarTest {

    private val emptySettings = module {
        single<Settings> { MapSettings() }
    }

    @Test
    fun testTopAppBarHasNoNavigationByDefault() = runComposeUiTest {
        withKoin(
            modules = listOf(emptySettings),
            content = {
                TopAppBar()
            },
            onContent = {
                onNodeWithContentDescription(getString(Res.string.navigate_back))
                    .assertDoesNotExist()
            }
        )
    }

    @Test
    fun testTopAppBarCanHaveNavigation() = runComposeUiTest {
        var navigationBackClicked = false
        withKoin(
            modules = listOf(emptySettings),
            content = {
                TopAppBar(
                    title = stringResource(Res.string.title_activity_about),
                    onNavigateUp = { navigationBackClicked = true }
                )
            },
            onContent = {
                onNodeWithText(getString(Res.string.title_activity_about)).assertIsDisplayed()
                onNodeWithContentDescription(getString(Res.string.navigate_back)).apply {
                    assertExists()
                    performClick()
                    assertTrue(navigationBackClicked)
                }
            }
        )
    }
}
