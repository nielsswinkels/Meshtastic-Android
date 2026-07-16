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
package org.meshtastic.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.navigation.ConnectionsRoute
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.feature.settings.easy.EasySettingsScreen

/**
 * Easy mode registration for the settings tab root: the same [SettingsRoute.Settings] key the full app uses, rendered
 * as the minimal Easy settings screen.
 *
 * @param onOpenNotificationSettings platform hook for the notifications row (Android launches the system app
 *   notification settings); pass null on desktop to render the in-app toggles inline.
 */
fun EntryProviderScope<NavKey>.easySettingsGraph(
    backStack: NavBackStack<NavKey>,
    onOpenNotificationSettings: (() -> Unit)? = null,
) {
    entry<SettingsRoute.Settings> {
        EasySettingsScreen(
            settingsViewModel = koinViewModel(),
            connectionsViewModel = koinViewModel(),
            radioConfigViewModel = getRadioConfigViewModel(backStack),
            onOpenConnections = { backStack.add(ConnectionsRoute.Connections()) },
            onOpenNotificationSettings = onOpenNotificationSettings,
        )
    }
}
