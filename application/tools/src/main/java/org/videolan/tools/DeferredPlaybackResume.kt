/*
 * Copyright © 2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License, version 2 or later.
 */
package org.videolan.tools

import kotlin.math.absoluteValue

/** One resume request per input, retained until the engine reports arrival. */
class DeferredPlaybackResume {
    private var target: Long? = null
    private var submitted = false
    private var buffered = false
    val active: Boolean get() = target != null

    fun begin(time: Long) {
        cancel()
        target = time.takeIf { it > 0 }
    }

    fun cancel() {
        target = null
        submitted = false
        buffered = false
    }

    fun buffering(percent: Float) {
        buffered = percent >= 100f
    }

    fun takeReady(seekable: Boolean, length: Long, observedTime: Long): Long? {
        val time = target ?: return null
        if (submitted || !buffered || !seekable || (length <= 0 && observedTime <= 0)) return null
        submitted = true
        return time
    }

    fun observe(time: Long): Boolean {
        val expected = target ?: return false
        if (!submitted || (time - expected).absoluteValue > 2000) return false
        cancel()
        return true
    }
}
