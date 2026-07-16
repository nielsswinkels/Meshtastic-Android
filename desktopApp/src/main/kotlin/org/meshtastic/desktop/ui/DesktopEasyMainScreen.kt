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
package org.meshtastic.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.navigation.ConnectionsRoute
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.ui.component.MeshtasticAppShell
import org.meshtastic.core.ui.component.MeshtasticNavDisplay
import org.meshtastic.core.ui.component.easy.EasyDisconnectedBanner
import org.meshtastic.core.ui.component.easy.EasyNavigationSuite
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.feature.connections.navigation.connectionsGraph
import org.meshtastic.feature.discovery.navigation.discoveryGraph
import org.meshtastic.feature.docs.navigation.docsEntries
import org.meshtastic.feature.firmware.navigation.firmwareGraph
import org.meshtastic.feature.map.navigation.mapGraph
import org.meshtastic.feature.messaging.navigation.easyContactsGraph
import org.meshtastic.feature.node.navigation.easyNodesGraph
import org.meshtastic.feature.settings.navigation.easySettingsGraph
import org.meshtastic.feature.settings.radio.channel.channelsGraph
import org.meshtastic.feature.wifiprovision.navigation.wifiProvisionGraph

/**
 * Desktop Easy mode shell — the desktop counterpart of the Android `EasyMainScreen`: four friendly tabs over the same
 * routes the full app uses. Notifications settings render inline (desktop has in-app toggles).
 */
@Suppress("ViewModelForwarding")
@Composable
fun DesktopEasyMainScreen(uiViewModel: UIViewModel, multiBackstack: MultiBackstack, modifier: Modifier = Modifier) {
    val backStack = multiBackstack.activeBackStack
    val scrollToTopEvents = uiViewModel.scrollToTopEventFlow
    val connectionState by uiViewModel.connectionState.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize()) {
        MeshtasticAppShell(
            multiBackstack = multiBackstack,
            uiViewModel = uiViewModel,
            hostModifier = Modifier.padding(bottom = 24.dp),
        ) {
            EasyNavigationSuite(
                multiBackstack = multiBackstack,
                uiViewModel = uiViewModel,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = connectionState is ConnectionState.Disconnected) {
                        EasyDisconnectedBanner(
                            onConnectClick = {
                                if (backStack.lastOrNull() !is ConnectionsRoute.Connections) {
                                    backStack.add(ConnectionsRoute.Connections())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val provider =
                        entryProvider<NavKey> {
                            easyContactsGraph(backStack, scrollToTopEvents)
                            easyNodesGraph(backStack, scrollToTopEvents)
                            mapGraph(backStack)
                            easySettingsGraph(backStack)
                            connectionsGraph(backStack)
                            // Not reachable from Easy screens, but registered so externally-triggered routes
                            // (deep links, docs links) render instead of crashing NavDisplay.
                            channelsGraph(backStack)
                            discoveryGraph(backStack)
                            docsEntries(backStack)
                            firmwareGraph(backStack)
                            wifiProvisionGraph(backStack)
                        }
                    MeshtasticNavDisplay(
                        multiBackstack = multiBackstack,
                        entryProvider = provider,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}
