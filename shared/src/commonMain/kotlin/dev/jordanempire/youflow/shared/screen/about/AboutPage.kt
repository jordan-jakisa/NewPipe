/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.shared.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import dev.jordanempire.youflow.shared.BuildConfig
import dev.jordanempire.youflow.shared.Constants
import dev.jordanempire.youflow.shared.composable.about.LinkListItem
import dev.jordanempire.youflow.shared.model.Link
import dev.jordanempire.youflow.shared.platform.ShareHandler
import dev.jordanempire.youflow.shared.preview.ThemePreviewProvider
import dev.jordanempire.youflow.shared.theme.iconTVDPI
import dev.jordanempire.youflow.shared.theme.logoBackground
import dev.jordanempire.youflow.shared.theme.spaceLarge
import dev.jordanempire.youflow.shared.theme.spaceXSmall
import dev.jordanempire.youflow.shared.theme.spaceXXSmall
import dev.jordanempire.youflow.shared.generated.resources.Res
import dev.jordanempire.youflow.shared.generated.resources.app_description
import dev.jordanempire.youflow.shared.generated.resources.contribution_encouragement
import dev.jordanempire.youflow.shared.generated.resources.contribution_title
import dev.jordanempire.youflow.shared.generated.resources.ic_foreground
import dev.jordanempire.youflow.shared.generated.resources.view_on_github
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AboutPage(shareHandler: ShareHandler = koinInject()) {
    AboutPageContent(
        onOpenUrl = { url -> shareHandler.openUrlInBrowser(url) }
    )
}

@Composable
fun AboutPageContent(
    links: List<Link> = defaultLinks(),
    onOpenUrl: (url: String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(spaceXXSmall)
    ) {
        // Page Header
        item {
            Column(
                modifier = Modifier
                    .padding(spaceLarge)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .requiredSize(iconTVDPI)
                        .clip(CircleShape)
                        .background(color = logoBackground),
                    painter = painterResource(Res.drawable.ic_foreground),
                    contentDescription = BuildConfig.APP_NAME
                )
                Spacer(modifier = Modifier.height(spaceXSmall))
                Text(
                    text = BuildConfig.APP_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.app_description),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Link to the source code
        items(items = links, key = { link -> link.url }) { link ->
            LinkListItem(
                link = link,
                onAction = { onOpenUrl(link.url) }
            )
        }
    }
}

@Composable
private fun defaultLinks() = listOf(
    Link(
        title = stringResource(Res.string.contribution_title),
        description = stringResource(Res.string.contribution_encouragement),
        action = stringResource(Res.string.view_on_github),
        url = Constants.URL_GITHUB
    )
)

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun AboutPagePreview() {
    AboutPageContent()
}
