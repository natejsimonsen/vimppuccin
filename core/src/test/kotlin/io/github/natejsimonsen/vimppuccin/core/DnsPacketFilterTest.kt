package io.github.natejsimonsen.vimppuccin.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DnsPacketFilterTest {
    private val filter = DnsPacketFilter(Blocklist(setOf("doubleclick.net")))

    @Test
    fun parsesQuestion() {
        val q = assertNotNull(Dns.parseQuestion(TestPackets.dnsQuery("WWW.Example.com", type = 28)))
        assertEquals("www.example.com", q.name)
        assertEquals(28, q.type)
    }

    @Test
    fun rejectsResponsesAndTruncatedMessages() {
        val query = TestPackets.dnsQuery("example.com")
        val response = query.copyOf().also { it[2] = (it[2].toInt() or 0x80).toByte() }
        assertNull(Dns.parseQuestion(response))
        assertNull(Dns.parseQuestion(query.copyOf(query.size - 3)))
        assertNull(Dns.parseQuestion(ByteArray(5)))
    }

    @Test
    fun blockedQueryGetsNxdomainReply() {
        val packet = TestPackets.udpQueryPacket("ads.g.doubleclick.net")
        val verdict = assertIs<DnsPacketFilter.Verdict.Block>(filter.handle(packet, packet.size))
        assertEquals("ads.g.doubleclick.net", verdict.domain)

        val reply = assertNotNull(Ipv4Udp.parse(verdict.response, verdict.response.size))
        assertContentEquals(TestPackets.DNS_SERVER, reply.srcAddress)
        assertContentEquals(TestPackets.CLIENT, reply.dstAddress)
        assertEquals(53, reply.srcPort)
        assertEquals(40000, reply.dstPort)

        val dns = reply.payload
        assertEquals(0x1234, ((dns[0].toInt() and 0xFF) shl 8) or (dns[1].toInt() and 0xFF))
        assertEquals(0x80, dns[2].toInt() and 0x80, "QR bit set")
        assertEquals(Dns.RCODE_NXDOMAIN, Dns.rcode(dns))
        assertEquals(0, dns[11].toInt(), "additional records dropped")
    }

    @Test
    fun blockedReplyDropsEdnsRecord() {
        val opt = byteArrayOf(0, 0, 41, 0x10, 0, 0, 0, 0, 0, 0, 0)
        val query = TestPackets.dnsQuery("doubleclick.net", additional = opt)
        val packet = Ipv4Udp.build(TestPackets.CLIENT, TestPackets.DNS_SERVER, 5555, 53, query)
        val verdict = assertIs<DnsPacketFilter.Verdict.Block>(filter.handle(packet, packet.size))
        val reply = assertNotNull(Ipv4Udp.parse(verdict.response, verdict.response.size))
        assertEquals(query.size - opt.size, reply.payload.size)
    }

    @Test
    fun allowedQueryIsForwarded() {
        val packet = TestPackets.udpQueryPacket("example.com")
        val verdict = assertIs<DnsPacketFilter.Verdict.Forward>(filter.handle(packet, packet.size))
        assertEquals("example.com", verdict.domain)
        assertContentEquals(TestPackets.dnsQuery("example.com"), verdict.packet.payload)
    }

    @Test
    fun nonDnsTrafficIsIgnored() {
        val packet = TestPackets.udpQueryPacket("example.com", dstPort = 443)
        assertEquals(DnsPacketFilter.Verdict.Ignore, filter.handle(packet, packet.size))
        assertEquals(DnsPacketFilter.Verdict.Ignore, filter.handle(ByteArray(10), 10))
    }

    @Test
    fun ignoresFragmentsAndNonUdp() {
        val packet = TestPackets.udpQueryPacket("doubleclick.net")
        val fragment = packet.copyOf().also { it[6] = 0x20 } // more-fragments
        assertEquals(DnsPacketFilter.Verdict.Ignore, filter.handle(fragment, fragment.size))
        val tcp = packet.copyOf().also { it[9] = 6 }
        assertEquals(DnsPacketFilter.Verdict.Ignore, filter.handle(tcp, tcp.size))
    }

    @Test
    fun builtPacketsHaveValidChecksums() {
        val packet = TestPackets.udpQueryPacket("example.com")
        // An IPv4 header including its checksum sums to zero.
        assertEquals(0, Ipv4Udp.checksum(packet, 0, 20, 0))

        // UDP checksum over pseudo-header + datagram (including checksum field) sums to zero.
        val udpLength = packet.size - 20
        var pseudo = 17L + udpLength
        for (i in 12 until 20 step 2) pseudo += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        assertEquals(0, Ipv4Udp.checksum(packet, 20, udpLength, pseudo))
    }

    @Test
    fun swappingBlocklistTakesEffect() {
        val f = DnsPacketFilter(Blocklist.EMPTY)
        val packet = TestPackets.udpQueryPacket("doubleclick.net")
        assertIs<DnsPacketFilter.Verdict.Forward>(f.handle(packet, packet.size))
        f.blocklist = Blocklist(setOf("doubleclick.net"))
        assertIs<DnsPacketFilter.Verdict.Block>(f.handle(packet, packet.size))
    }
}
