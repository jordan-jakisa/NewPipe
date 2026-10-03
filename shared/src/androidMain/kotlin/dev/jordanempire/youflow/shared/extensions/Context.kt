/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.shared.extensions

import android.content.Context
import android.content.Intent
import kotlinx.serialization.json.Json
import dev.jordanempire.youflow.shared.android.Constants
import dev.jordanempire.youflow.shared.ComposeActivity
import dev.jordanempire.youflow.shared.navigation.Destination

/**
 * Navigates to a given compose destination
 */
fun Context.navigateTo(destination: Destination) = Intent(this, ComposeActivity::class.java).also { intent ->
    intent.putExtra(Constants.INTENT_SCREEN_KEY, Json.encodeToString(destination))
    startActivity(intent)
}
