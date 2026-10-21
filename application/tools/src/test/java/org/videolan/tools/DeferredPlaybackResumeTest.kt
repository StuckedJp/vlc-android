package org.videolan.tools

import org.junit.Assert.*
import org.junit.Test

class DeferredPlaybackResumeTest {
    @Test fun playingAloneDoesNotAllowAnEarlyTsSeek() {
        val resume = DeferredPlaybackResume()
        resume.begin(142168)
        assertNull(resume.takeReady(true, 1440000, 0))
        resume.buffering(50f)
        assertNull(resume.takeReady(true, 1440000, 0))
        resume.buffering(100f)
        assertEquals(142168L, resume.takeReady(true, 1440000, 0))
    }

    @Test fun readinessCanArriveInAnyOrder() {
        val resume = DeferredPlaybackResume()
        resume.begin(75000)
        resume.buffering(100f)
        assertNull(resume.takeReady(false, 1440000, 0))
        assertNull(resume.takeReady(true, 0, 0))
        assertEquals(75000L, resume.takeReady(true, 1440000, 0))
    }

    @Test fun submissionIsOnceAndArrivalMustBeConfirmed() {
        val resume = DeferredPlaybackResume()
        resume.begin(142168)
        resume.buffering(100f)
        assertFalse(resume.observe(142168))
        assertEquals(142168L, resume.takeReady(true, 1440000, 0))
        assertNull(resume.takeReady(true, 1440000, 0))
        assertFalse(resume.observe(0))
        assertTrue(resume.active)
        assertTrue(resume.observe(142000))
        assertFalse(resume.active)
    }

    @Test fun stopOrExplicitSeekCancelsPendingResume() {
        val resume = DeferredPlaybackResume()
        resume.begin(75000)
        resume.cancel()
        resume.buffering(100f)
        assertNull(resume.takeReady(true, 1440000, 0))
        assertFalse(resume.active)
    }

    @Test fun aNewInputResetsAllReadinessState() {
        val resume = DeferredPlaybackResume()
        resume.begin(75000)
        resume.buffering(100f)
        resume.begin(120000)
        assertNull(resume.takeReady(true, 1440000, 1000))
        resume.buffering(100f)
        assertEquals(120000L, resume.takeReady(true, 1440000, 1000))
    }

    @Test fun freshProcessStartsWithoutAResumeRequest() {
        val resume = DeferredPlaybackResume()
        resume.begin(0)
        resume.buffering(100f)
        assertNull(resume.takeReady(true, 1440000, 0))
        assertFalse(resume.active)
    }
}
