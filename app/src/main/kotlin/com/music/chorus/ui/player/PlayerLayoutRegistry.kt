package pushkar.chorus.music.ui.player

import androidx.annotation.StringRes
import pushkar.chorus.music.R

enum class PlayerSlot(@StringRes val labelRes: Int, val hideable: Boolean) {
    ALBUM_ART(R.string.player_slot_album_art, hideable = true),
    TRACK_INFO(R.string.player_slot_track_info, hideable = true),
    SEEK_BAR(R.string.player_slot_seek_bar, hideable = true),
    CONTROLS(R.string.player_slot_controls, hideable = false),
    ACTION_ROW(R.string.player_slot_action_row, hideable = true),
}

object PlayerLayoutRegistry {
    fun getDefaultOrder(): List<PlayerSlot> = listOf(
        PlayerSlot.ALBUM_ART,
        PlayerSlot.TRACK_INFO,
        PlayerSlot.SEEK_BAR,
        PlayerSlot.CONTROLS,
        PlayerSlot.ACTION_ROW,
    )

    fun deserializeOrder(orderString: String): List<PlayerSlot> {
        if (orderString.isBlank()) return getDefaultOrder()
        val parsed = orderString.split(",").mapNotNull { name ->
            runCatching { PlayerSlot.valueOf(name.trim()) }.getOrNull()
        }
        val missing = PlayerSlot.entries.filter { it !in parsed }
        return parsed + missing
    }

    fun serializeOrder(slots: List<PlayerSlot>): String = slots.joinToString(",") { it.name }

    fun deserializeHiddenSlots(hiddenString: String): Set<PlayerSlot> {
        if (hiddenString.isBlank()) return emptySet()
        return hiddenString.split(",")
            .mapNotNull { name -> runCatching { PlayerSlot.valueOf(name.trim()) }.getOrNull() }
            .filter { it.hideable }
            .toSet()
    }

    fun serializeHiddenSlots(hidden: Set<PlayerSlot>): String =
        hidden.filter { it.hideable }.joinToString(",") { it.name }
}
