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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed

/**
 * Easy mode extras for the chat list that [org.meshtastic.feature.messaging.ui.contact.ContactsViewModel] doesn't
 * cover: pinned chats and the people picker behind the "new chat" button.
 */
@KoinViewModel
class EasyChatsViewModel(private val uiPrefs: UiPrefs, nodeRepository: NodeRepository) : ViewModel() {

    val pinnedContactKeys: StateFlow<Set<String>> = uiPrefs.pinnedContactKeys

    fun setContactPinned(contactKey: String, pinned: Boolean) {
        uiPrefs.setContactPinned(contactKey, pinned)
    }

    private val ourNode = nodeRepository.ourNodeInfo

    /** People the user can start a direct message with: known, messageable, and not ourselves. */
    val messageableNodes: StateFlow<List<Node>> =
        combine(nodeRepository.getNodes(includeUnknown = false), ourNode) { nodes, us ->
            // distinctBy: the upstream flow can momentarily emit duplicate nums, which crashes keyed lazy lists.
            nodes.distinctBy { it.num }.filter { node -> node.num != us?.num && node.user.is_unmessagable != true }
        }
            .stateInWhileSubscribed(initialValue = emptyList())

    /**
     * Builds the conversation contact key for a direct message to [node], preferring the private PKC channel when both
     * sides support it — the same rule the full app's node screens apply.
     */
    fun directMessageContactKey(node: Node): String {
        val hasMutualPkc = ourNode.value?.hasPKC == true && node.hasPKC
        val channel = if (hasMutualPkc) NodeAddress.PKC_CHANNEL_INDEX else node.channel
        return "$channel${node.user.id}"
    }
}
