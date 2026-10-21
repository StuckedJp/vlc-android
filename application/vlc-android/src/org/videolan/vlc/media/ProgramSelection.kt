/*
 * Copyright © 2026 VLC authors and VideoLAN
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package org.videolan.vlc.media

import org.videolan.libvlc.MediaPlayer

/** A TS service, distinct from the audio/video elementary streams. */
data class ProgramInfo(val id: Int, val name: String)

/** Keep published LibVLC and VLC 4 builds usable without our local VLC 3 API. */
internal object ProgramSelection {
    const val CHANGED = 0x11f
    private val getPrograms = runCatching { MediaPlayer::class.java.getMethod("getPrograms") }.getOrNull()
    private val getProgram = runCatching { MediaPlayer::class.java.getMethod("getProgram") }.getOrNull()
    private val setProgram = runCatching { MediaPlayer::class.java.getMethod("setProgram", Int::class.javaPrimitiveType) }.getOrNull()

    fun programs(player: MediaPlayer): List<ProgramInfo> = runCatching {
        val entries = getPrograms?.invoke(player) as? Array<*> ?: return emptyList()
        entries.filterNotNull().map { entry ->
            ProgramInfo(entry.javaClass.getField("id").getInt(entry),
                entry.javaClass.getField("name").get(entry) as? String ?: "")
        }.filter { it.id > 0 }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    fun selected(player: MediaPlayer): Int = runCatching {
        (getProgram?.invoke(player) as? Number)?.toInt() ?: -1
    }.getOrDefault(-1)

    fun select(player: MediaPlayer, id: Int): Boolean = runCatching {
        setProgram?.invoke(player, id) as? Boolean ?: false
    }.getOrDefault(false)
}
