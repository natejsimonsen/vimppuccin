package io.github.natejsimonsen.vimppuccin.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlocklistTest {
    private val list = Blocklist(
        blocked = setOf("doubleclick.net", "ads.example.com", "tracker.cdn.example.org"),
        allowed = setOf("ok.doubleclick.net"),
    )

    @Test
    fun blocksExactAndSubdomains() {
        assertTrue(list.isBlocked("doubleclick.net"))
        assertTrue(list.isBlocked("stats.g.doubleclick.net"))
        assertTrue(list.isBlocked("ADS.Example.com."))
        assertTrue(list.isBlocked("x.tracker.cdn.example.org"))
    }

    @Test
    fun doesNotBlockParentsOrLookalikes() {
        assertFalse(list.isBlocked("example.com"))
        assertFalse(list.isBlocked("www.example.com"))
        assertFalse(list.isBlocked("notdoubleclick.net"))
        assertFalse(list.isBlocked("cdn.example.org"))
        assertFalse(list.isBlocked(""))
    }

    @Test
    fun allowlistWins() {
        assertFalse(list.isBlocked("ok.doubleclick.net"))
        assertFalse(list.isBlocked("deep.ok.doubleclick.net"))
        assertTrue(list.isBlocked("bad.doubleclick.net"))
    }
}
