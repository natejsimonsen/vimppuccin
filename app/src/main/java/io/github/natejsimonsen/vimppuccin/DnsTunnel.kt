package io.github.natejsimonsen.vimppuccin

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import io.github.natejsimonsen.vimppuccin.core.DnsPacketFilter
import io.github.natejsimonsen.vimppuccin.core.Ipv4Udp
import io.github.natejsimonsen.vimppuccin.core.UdpPacket
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

/**
 * Reads DNS packets from the TUN device, answers blocked queries directly and
 * relays the rest to an upstream resolver over a socket that bypasses the VPN.
 */
class DnsTunnel(
    private val vpn: VpnService,
    private val tun: ParcelFileDescriptor,
    private val filter: DnsPacketFilter,
    private val upstreams: () -> List<InetAddress>,
) {
    private val output = FileOutputStream(tun.fileDescriptor)
    private val forwarders = Executors.newFixedThreadPool(FORWARD_THREADS)

    // Closing the write end wakes the read loop out of poll().
    private val stopPipe: Array<FileDescriptor> = Os.pipe()
    private var thread: Thread? = null

    fun start() {
        thread = Thread(::readLoop, "vimppuccin-dns").also { it.start() }
    }

    fun stop() {
        closeQuietly(stopPipe[1])
        thread?.join(STOP_TIMEOUT_MS)
        thread = null
        forwarders.shutdownNow()
        forwarders.awaitTermination(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        closeQuietly(stopPipe[0])
        try {
            tun.close()
        } catch (_: IOException) {
        }
    }

    private fun readLoop() {
        val input = FileInputStream(tun.fileDescriptor)
        val buf = ByteArray(MAX_PACKET)
        val tunPoll = StructPollfd().apply {
            fd = tun.fileDescriptor
            events = OsConstants.POLLIN.toShort()
        }
        val stopPoll = StructPollfd().apply {
            fd = stopPipe[0]
            events = (OsConstants.POLLHUP or OsConstants.POLLERR).toShort()
        }
        val fds = arrayOf(tunPoll, stopPoll)

        while (true) {
            tunPoll.revents = 0
            stopPoll.revents = 0
            try {
                Os.poll(fds, -1)
            } catch (e: ErrnoException) {
                if (e.errno == OsConstants.EINTR) continue
                Log.w(TAG, "poll failed", e)
                return
            }
            if (stopPoll.revents.toInt() != 0) return
            val revents = tunPoll.revents.toInt()
            if (revents and (OsConstants.POLLHUP or OsConstants.POLLERR or OsConstants.POLLNVAL) != 0) return
            if (revents and OsConstants.POLLIN == 0) continue

            val n = try {
                input.read(buf)
            } catch (e: IOException) {
                Log.w(TAG, "tun read failed", e)
                return
            }
            if (n > 0) handlePacket(buf, n)
        }
    }

    private fun handlePacket(buf: ByteArray, len: Int) {
        when (val verdict = filter.handle(buf, len)) {
            is DnsPacketFilter.Verdict.Block -> {
                BlockerState.recordQuery(verdict.domain)
                writeToTun(verdict.response)
            }
            is DnsPacketFilter.Verdict.Forward -> {
                BlockerState.recordQuery(null)
                try {
                    forwarders.execute { forward(verdict.packet) }
                } catch (_: RejectedExecutionException) {
                    // Shutting down.
                }
            }
            DnsPacketFilter.Verdict.Ignore -> Unit
        }
    }

    private fun forward(request: UdpPacket) {
        val query = request.payload
        for (server in upstreams()) {
            try {
                DatagramSocket().use { socket ->
                    vpn.protect(socket)
                    socket.soTimeout = UPSTREAM_TIMEOUT_MS
                    socket.send(DatagramPacket(query, query.size, server, DnsPacketFilter.DNS_PORT))
                    val response = ByteArray(MAX_DNS_RESPONSE)
                    val packet = DatagramPacket(response, response.size)
                    socket.receive(packet)
                    writeToTun(Ipv4Udp.reply(request, response.copyOf(packet.length)))
                }
                return
            } catch (e: IOException) {
                if (Thread.currentThread().isInterrupted) return
                // Try the next upstream server.
            }
        }
    }

    private fun writeToTun(packet: ByteArray) {
        synchronized(output) {
            try {
                output.write(packet)
            } catch (e: IOException) {
                Log.w(TAG, "tun write failed", e)
            }
        }
    }

    private fun closeQuietly(fd: FileDescriptor) {
        try {
            Os.close(fd)
        } catch (_: ErrnoException) {
        }
    }

    companion object {
        private const val TAG = "DnsTunnel"
        private const val MAX_PACKET = 32767
        private const val MAX_DNS_RESPONSE = 65507
        private const val UPSTREAM_TIMEOUT_MS = 5_000
        private const val FORWARD_THREADS = 8
        private const val STOP_TIMEOUT_MS = 2_000L
    }
}
