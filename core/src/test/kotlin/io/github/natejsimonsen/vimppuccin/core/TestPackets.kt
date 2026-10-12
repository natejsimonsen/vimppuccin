package io.github.natejsimonsen.vimppuccin.core

import java.io.ByteArrayOutputStream

internal object TestPackets {
    val CLIENT = byteArrayOf(10, 111, 222.toByte(), 1)
    val DNS_SERVER = byteArrayOf(10, 111, 222.toByte(), 2)

    fun dnsQuery(name: String, type: Int = 1, id: Int = 0x1234, additional: ByteArray = ByteArray(0)): ByteArray {
        val out = ByteArrayOutputStream()
        fun u16(v: Int) { out.write(v shr 8); out.write(v and 0xFF) }
        u16(id)
        u16(0x0100) // RD
        u16(1)
        u16(0)
        u16(0)
        u16(if (additional.isEmpty()) 0 else 1)
        for (label in name.split('.')) {
            out.write(label.length)
            out.write(label.toByteArray())
        }
        out.write(0)
        u16(type)
        u16(1) // IN
        out.write(additional)
        return out.toByteArray()
    }

    fun udpQueryPacket(name: String, dstPort: Int = 53): ByteArray =
        Ipv4Udp.build(CLIENT, DNS_SERVER, 40000, dstPort, dnsQuery(name))
}
