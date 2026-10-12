package io.github.natejsimonsen.vimppuccin.core

/**
 * Decides what to do with each raw packet read from the TUN device:
 * answer it locally (blocked), forward it upstream, or ignore it.
 */
class DnsPacketFilter(@Volatile var blocklist: Blocklist) {

    sealed interface Verdict {
        /** The query was for a blocked domain; write [response] back to the TUN device. */
        class Block(val domain: String, val response: ByteArray) : Verdict

        /** Forward [packet]'s payload to an upstream resolver and relay the answer. */
        class Forward(val domain: String?, val packet: UdpPacket) : Verdict

        /** Not a DNS-over-UDP packet; drop it. */
        data object Ignore : Verdict
    }

    fun handle(buf: ByteArray, len: Int): Verdict {
        val packet = Ipv4Udp.parse(buf, len) ?: return Verdict.Ignore
        if (packet.dstPort != DNS_PORT) return Verdict.Ignore
        val question = Dns.parseQuestion(packet.payload) ?: return Verdict.Forward(null, packet)
        if (blocklist.isBlocked(question.name)) {
            val response = Dns.blockedResponse(packet.payload, question)
            return Verdict.Block(question.name, Ipv4Udp.reply(packet, response))
        }
        return Verdict.Forward(question.name, packet)
    }

    companion object {
        const val DNS_PORT = 53
    }
}
