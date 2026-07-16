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
package org.meshtastic.feature.node.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.feature.node.easy.EasyPeopleScreen
import org.meshtastic.feature.node.easy.EasyPeopleViewModel
import org.meshtastic.feature.node.easy.EasyPersonDetailScreen

/**
 * Easy mode registrations for the node routes: the same [NodesRoute] keys the full app uses, rendered as the friendly
 * People list and person detail screens.
 */
fun EntryProviderScope<NavKey>.easyNodesGraph(
    backStack: NavBackStack<NavKey>,
    scrollToTopEvents: Flow<ScrollToTopEvent> = MutableSharedFlow(),
) {
    entry<NodesRoute.Nodes> {
        EasyPeopleScreen(
            onOpenPerson = { nodeNum -> backStack.add(NodesRoute.NodeDetail(nodeNum)) },
            viewModel = koinViewModel<EasyPeopleViewModel>(),
            scrollToTopEvents = scrollToTopEvents,
        )
    }

    entry<NodesRoute.NodeDetail> { args ->
        val nodeNum = args.destNum
        if (nodeNum == null) {
            // A detail route with no target (e.g. a malformed deep link) has nothing to show.
            backStack.removeLastOrNull()
        } else {
            EasyPersonDetailScreen(
                nodeNum = nodeNum,
                viewModel = koinViewModel<EasyPeopleViewModel>(),
                navigateToMessages = { contactKey -> backStack.add(ContactsRoute.Messages(contactKey)) },
                onNavigateBack = dropUnlessResumed { backStack.removeLastOrNull() },
            )
        }
    }
}
