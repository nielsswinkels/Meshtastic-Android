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
package org.meshtastic.feature.messaging.easy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.Contact
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.chats
import org.meshtastic.core.resources.mute
import org.meshtastic.core.resources.new_chat
import org.meshtastic.core.resources.no_messages_yet
import org.meshtastic.core.resources.pin
import org.meshtastic.core.resources.public_channel
import org.meshtastic.core.resources.unmute
import org.meshtastic.core.resources.unpin
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.component.easy.EasyAvatar
import org.meshtastic.core.ui.component.easy.EasyAvatarIcon
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.Groups
import org.meshtastic.core.ui.icon.KeepPin
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.VolumeOff
import org.meshtastic.feature.messaging.ui.contact.ContactSection
import org.meshtastic.feature.messaging.ui.contact.ContactsViewModel
import org.meshtastic.feature.messaging.ui.contact.section

/** Contact key of the primary broadcast channel — the "public channel" every device ships with (LongFast). */
private const val PRIMARY_CHANNEL_CONTACT_KEY = "0^all"

private const val UNREAD_LIMIT = 99

/**
 * Signal-style chat list for Easy mode: one flat list of conversations (channels and direct messages together), pinned
 * chats first, then most recently active. Long-press pins or mutes; the FAB starts a new direct message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyChatsScreen(
    onNavigateToMessages: (String) -> Unit,
    viewModel: ContactsViewModel,
    easyViewModel: EasyChatsViewModel,
    modifier: Modifier = Modifier,
    scrollToTopEvents: Flow<ScrollToTopEvent> = MutableSharedFlow(),
) {
    val contacts by viewModel.contactList.collectAsStateWithLifecycle()
    val pinnedKeys by easyViewModel.pinnedContactKeys.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showNewChatSheet by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { event ->
            if (event == ScrollToTopEvent.ConversationsTabPressed) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val sortedContacts =
        remember(contacts, pinnedKeys) {
            contacts.sortedWith(
                compareByDescending<Contact> { it.contactKey in pinnedKeys }
                    .thenByDescending { it.lastMessageTime ?: Long.MIN_VALUE },
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.chats)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatSheet = true }) {
                Icon(imageVector = MeshtasticIcons.Add, contentDescription = stringResource(Res.string.new_chat))
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), state = listState) {
            items(sortedContacts, key = { it.contactKey }) { contact ->
                EasyChatRow(
                    contact = contact,
                    isPinned = contact.contactKey in pinnedKeys,
                    onClick = { onNavigateToMessages(contact.contactKey) },
                    onTogglePin = {
                        easyViewModel.setContactPinned(contact.contactKey, contact.contactKey !in pinnedKeys)
                    },
                    onToggleMute = {
                        val newMuteUntil = if (contact.isMuted) 0L else Long.MAX_VALUE
                        viewModel.setMuteUntil(listOf(contact.contactKey), newMuteUntil)
                    },
                )
            }
        }
    }

    if (showNewChatSheet) {
        val people by easyViewModel.messageableNodes.collectAsStateWithLifecycle()
        EasyNewChatSheet(
            people = people,
            onDismiss = { showNewChatSheet = false },
            onSelectPerson = { node ->
                showNewChatSheet = false
                onNavigateToMessages(easyViewModel.directMessageContactKey(node))
            },
        )
    }
}

/** People picker behind the "new chat" FAB: tap a person to open (or start) a direct message with them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EasyNewChatSheet(people: List<Node>, onDismiss: () -> Unit, onSelectPerson: (Node) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(Res.string.new_chat),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn {
            items(people, key = { it.num }) { node ->
                Row(
                    modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onSelectPerson(node) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EasyAvatar(text = node.user.short_name, colors = node.colors)
                    Text(
                        text = node.user.long_name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EasyChatRow(
    contact: Contact,
    isPinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isChannel = contact.section() == ContactSection.CHANNELS
    val isPublicChannel = contact.contactKey == PRIMARY_CHANNEL_CONTACT_KEY

    Box(modifier = modifier) {
        Row(
            modifier =
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isChannel) {
                EasyAvatarIcon(imageVector = MeshtasticIcons.Groups, contentDescription = null)
            } else {
                EasyAvatar(text = contact.shortName, colors = contact.nodeColors)
            }

            EasyChatRowTitle(
                contact = contact,
                isPublicChannel = isPublicChannel,
                modifier = Modifier.weight(1f).padding(start = 16.dp),
            )

            EasyChatRowMeta(contact = contact, isPinned = isPinned, modifier = Modifier.padding(start = 8.dp))
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (isPinned) Res.string.unpin else Res.string.pin)) },
                onClick = {
                    showMenu = false
                    onTogglePin()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(if (contact.isMuted) Res.string.unmute else Res.string.mute)) },
                onClick = {
                    showMenu = false
                    onToggleMute()
                },
            )
        }
    }
}

/** Name (with the public-channel marker) over the last-message preview. */
@Composable
private fun EasyChatRowTitle(contact: Contact, isPublicChannel: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = contact.longName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isPublicChannel) {
                Text(
                    text = stringResource(Res.string.public_channel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            text = contact.lastMessageText ?: stringResource(Res.string.no_messages_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Trailing column of a chat row: relative time on top, then pin/mute markers and the unread badge. */
@Composable
private fun EasyChatRowMeta(contact: Contact, isPinned: Boolean, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        Text(
            text = contact.lastMessageTime?.let { DateFormatter.formatShortDate(it) }.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (isPinned) {
                Icon(
                    imageVector = MeshtasticIcons.KeepPin,
                    contentDescription = stringResource(Res.string.pin),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contact.isMuted) {
                Icon(
                    imageVector = MeshtasticIcons.VolumeOff,
                    contentDescription = stringResource(Res.string.mute),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contact.unreadCount > 0) {
                val unreadText =
                    if (contact.unreadCount > UNREAD_LIMIT) "$UNREAD_LIMIT+" else contact.unreadCount.toString()
                Text(
                    text = unreadText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier =
                    Modifier.background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .defaultMinSize(minWidth = 20.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
