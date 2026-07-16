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

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.easy_airtime_almost
import org.meshtastic.core.resources.easy_airtime_exhausted
import org.meshtastic.core.resources.easy_airtime_low
import org.meshtastic.core.resources.easy_airtime_plenty
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.theme.StatusColors.StatusOrange
import org.meshtastic.core.ui.theme.StatusColors.StatusRed
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow

/**
 * Hourly TX airtime budget (percent) Easy mode measures against: the EU 868 duty-cycle cap, which is also Meshtastic's
 * general keep-the-mesh-healthy guidance for every region.
 */
const val AIRTIME_BUDGET_PERCENT = 10f

/** Above this TX airtime (percent), Easy mode starts warning that sending may pause soon. */
const val AIRTIME_WARN_PERCENT = 8f

private const val AIRTIME_LOW_PERCENT = 5f

/** Channel utilization (percent) above which the shared airwaves count as congested (firmware throttles ~25%). */
const val MESH_BUSY_CHANNEL_UTIL_PERCENT = 25f

/** Plain-words bucketing of the radio's hourly TX airtime against [AIRTIME_BUDGET_PERCENT]. */
@Stable
enum class EasyAirtimeLevel(@Stable val labelRes: StringResource, @Stable val color: @Composable () -> Color) {
    PLENTY(Res.string.easy_airtime_plenty, { colorScheme.StatusGreen }),
    LOW(Res.string.easy_airtime_low, { colorScheme.StatusYellow }),
    ALMOST(Res.string.easy_airtime_almost, { colorScheme.StatusOrange }),
    EXHAUSTED(Res.string.easy_airtime_exhausted, { colorScheme.StatusRed }),
    ;

    companion object {
        fun from(airUtilTx: Float): EasyAirtimeLevel = when {
            airUtilTx >= AIRTIME_BUDGET_PERCENT -> EXHAUSTED
            airUtilTx >= AIRTIME_WARN_PERCENT -> ALMOST
            airUtilTx >= AIRTIME_LOW_PERCENT -> LOW
            else -> PLENTY
        }
    }
}

/** Fraction of the airtime budget already used, for progress displays. */
fun airtimeBudgetFraction(airUtilTx: Float): Float = (airUtilTx / AIRTIME_BUDGET_PERCENT).coerceIn(0f, 1f)
