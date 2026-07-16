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
package org.meshtastic.feature.settings.easy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_notifications
import org.meshtastic.core.resources.app_version
import org.meshtastic.core.resources.bottom_nav_settings
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.connected
import org.meshtastic.core.resources.connected_sleeping
import org.meshtastic.core.resources.connecting
import org.meshtastic.core.resources.easy_airtime
import org.meshtastic.core.resources.easy_open_connection_settings
import org.meshtastic.core.resources.easy_switch_to_advanced
import org.meshtastic.core.resources.easy_switch_to_advanced_message
import org.meshtastic.core.resources.easy_your_device
import org.meshtastic.core.resources.easy_your_name
import org.meshtastic.core.resources.easy_your_name_hint
import org.meshtastic.core.resources.long_name
import org.meshtastic.core.resources.must_set_region
import org.meshtastic.core.resources.not_connected
import org.meshtastic.core.resources.save
import org.meshtastic.core.resources.short_name
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MaterialBatteryInfo
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.easy.EasyAirtimeLevel
import org.meshtastic.core.ui.component.easy.EasyAvatar
import org.meshtastic.core.ui.component.easy.airtimeBudgetFraction
import org.meshtastic.core.ui.viewmodel.ConnectionStatus
import org.meshtastic.core.ui.viewmodel.ConnectionsViewModel
import org.meshtastic.feature.settings.SettingsViewModel
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.NotificationSection
import org.meshtastic.feature.settings.radio.RadioConfigViewModel

private const val LONG_NAME_MAX_LENGTH = 39
private const val SHORT_NAME_MAX_LENGTH = 4

/**
 * Easy mode settings: the four things a non-technical user needs — their name, whether the device is connected,
 * notifications, and a quiet, guarded way back to the advanced app. Everything else stays in the full settings.
 *
 * @param onOpenNotificationSettings platform hook for the notifications row; when null (desktop) the in-app
 *   notification toggles render inline instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun EasySettingsScreen(
    settingsViewModel: SettingsViewModel,
    connectionsViewModel: ConnectionsViewModel,
    radioConfigViewModel: RadioConfigViewModel,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenNotificationSettings: (() -> Unit)? = null,
) {
    val ourNode by settingsViewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val isConnected by settingsViewModel.isConnected.collectAsStateWithLifecycle()
    val connectionStatus by connectionsViewModel.connectionStatus.collectAsStateWithLifecycle()
    val deviceNode by connectionsViewModel.ourNodeForDisplay.collectAsStateWithLifecycle()

    var showNameDialog by remember { mutableStateOf(false) }
    var showAdvancedDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.bottom_nav_settings)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ExpressiveSection(title = stringResource(Res.string.easy_your_name)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ourNode?.let { EasyAvatar(text = it.user.short_name, colors = it.colors) }
                }
                ListItem(
                    text = ourNode?.user?.long_name.orEmpty(),
                    supportingText = stringResource(Res.string.easy_your_name_hint),
                    enabled = isConnected && ourNode != null,
                    onClick = { showNameDialog = true },
                )
            }

            ExpressiveSection(title = stringResource(Res.string.easy_your_device)) {
                ListItem(text = stringResource(connectionStatus.easyLabel()), trailingIcon = null, onClick = null)
                deviceNode?.let { node ->
                    if (node.batteryLevel != null) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            MaterialBatteryInfo(level = node.batteryLevel, voltage = node.voltage)
                        }
                    }
                }
                EasyAirtimeRow(airUtilTx = ourNode?.deviceMetrics?.air_util_tx)
                ListItem(text = stringResource(Res.string.easy_open_connection_settings), onClick = onOpenConnections)
            }

            if (onOpenNotificationSettings != null) {
                ExpressiveSection(title = stringResource(Res.string.app_notifications)) {
                    ListItem(text = stringResource(Res.string.app_notifications), onClick = onOpenNotificationSettings)
                }
            } else {
                val messagesEnabled by settingsViewModel.messagesEnabled.collectAsStateWithLifecycle()
                val nodeEventsEnabled by settingsViewModel.nodeEventsEnabled.collectAsStateWithLifecycle()
                val lowBatteryEnabled by settingsViewModel.lowBatteryEnabled.collectAsStateWithLifecycle()
                NotificationSection(
                    messagesEnabled = messagesEnabled,
                    onToggleMessages = settingsViewModel::setMessagesEnabled,
                    nodeEventsEnabled = nodeEventsEnabled,
                    onToggleNodeEvents = settingsViewModel::setNodeEventsEnabled,
                    lowBatteryEnabled = lowBatteryEnabled,
                    onToggleLowBattery = settingsViewModel::setLowBatteryEnabled,
                )
            }

            ExpressiveSection(title = stringResource(Res.string.app_version)) {
                ListItem(text = settingsViewModel.appVersionName, trailingIcon = null, onClick = null)
                ListItem(
                    text = stringResource(Res.string.easy_switch_to_advanced),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { showAdvancedDialog = true },
                )
            }
        }
    }

    if (showNameDialog) {
        ourNode?.let { node ->
            EasyNameDialog(
                initialLongName = node.user.long_name,
                initialShortName = node.user.short_name,
                onSave = { longName, shortName ->
                    radioConfigViewModel.setOwner(node.user.copy(long_name = longName, short_name = shortName))
                    showNameDialog = false
                },
                onDismiss = { showNameDialog = false },
            )
        }
    }

    if (showAdvancedDialog) {
        MeshtasticDialog(
            titleRes = Res.string.easy_switch_to_advanced,
            messageRes = Res.string.easy_switch_to_advanced_message,
            confirmTextRes = Res.string.easy_switch_to_advanced,
            onConfirm = {
                showAdvancedDialog = false
                settingsViewModel.setEasyModeEnabled(false)
            },
            dismissTextRes = Res.string.cancel,
            onDismiss = { showAdvancedDialog = false },
        )
    }
}

/**
 * "Sending budget" in plain words: how much of the radio's hourly transmit airtime is used, measured against the
 * duty-cycle cap. Hidden until the radio has reported any telemetry.
 */
@Composable
private fun EasyAirtimeRow(airUtilTx: Float?) {
    if (airUtilTx == null || airUtilTx <= 0f) return
    val level = EasyAirtimeLevel.from(airUtilTx)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(Res.string.easy_airtime), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(level.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = level.color(),
            )
        }
        LinearProgressIndicator(
            progress = { airtimeBudgetFraction(airUtilTx) },
            color = level.color(),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

private fun ConnectionStatus.easyLabel() = when (this) {
    ConnectionStatus.CONNECTED -> Res.string.connected

    ConnectionStatus.CONNECTING,
    ConnectionStatus.RECONNECTING,
    -> Res.string.connecting

    ConnectionStatus.CONNECTED_SLEEPING -> Res.string.connected_sleeping

    ConnectionStatus.MUST_SET_REGION -> Res.string.must_set_region

    ConnectionStatus.NOT_CONNECTED -> Res.string.not_connected
}

@Composable
private fun EasyNameDialog(
    initialLongName: String,
    initialShortName: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var longName by remember { mutableStateOf(initialLongName) }
    var shortName by remember { mutableStateOf(initialShortName) }
    val valid = longName.isNotBlank() && shortName.isNotBlank()

    MeshtasticDialog(
        titleRes = Res.string.easy_your_name,
        text = {
            Column {
                OutlinedTextField(
                    value = longName,
                    onValueChange = { if (it.length <= LONG_NAME_MAX_LENGTH) longName = it },
                    label = { Text(stringResource(Res.string.long_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { if (it.length <= SHORT_NAME_MAX_LENGTH) shortName = it },
                    label = { Text(stringResource(Res.string.short_name)) },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmTextRes = Res.string.save,
        onConfirm = { if (valid) onSave(longName.trim(), shortName.trim()) },
        dismissTextRes = Res.string.cancel,
        onDismiss = onDismiss,
    )
}
