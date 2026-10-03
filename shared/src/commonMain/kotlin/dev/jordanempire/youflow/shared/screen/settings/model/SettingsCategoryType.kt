package dev.jordanempire.youflow.shared.screen.settings.model

import dev.jordanempire.youflow.shared.generated.resources.Res
import dev.jordanempire.youflow.shared.generated.resources.content
import dev.jordanempire.youflow.shared.generated.resources.ic_bug_report
import dev.jordanempire.youflow.shared.generated.resources.ic_file_download
import dev.jordanempire.youflow.shared.generated.resources.ic_headset
import dev.jordanempire.youflow.shared.generated.resources.ic_history
import dev.jordanempire.youflow.shared.generated.resources.ic_language
import dev.jordanempire.youflow.shared.generated.resources.ic_notifications
import dev.jordanempire.youflow.shared.generated.resources.ic_palette
import dev.jordanempire.youflow.shared.generated.resources.ic_settings_backup_restore
import dev.jordanempire.youflow.shared.generated.resources.notifications
import dev.jordanempire.youflow.shared.generated.resources.settings_category_appearance_title
import dev.jordanempire.youflow.shared.generated.resources.settings_category_backup_restore_title
import dev.jordanempire.youflow.shared.generated.resources.settings_category_debug_title
import dev.jordanempire.youflow.shared.generated.resources.settings_category_downloads_title
import dev.jordanempire.youflow.shared.generated.resources.settings_category_history_title
import dev.jordanempire.youflow.shared.generated.resources.settings_category_video_audio_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * Categories on the settings home screen.
 */
enum class SettingsCategoryType(
    val title: StringResource,
    val icon: DrawableResource
) {
    VIDEO_AUDIO(Res.string.settings_category_video_audio_title, Res.drawable.ic_headset),
    DOWNLOADS(Res.string.settings_category_downloads_title, Res.drawable.ic_file_download),
    APPEARANCE(Res.string.settings_category_appearance_title, Res.drawable.ic_palette),
    HISTORY(Res.string.settings_category_history_title, Res.drawable.ic_history),
    CONTENT(Res.string.content, Res.drawable.ic_language),
    NOTIFICATIONS(Res.string.notifications, Res.drawable.ic_notifications),
    BACKUP_RESTORE(Res.string.settings_category_backup_restore_title, Res.drawable.ic_settings_backup_restore),
    DEBUG(Res.string.settings_category_debug_title, Res.drawable.ic_bug_report)
}
