/*
* SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package dev.jordanempire.youflow.shared.viewmodel.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import dev.jordanempire.youflow.shared.platform.BuildInfo
import dev.jordanempire.youflow.shared.screen.settings.model.SettingsCategoryType
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(buildInfo: BuildInfo) : ViewModel() {

    val categories: StateFlow<List<SettingsCategoryType>>
        field = MutableStateFlow(computeVisible(buildInfo))

    private fun computeVisible(buildInfo: BuildInfo): List<SettingsCategoryType> =
        SettingsCategoryType.entries.filter { type ->
            when (type) {
                SettingsCategoryType.DEBUG -> buildInfo.isDebug
                else -> true
            }
        }
}

