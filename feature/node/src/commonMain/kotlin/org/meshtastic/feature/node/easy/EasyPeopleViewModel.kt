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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.NodeSortOption
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.node.detail.NodeManagementActions
import org.meshtastic.feature.node.domain.usecase.GetFilteredNodesUseCase
import org.meshtastic.feature.node.list.NodeFilterState

/**
 * Backs both Easy mode People screens (list + person detail): a friendly, always-recency-sorted view of the node
 * database with favorites first and all technical filtering fixed to sensible defaults.
 */
@KoinViewModel
class EasyPeopleViewModel(
    private val nodeRepository: NodeRepository,
    getFilteredNodesUseCase: GetFilteredNodesUseCase,
    connectionStateProvider: ConnectionStateProvider,
    private val nodeManagementActions: NodeManagementActions,
) : ViewModel() {

    val ourNode: StateFlow<Node?> = nodeRepository.ourNodeInfo

    val connectionState = connectionStateProvider.connectionState

    /** Everyone else on the mesh: favorites first, then most recently heard. Our own node is not a "person" here. */
    val people: StateFlow<List<Node>> =
        combine(getFilteredNodesUseCase(filter = NodeFilterState(), sort = NodeSortOption.LAST_HEARD), ourNode) {
                nodes,
                us,
            ->
            // distinctBy: the upstream flow can momentarily emit duplicate nums, which crashes keyed lazy lists.
            nodes
                .distinctBy { it.num }
                .filter { it.num != us?.num }
                .sortedWith(compareByDescending<Node> { it.isFavorite }.thenByDescending { it.lastHeard })
        }
            .stateInWhileSubscribed(initialValue = emptyList())

    /** The node for the person detail screen, kept live as new telemetry arrives. */
    fun person(nodeNum: Int): StateFlow<Node?> =
        nodeRepository.nodeDBbyNum.map { it[nodeNum] }.stateInWhileSubscribed(initialValue = null)

    fun toggleFavorite(node: Node) {
        nodeManagementActions.requestFavoriteNode(viewModelScope, node)
    }

    /** PKC-aware direct-message contact key, mirroring the full app's node screens. */
    fun directMessageContactKey(node: Node): String {
        val hasMutualPkc = ourNode.value?.hasPKC == true && node.hasPKC
        val channel = if (hasMutualPkc) NodeAddress.PKC_CHANNEL_INDEX else node.channel
        return "$channel${node.user.id}"
    }
}
