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
package org.meshtastic.feature.node.easy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.util.DistanceUnit
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.battery
import org.meshtastic.core.resources.distance
import org.meshtastic.core.resources.favorite
import org.meshtastic.core.resources.last_seen
import org.meshtastic.core.resources.message
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.signal
import org.meshtastic.core.ui.component.MaterialBatteryInfo
import org.meshtastic.core.ui.component.determineSignalQuality
import org.meshtastic.core.ui.component.easy.EasyAvatar
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NotFavorite
import org.meshtastic.core.ui.icon.Send
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.core.ui.util.formatAgo

private const val SNR_UNKNOWN_SENTINEL = 100f

/**
 * Easy mode person detail: a big friendly card with the handful of facts a non-technical user cares about — last seen,
 * distance, battery, and a plain-words signal rating — plus Message and favorite actions. No IDs, keys, or telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun EasyPersonDetailScreen(
    nodeNum: Int,
    viewModel: EasyPeopleViewModel,
    navigateToMessages: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val personFlow = remember(nodeNum, viewModel) { viewModel.person(nodeNum) }
    val person by personFlow.collectAsStateWithLifecycle()
    val ourNode by viewModel.ourNode.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = MeshtasticIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
                title = {},
                actions = {
                    person?.let { node ->
                        IconButton(onClick = { viewModel.toggleFavorite(node) }) {
                            Icon(
                                imageVector =
                                if (node.isFavorite) MeshtasticIcons.Favorite else MeshtasticIcons.NotFavorite,
                                contentDescription = stringResource(Res.string.favorite),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val node = person ?: return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EasyAvatar(text = node.user.short_name, colors = node.colors, size = 96.dp)
            Text(
                text = node.user.long_name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (node.user.is_unmessagable != true) {
                Button(
                    onClick = { navigateToMessages(viewModel.directMessageContactKey(node)) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Icon(imageVector = MeshtasticIcons.Send, contentDescription = null)
                    Text(text = stringResource(Res.string.message), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.last_seen)) },
                    supportingContent = { Text(formatAgo(node.lastHeard)) },
                )

                val distance = ourNode?.let { node.distanceStr(it, DistanceUnit.getFromLocale()) }
                if (distance != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.distance)) },
                        supportingContent = { Text(distance) },
                    )
                }

                if (node.batteryLevel != null && node.batteryStr.isNotEmpty()) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.battery)) },
                        supportingContent = { MaterialBatteryInfo(level = node.batteryLevel, voltage = node.voltage) },
                    )
                }

                SignalRow(node)
            }
        }
    }
}

/**
 * Plain-words signal rating, shown only for direct radio contact — a relayed or internet-bridged node's radio numbers
 * would be misleading.
 */
@Composable
private fun SignalRow(node: Node) {
    val hasDirectSignal = node.hopsAway == 0 && !node.viaMqtt && node.snr < SNR_UNKNOWN_SENTINEL
    if (!hasDirectSignal) return
    val quality = determineSignalQuality(node.snr, LocalModemPreset.current)
    ListItem(
        headlineContent = { Text(stringResource(Res.string.signal)) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vectorResource(quality.icon),
                    contentDescription = null,
                    tint = quality.color(),
                    modifier = Modifier.size(18.dp),
                )
                Text(text = stringResource(quality.nameRes), modifier = Modifier.padding(start = 6.dp))
            }
        },
    )
}
