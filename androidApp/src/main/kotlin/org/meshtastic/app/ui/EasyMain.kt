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
@file:Suppress("MatchingDeclarationName")

package org.meshtastic.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.navigation.ConnectionsRoute
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.navigation.rememberMultiBackstack
import org.meshtastic.core.repository.PlatformAnalytics
import org.meshtastic.core.ui.component.MeshtasticAppShell
import org.meshtastic.core.ui.component.MeshtasticNavDisplay
import org.meshtastic.core.ui.component.easy.EasyDisconnectedBanner
import org.meshtastic.core.ui.component.easy.EasyNavigationSuite
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.feature.connections.navigation.connectionsGraph
import org.meshtastic.feature.map.navigation.mapGraph
import org.meshtastic.feature.messaging.navigation.easyContactsGraph
import org.meshtastic.feature.node.navigation.easyNodesGraph
import org.meshtastic.feature.settings.navigation.easySettingsGraph

/**
 * The Easy mode shell: the same app plumbing as [MainScreen] (deep links, alerts, snackbars, version check) with a
 * four-tab Signal-style UI. The full app's routes are reused, rendered by the simplified Easy screens, so notifications
 * and deep links land in the right place with no extra wiring.
 */
@Composable
fun EasyMainScreen(modifier: Modifier = Modifier) {
    val viewModel: UIViewModel = koinViewModel()
    val context = LocalContext.current

    // Easy mode always starts on Chats; connection problems surface through the banner instead of a Connect tab.
    val multiBackstack = rememberMultiBackstack(TopLevelDestination.Messages.route)
    val backStack = multiBackstack.activeBackStack
    val scrollToTopEvents = viewModel.scrollToTopEventFlow

    AndroidAppVersionCheck(viewModel)
    AndroidLockdownHandler(viewModel)

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    MeshtasticAppShell(multiBackstack = multiBackstack, uiViewModel = viewModel, hostModifier = modifier) {
        EasyNavigationSuite(
            multiBackstack = multiBackstack,
            uiViewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize().recalculateWindowInsets().safeDrawingPadding()) {
                AnimatedVisibility(visible = connectionState is ConnectionState.Disconnected) {
                    EasyDisconnectedBanner(
                        onConnectClick = { backStack.add(ConnectionsRoute.Connections()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MeshtasticNavDisplay(
                    multiBackstack = multiBackstack,
                    entryProvider =
                    entryProvider<NavKey> {
                        easyContactsGraph(backStack, scrollToTopEvents)
                        easyNodesGraph(backStack, scrollToTopEvents)
                        mapGraph(backStack)
                        easySettingsGraph(
                            backStack = backStack,
                            onOpenNotificationSettings = {
                                val intent =
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                context.startActivity(intent)
                            },
                        )
                        connectionsGraph(backStack)
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    analytics = koinInject<PlatformAnalytics>(),
                )
            }
        }
    }
}
