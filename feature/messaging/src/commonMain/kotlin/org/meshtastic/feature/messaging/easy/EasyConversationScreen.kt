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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.public_channel
import org.meshtastic.core.resources.unknown_channel
import org.meshtastic.core.ui.component.easy.EasyAvatar
import org.meshtastic.core.ui.component.easy.EasyAvatarIcon
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.Groups
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.createClipEntry
import org.meshtastic.feature.messaging.MessageInput
import org.meshtastic.feature.messaging.MessageListHandlers
import org.meshtastic.feature.messaging.MessageListPaged
import org.meshtastic.feature.messaging.MessageListPagedState
import org.meshtastic.feature.messaging.MessageViewModel
import org.meshtastic.feature.messaging.component.ActionModeTopBar
import org.meshtastic.feature.messaging.component.DeleteMessageDialog
import org.meshtastic.feature.messaging.component.MessageMenuAction
import org.meshtastic.feature.messaging.component.ReplySnippet
import org.meshtastic.feature.messaging.component.ScrollToBottomFab

/** Contact key of the primary broadcast channel. */
private const val PRIMARY_CHANNEL_CONTACT_KEY = "0^all"

/**
 * The Easy mode conversation screen: the same battle-tested message list and input the full app uses, under a plain
 * chat-app top bar (avatar + name + back) with none of the search/filter/quick-chat chrome.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun EasyConversationScreen(
    contactKey: String,
    viewModel: MessageViewModel,
    navigateToNodeDetails: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "",
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current
    val focusManager = LocalFocusManager.current

    val nodes by viewModel.nodeList.collectAsStateWithLifecycle()
    val ourNode by viewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val pagedMessages = viewModel.getMessagesFromPaged(contactKey).collectAsLazyPagingItems()
    val homoglyphEncodingEnabled by viewModel.homoglyphEncodingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val hasUnreadMessages by viewModel.hasUnreadMessages.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val firstUnreadMessageUuid by viewModel.firstUnreadMessageUuid.collectAsStateWithLifecycle()

    var replyingToPacketId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val selectedMessageIds = rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val messageInputState = rememberTextFieldState(message.ifEmpty { viewModel.draftMessage.value })
    val inSelectionMode by remember { derivedStateOf { selectedMessageIds.value.isNotEmpty() } }
    val listState = rememberLazyListState()

    LaunchedEffect(messageInputState) {
        snapshotFlow { messageInputState.text.toString() }.collect { text -> viewModel.setDraftMessage(text) }
    }
    LaunchedEffect(contactKey) { focusManager.clearFocus() }

    // Open at the first unread message when there is one, otherwise at the newest message.
    var hasPerformedInitialScroll by rememberSaveable(contactKey) { mutableStateOf(false) }
    LaunchedEffect(hasPerformedInitialScroll, pagedMessages.itemCount, hasUnreadMessages, firstUnreadMessageUuid) {
        if (hasPerformedInitialScroll || pagedMessages.itemCount == 0 || hasUnreadMessages == null) {
            return@LaunchedEffect
        }
        if (hasUnreadMessages == true) {
            val uuid = firstUnreadMessageUuid ?: return@LaunchedEffect
            val index = pagedMessages.itemSnapshotList.indexOfFirst { it?.uuid == uuid }.takeIf { it != -1 }
            if (index != null) {
                listState.scrollToItem(index)
                hasPerformedInitialScroll = true
            } else {
                // First unread is deeper than the loaded pages; loading more re-triggers this effect.
                listState.scrollToItem(pagedMessages.itemCount - 1)
            }
        } else {
            listState.scrollToItem(0)
            hasPerformedInitialScroll = true
        }
    }

    // Title, subtitle, and avatar mirror the chat list row so the conversation feels like the same place.
    val parsedKey = remember(contactKey) { ContactKey(contactKey) }
    val isBroadcast = parsedKey.addressString == NodeAddress.ID_BROADCAST
    val channelName = parsedKey.channelOrNull?.let { index -> channels.getChannel(index)?.name }
    val title =
        if (isBroadcast) {
            channelName ?: stringResource(Res.string.unknown_channel)
        } else {
            remember(parsedKey, viewModel) { viewModel.getUser(parsedKey.addressString).long_name }
        }

    if (showDeleteDialog) {
        DeleteMessageDialog(
            count = selectedMessageIds.value.size,
            onConfirm = {
                viewModel.deleteMessages(selectedMessageIds.value.toList())
                selectedMessageIds.value = emptySet()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val originalMessage by
        remember(replyingToPacketId, pagedMessages.itemCount) {
            derivedStateOf {
                replyingToPacketId?.let { id -> pagedMessages.itemSnapshotList.firstOrNull { it?.packetId == id } }
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (inSelectionMode) {
                ActionModeTopBar(
                    selectedCount = selectedMessageIds.value.size,
                    onAction = { action ->
                        when (action) {
                            MessageMenuAction.ClipboardCopy -> {
                                val copiedText =
                                    (0 until pagedMessages.itemCount)
                                        .mapNotNull { pagedMessages[it] }
                                        .filter { it.uuid in selectedMessageIds.value }
                                        .joinToString("\n") { it.displayedText(searching = false) }
                                coroutineScope.launch {
                                    clipboardManager.setClipEntry(createClipEntry(copiedText, copiedText))
                                }
                                selectedMessageIds.value = emptySet()
                            }

                            MessageMenuAction.Delete -> showDeleteDialog = true

                            MessageMenuAction.Dismiss -> selectedMessageIds.value = emptySet()

                            MessageMenuAction.SelectAll ->
                                selectedMessageIds.value =
                                    if (selectedMessageIds.value.size == pagedMessages.itemCount) {
                                        emptySet()
                                    } else {
                                        (0 until pagedMessages.itemCount).mapNotNull { pagedMessages[it]?.uuid }.toSet()
                                    }
                        }
                    },
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = MeshtasticIcons.ArrowBack,
                                contentDescription = stringResource(Res.string.navigate_back),
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isBroadcast) {
                                EasyAvatarIcon(
                                    imageVector = MeshtasticIcons.Groups,
                                    contentDescription = null,
                                    size = 36.dp,
                                )
                            } else {
                                val node = remember(parsedKey) { viewModel.getNode(parsedKey.addressString) }
                                EasyAvatar(text = node.user.short_name, colors = node.colors, size = 36.dp)
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (contactKey == PRIMARY_CHANNEL_CONTACT_KEY) {
                                    Text(
                                        text = stringResource(Res.string.public_channel),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                ReplySnippet(
                    originalMessage = originalMessage,
                    onClearReply = { replyingToPacketId = null },
                    ourNode = ourNode,
                )
                MessageInput(
                    isEnabled = connectionState is ConnectionState.Connected,
                    isHomoglyphEncodingEnabled = homoglyphEncodingEnabled,
                    textFieldState = messageInputState,
                    nodes = nodes,
                    onSendMessage = {
                        val messageText = messageInputState.text.toString().trim { it.isWhitespace() }
                        if (messageText.isNotEmpty()) {
                            viewModel.sendMessage(messageText, contactKey, replyingToPacketId)
                            replyingToPacketId = null
                            messageInputState.clearText()
                            viewModel.clearDraftMessage()
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues).focusable()) {
            MessageListPaged(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                state =
                MessageListPagedState(
                    nodes = nodes,
                    ourNode = ourNode,
                    messages = pagedMessages,
                    selectedIds = selectedMessageIds,
                    contactKey = contactKey,
                    firstUnreadMessageUuid = firstUnreadMessageUuid,
                    hasUnreadMessages = hasUnreadMessages == true,
                ),
                handlers =
                MessageListHandlers(
                    onUnreadChanged = { messageUuid, timestamp ->
                        viewModel.clearUnreadCount(contactKey, messageUuid, timestamp)
                    },
                    onSendReaction = { emoji, id -> viewModel.sendReaction(emoji, id, contactKey) },
                    onClickChip = { node -> navigateToNodeDetails(node.num) },
                    onDeleteMessages = { viewModel.deleteMessages(it) },
                    onSendMessage = { text, key -> viewModel.sendMessage(text, key) },
                    onReply = { message -> replyingToPacketId = message?.packetId },
                ),
                quickEmojis = viewModel.frequentEmojis,
            )
            if (listState.canScrollBackward) {
                ScrollToBottomFab(coroutineScope, listState, unreadCount)
            }
        }
    }
}
