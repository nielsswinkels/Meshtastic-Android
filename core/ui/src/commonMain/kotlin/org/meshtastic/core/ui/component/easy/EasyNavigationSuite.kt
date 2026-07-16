/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.ui.component.easy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bottom_nav_settings
import org.meshtastic.core.resources.chats
import org.meshtastic.core.resources.ic_forum
import org.meshtastic.core.resources.ic_groups
import org.meshtastic.core.resources.ic_map
import org.meshtastic.core.resources.ic_settings
import org.meshtastic.core.resources.map
import org.meshtastic.core.resources.people
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.viewmodel.UIViewModel

/** The four tabs of the Easy mode shell, in display order, with friendly labels and icons. */
private val easyDestinations =
    listOf(
        TopLevelDestination.Messages,
        TopLevelDestination.Nodes,
        TopLevelDestination.Map,
        TopLevelDestination.Settings,
    )

private val TopLevelDestination.easyLabel: StringResource
    get() =
        when (this) {
            TopLevelDestination.Messages -> Res.string.chats
            TopLevelDestination.Nodes -> Res.string.people
            TopLevelDestination.Map -> Res.string.map
            else -> Res.string.bottom_nav_settings
        }

/**
 * Navigation shell for Easy mode: the same adaptive [NavigationSuiteScaffold] the full app uses, but with only the
 * Chats/People/Map/Settings tabs, always-visible labels, and no technical iconography. The Connect tab is intentionally
 * absent — connection problems surface through the friendly banner instead.
 */
@Composable
fun EasyNavigationSuite(
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val unreadMessageCount by uiViewModel.unreadMessageCount.collectAsStateWithLifecycle()

    val currentDestination = TopLevelDestination.fromNavKey(multiBackstack.currentTabRoute)
    val layoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2()).coerceNavigationType()

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        navigationSuiteItems = {
            easyDestinations.forEach { destination ->
                item(
                    selected = destination == currentDestination,
                    onClick = { handleEasyNavigation(destination, currentDestination, multiBackstack, uiViewModel) },
                    icon = { EasyNavigationIcon(destination = destination, unreadMessageCount = unreadMessageCount) },
                    label = { Text(stringResource(destination.easyLabel)) },
                    alwaysShowLabel = true,
                )
            }
        },
    ) {
        Row { content() }
    }
}

/** Keeps wide layouts on a NavigationRail instead of promoting to a permanent NavigationDrawer. */
private fun NavigationSuiteType.coerceNavigationType(): NavigationSuiteType = when (this) {
    NavigationSuiteType.NavigationDrawer -> NavigationSuiteType.NavigationRail
    else -> this
}

private fun handleEasyNavigation(
    destination: TopLevelDestination,
    currentDestination: TopLevelDestination?,
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
) {
    if (destination != currentDestination) {
        multiBackstack.navigateTopLevel(destination.route)
        return
    }
    // Repressing the current tab: scroll to top when already on the tab root, otherwise reset to the root.
    val currentKey = multiBackstack.activeBackStack.lastOrNull()
    when {
        destination == TopLevelDestination.Messages && currentKey is ContactsRoute.Contacts ->
            uiViewModel.emitScrollToTopEvent(ScrollToTopEvent.ConversationsTabPressed)

        destination == TopLevelDestination.Nodes && currentKey is NodesRoute.Nodes ->
            uiViewModel.emitScrollToTopEvent(ScrollToTopEvent.NodesTabPressed)

        currentKey != destination.route -> multiBackstack.navigateTopLevel(destination.route)
    }
}

@Composable
private fun EasyNavigationIcon(destination: TopLevelDestination, unreadMessageCount: Int) {
    BadgedBox(
        badge = {
            if (destination == TopLevelDestination.Messages) {
                var lastNonZeroCount by remember { mutableIntStateOf(unreadMessageCount) }
                if (unreadMessageCount > 0) {
                    lastNonZeroCount = unreadMessageCount
                }
                AnimatedVisibility(
                    visible = unreadMessageCount > 0,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Badge { Text(lastNonZeroCount.toString()) }
                }
            }
        },
    ) {
        val icon =
            when (destination) {
                TopLevelDestination.Messages -> Res.drawable.ic_forum
                TopLevelDestination.Nodes -> Res.drawable.ic_groups
                TopLevelDestination.Map -> Res.drawable.ic_map
                else -> Res.drawable.ic_settings
            }
        Icon(imageVector = vectorResource(icon), contentDescription = stringResource(destination.easyLabel))
    }
}
