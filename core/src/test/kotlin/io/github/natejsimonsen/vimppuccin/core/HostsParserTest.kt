package io.github.natejsimonsen.vimppuccin.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostsParserTest {
    @Test
    fun parsesHostsFileFormats() {
        val text = """
            # StevenBlack-style header
            127.0.0.1 localhost
            0.0.0.0 0.0.0.0
            ::1 ip6-localhost
            0.0.0.0 ads.example.com
            127.0.0.1	tracker.example.net  # trailing comment
            0.0.0.0 a.example.org b.example.org
            Plain.Example.IO.
            ||adblock.example.com^
            ||important.example.com^${'$'}important
            ||path.example.com/banner.js
            ! adblock comment
            [Adblock Plus 2.0]
            example.com##.ad-banner
            singlelabel
            1.2.3.4
            bad..domain.com
        """.trimIndent()

        assertEquals(
            setOf(
                "ads.example.com",
                "tracker.example.net",
                "a.example.org",
                "b.example.org",
                "plain.example.io",
                "adblock.example.com",
                "important.example.com",
            ),
            HostsParser.parse(text),
        )
    }

    @Test
    fun normalizeRejectsJunk() {
        assertNull(HostsParser.normalize("localhost"))
        assertNull(HostsParser.normalize("has space.com"))
        assertNull(HostsParser.normalize(".leading.com"))
        assertEquals("ok.example.com", HostsParser.normalize("OK.Example.com."))
    }
}
