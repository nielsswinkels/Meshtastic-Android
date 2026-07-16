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
package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.easy_mode
import org.meshtastic.core.resources.easy_mode_description
import org.meshtastic.core.resources.easy_mode_enable_message
import org.meshtastic.core.resources.easy_mode_enable_title
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.SwitchListItem

/**
 * Full-app settings section for switching into Easy mode: a simplified shell (chats, people, map, minimal settings) for
 * non-technical users. Enabling asks for confirmation because the whole UI changes on the spot; the way back is at the
 * bottom of Easy mode's own settings.
 */
@Composable
fun EasyModeSection(easyModeEnabled: Boolean, onToggleEasyMode: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    ExpressiveSection(title = stringResource(Res.string.easy_mode), modifier = modifier) {
        SwitchListItem(
            checked = easyModeEnabled,
            text = stringResource(Res.string.easy_mode),
            onClick = {
                if (easyModeEnabled) {
                    onToggleEasyMode(false)
                } else {
                    showConfirmDialog = true
                }
            },
        )
        Text(
            text = stringResource(Res.string.easy_mode_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    if (showConfirmDialog) {
        MeshtasticDialog(
            titleRes = Res.string.easy_mode_enable_title,
            messageRes = Res.string.easy_mode_enable_message,
            confirmTextRes = Res.string.okay,
            onConfirm = {
                showConfirmDialog = false
                onToggleEasyMode(true)
            },
            dismissTextRes = Res.string.cancel,
            onDismiss = { showConfirmDialog = false },
        )
    }
}
