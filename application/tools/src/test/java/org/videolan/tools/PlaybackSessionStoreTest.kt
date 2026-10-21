package org.videolan.tools

import org.junit.Assert.*
import org.junit.Test

class PlaybackSessionStoreTest {
    @Test fun positionsSurviveConsumerRecreationWithinOneSession() {
        val session = PlaybackSessionStore()
        session.remember("upnp://recording/a", 120000, 600000, 2)
        assertEquals(PlaybackSessionStore.Position(120000, 2), session.get("upnp://recording/a"))
        assertEquals(0L, session.get("upnp://recording/b").time)
    }

    @Test fun aNewProcessCannotResumeAnyPreviousContent() {
        val previous = PlaybackSessionStore()
        previous.remember("file:///video", 120000)
        previous.remember("http://server/recording", 300000)
        val next = PlaybackSessionStore()
        assertEquals(0L, next.get("file:///video").time)
        assertEquals(0L, next.get("http://server/recording").time)
        assertNotEquals(previous.id, next.id)
        assertEquals(0L, next.restoreTime(previous.id, 120000))
    }

    @Test fun rotationRestoresOnlyTheCurrentProcessState() {
        val session = PlaybackSessionStore()
        assertEquals(75000L, session.restoreTime(session.id, 75000))
        assertEquals(0L, session.restoreTime(null, 75000))
        assertEquals(0L, session.restoreTime("old-process", 75000))
    }

    @Test fun completionAndNearEndRestartFromTheBeginning() {
        val session = PlaybackSessionStore()
        session.remember("video", 40000, 60000, 3)
        session.remember("video", 60000, 60000, 3, true)
        assertEquals(PlaybackSessionStore.Position(), session.get("video"))
        session.remember("video", 58000, 60000, 3)
        assertEquals(PlaybackSessionStore.Position(), session.get("video"))
    }

    @Test fun unknownLengthAndInvalidClockAreHandled() {
        val session = PlaybackSessionStore()
        session.remember("stream", 120000)
        assertEquals(120000L, session.get("stream").time)
        session.remember("stream", -1, title = -1)
        assertEquals(PlaybackSessionStore.Position(), session.get("stream"))
    }
}
