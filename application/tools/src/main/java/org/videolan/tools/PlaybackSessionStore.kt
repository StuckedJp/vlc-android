/*
 * Copyright © 2026 VLC authors and VideoLAN
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package org.videolan.tools

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Resume points belong to one process, independently of service/activity lifetimes. */
class PlaybackSessionStore {
    data class Position(val time: Long = 0, val title: Int = 0)

    val id: String = UUID.randomUUID().toString()
    private val positions = ConcurrentHashMap<String, Position>()

    fun get(location: String): Position = positions[location] ?: Position()

    fun remember(location: String, time: Long, length: Long = 0, title: Int = 0, completed: Boolean = false) {
        val restart = completed || (length > 0 && time >= length - 5000)
        positions[location] = if (restart) Position() else Position(time.coerceAtLeast(0), title.coerceAtLeast(0))
    }

    fun restoreTime(sessionId: String?, time: Long): Long =
        if (sessionId == id) time.coerceAtLeast(0) else 0

    companion object {
        val current = PlaybackSessionStore()
    }
}
