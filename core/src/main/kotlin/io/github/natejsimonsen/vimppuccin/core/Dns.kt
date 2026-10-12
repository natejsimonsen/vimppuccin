package io.github.natejsimonsen.vimppuccin.core

import io.github.natejsimonsen.vimppuccin.core.Ipv4Udp.put16
import io.github.natejsimonsen.vimppuccin.core.Ipv4Udp.u16
import io.github.natejsimonsen.vimppuccin.core.Ipv4Udp.u8

/** The first question of a DNS query. [end] is the offset just past QTYPE/QCLASS. */
data class DnsQuestion(val name: String, val type: Int, val end: Int)

object Dns {
    private const val HEADER = 12
    const val RCODE_NXDOMAIN = 3

    /** Parses the first question of a standard DNS query, or returns null if [msg] isn't one. */
    fun parseQuestion(msg: ByteArray): DnsQuestion? {
        if (msg.size < HEADER) return null
        val flags = u16(msg, 2)
        if (flags and 0x8000 != 0) return null // a response, not a query
        if ((flags shr 11) and 0x0F != 0) return null // not a standard query
        if (u16(msg, 4) < 1) return null

        val name = StringBuilder()
        var i = HEADER
        while (true) {
            if (i >= msg.size) return null
            val labelLength = u8(msg, i)
            if (labelLength == 0) {
                i++
                break
            }
            // Compression pointers / extended labels never appear in a query's first name.
            if (labelLength and 0xC0 != 0) return null
            if (i + 1 + labelLength > msg.size) return null
            if (name.isNotEmpty()) name.append('.')
            for (j in i + 1..i + labelLength) name.append((msg[j].toInt() and 0xFF).toChar())
            i += 1 + labelLength
            if (name.length > 253) return null
        }
        if (i + 4 > msg.size) return null
        return DnsQuestion(name.toString().lowercase(), u16(msg, i), i + 4)
    }

    /**
     * Builds an NXDOMAIN response echoing [question]. Any additional records
     * in the query (e.g. EDNS OPT) are dropped.
     */
    fun blockedResponse(query: ByteArray, question: DnsQuestion): ByteArray {
        val r = query.copyOfRange(0, question.end)
        val flags = u16(query, 2)
        // QR=1, keep opcode and RD, RA=1, AA/TC/Z cleared, RCODE=NXDOMAIN.
        val newFlags = 0x8000 or (flags and 0x7900) or 0x0080 or RCODE_NXDOMAIN
        put16(r, 2, newFlags)
        put16(r, 4, 1)
        put16(r, 6, 0)
        put16(r, 8, 0)
        put16(r, 10, 0)
        return r
    }

    fun rcode(msg: ByteArray): Int = u16(msg, 2) and 0x0F
}
