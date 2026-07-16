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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.easy_no_people_yet
import org.meshtastic.core.resources.favorite
import org.meshtastic.core.resources.people
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.component.easy.EasyAvatar
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.formatAgo
import org.meshtastic.proto.Config.DisplayConfig.DisplayUnits

/**
 * Easy mode "People" tab: everyone on the mesh as a friendly contacts list — name, when they were last heard, and how
 * far away they are. All radio jargon lives only in the full app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyPeopleScreen(
    onOpenPerson: (Int) -> Unit,
    viewModel: EasyPeopleViewModel,
    modifier: Modifier = Modifier,
    scrollToTopEvents: Flow<ScrollToTopEvent> = MutableSharedFlow(),
) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    val ourNode by viewModel.ourNode.collectAsStateWithLifecycle()
    val displayUnits by viewModel.displayUnits.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { event ->
            if (event == ScrollToTopEvent.NodesTabPressed) {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.people)) }) },
    ) { padding ->
        if (people.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.easy_no_people_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), state = listState) {
                items(people, key = { it.num }) { person ->
                    EasyPersonRow(
                        person = person,
                        ourNode = ourNode,
                        displayUnits = displayUnits,
                        onClick = { onOpenPerson(person.num) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EasyPersonRow(
    person: Node,
    ourNode: Node?,
    displayUnits: DisplayUnits,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EasyAvatar(text = person.user.short_name, colors = person.colors)

        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = person.user.long_name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (person.isFavorite) {
                    Icon(
                        imageVector = MeshtasticIcons.Favorite,
                        contentDescription = stringResource(Res.string.favorite),
                        modifier = Modifier.padding(start = 6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = personSubtitle(person, ourNode, displayUnits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "5 min ago · 2.4 km" — the two facts a non-technical user actually cares about. */
@Composable
private fun personSubtitle(person: Node, ourNode: Node?, displayUnits: DisplayUnits): String {
    val lastSeen = formatAgo(person.lastHeard)
    val distance = ourNode?.let { person.distanceStr(it, displayUnits) }
    return if (distance != null) "$lastSeen · $distance" else lastSeen
}
