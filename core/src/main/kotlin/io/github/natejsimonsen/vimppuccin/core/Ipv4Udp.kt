package io.github.natejsimonsen.vimppuccin.core

/** A UDP datagram carried in an IPv4 packet. Addresses are 4-byte arrays. */
class UdpPacket(
    val srcAddress: ByteArray,
    val dstAddress: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val payload: ByteArray,
)

/** Minimal IPv4 + UDP parsing and serialization for packets read from / written to a TUN device. */
object Ipv4Udp {
    private const val IPV4_HEADER = 20
    private const val UDP_HEADER = 8
    const val PROTOCOL_UDP = 17

    /** Parses [len] bytes of [buf] as an unfragmented IPv4/UDP packet, or returns null. */
    fun parse(buf: ByteArray, len: Int): UdpPacket? {
        if (len < IPV4_HEADER + UDP_HEADER) return null
        if (u8(buf, 0) shr 4 != 4) return null
        val ihl = (u8(buf, 0) and 0x0F) * 4
        if (ihl < IPV4_HEADER) return null
        val totalLength = u16(buf, 2)
        if (totalLength < ihl + UDP_HEADER || totalLength > len) return null
        // Drop fragments: more-fragments flag set or non-zero fragment offset.
        if (u16(buf, 6) and 0x3FFF != 0) return null
        if (u8(buf, 9) != PROTOCOL_UDP) return null

        val udpLength = u16(buf, ihl + 4)
        if (udpLength < UDP_HEADER || ihl + udpLength > totalLength) return null

        return UdpPacket(
            srcAddress = buf.copyOfRange(12, 16),
            dstAddress = buf.copyOfRange(16, 20),
            srcPort = u16(buf, ihl),
            dstPort = u16(buf, ihl + 2),
            payload = buf.copyOfRange(ihl + UDP_HEADER, ihl + udpLength),
        )
    }

    /** Builds a reply to [request] carrying [payload], with source and destination swapped. */
    fun reply(request: UdpPacket, payload: ByteArray): ByteArray =
        build(request.dstAddress, request.srcAddress, request.dstPort, request.srcPort, payload)

    fun build(src: ByteArray, dst: ByteArray, srcPort: Int, dstPort: Int, payload: ByteArray): ByteArray {
        require(src.size == 4 && dst.size == 4) { "IPv4 addresses must be 4 bytes" }
        val udpLength = UDP_HEADER + payload.size
        val total = IPV4_HEADER + udpLength
        require(total <= 0xFFFF) { "Packet too large" }
        val p = ByteArray(total)

        p[0] = 0x45 // version 4, IHL 5
        put16(p, 2, total)
        put16(p, 6, 0x4000) // don't fragment
        p[8] = 64 // TTL
        p[9] = PROTOCOL_UDP.toByte()
        src.copyInto(p, 12)
        dst.copyInto(p, 16)
        put16(p, 10, checksum(p, 0, IPV4_HEADER, 0))

        put16(p, 20, srcPort)
        put16(p, 22, dstPort)
        put16(p, 24, udpLength)
        payload.copyInto(p, IPV4_HEADER + UDP_HEADER)
        put16(p, 26, udpChecksum(p, udpLength))
        return p
    }

    private fun udpChecksum(p: ByteArray, udpLength: Int): Int {
        // Pseudo-header: src addr, dst addr, zero, protocol, UDP length.
        var sum = 0L
        for (i in 12 until 20 step 2) sum += u16(p, i)
        sum += PROTOCOL_UDP
        sum += udpLength
        val c = checksum(p, IPV4_HEADER, udpLength, sum)
        return if (c == 0) 0xFFFF else c
    }

    /** RFC 1071 internet checksum over [len] bytes at [off], seeded with [initial]. */
    fun checksum(p: ByteArray, off: Int, len: Int, initial: Long): Int {
        var sum = initial
        var i = off
        val end = off + len
        while (i + 1 < end) {
            sum += u16(p, i)
            i += 2
        }
        if (i < end) sum += u8(p, i) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv().toInt() and 0xFFFF
    }

    internal fun u8(b: ByteArray, i: Int) = b[i].toInt() and 0xFF
    internal fun u16(b: ByteArray, i: Int) = (u8(b, i) shl 8) or u8(b, i + 1)
    internal fun put16(b: ByteArray, i: Int, v: Int) {
        b[i] = (v shr 8).toByte()
        b[i + 1] = v.toByte()
    }
}
