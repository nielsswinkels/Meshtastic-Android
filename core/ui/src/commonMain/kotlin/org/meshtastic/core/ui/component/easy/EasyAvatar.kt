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
package org.meshtastic.core.ui.component.easy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default diameter of an Easy mode avatar, sized to match familiar chat-app contact lists. */
val EasyAvatarSize: Dp = 44.dp

/**
 * Circular chat-app style avatar showing a node's short name on its deterministic node color. [colors] is the (text,
 * background) ARGB pair from `Node.colors` / `Contact.nodeColors`; falls back to the theme's primary container when
 * unknown.
 */
@Composable
fun EasyAvatar(text: String, modifier: Modifier = Modifier, colors: Pair<Int, Int>? = null, size: Dp = EasyAvatarSize) {
    val background = colors?.second?.let(::Color) ?: MaterialTheme.colorScheme.primaryContainer
    val foreground = colors?.first?.let(::Color) ?: MaterialTheme.colorScheme.onPrimaryContainer
    Box(modifier = modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(
            text = text.ifBlank { "?" }.take(MAX_AVATAR_CHARS),
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** Circular avatar variant showing an icon — used for channel (group) chats in the Easy mode chat list. */
@Composable
fun EasyAvatarIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = EasyAvatarSize,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private const val MAX_AVATAR_CHARS = 4
