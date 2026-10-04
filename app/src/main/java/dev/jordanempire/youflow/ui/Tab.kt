package dev.jordanempire.youflow.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

/** The destinations of the bottom bar. The enum name is the key stored in settings. */
internal enum class Tab(val label: String, val selected: ImageVector, val unselected: ImageVector) {
    Home("Home", Icons.Rounded.Home, Icons.Outlined.Home),
    Shorts("Shorts", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle),
    Subscriptions("Subscriptions", Icons.Rounded.Subscriptions, Icons.Outlined.Subscriptions),
    You("You", Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary)
}
