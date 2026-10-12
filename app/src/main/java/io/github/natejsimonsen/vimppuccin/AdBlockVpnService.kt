package io.github.natejsimonsen.vimppuccin

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.util.Log
import io.github.natejsimonsen.vimppuccin.core.DnsPacketFilter
import java.util.concurrent.Executors

/**
 * A local-only VPN that captures DNS traffic and nothing else.
 *
 * Android is told to use [VPN_DNS] as the DNS server, and only that single
 * address is routed into the tunnel, so all other traffic keeps flowing over
 * the normal network untouched. DNS queries for blocked domains are answered
 * with NXDOMAIN on-device; everything else is forwarded to the real network's
 * resolver.
 */
class AdBlockVpnService : VpnService() {
    // All start/stop/reload work is serialized on this thread.
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var repository: BlocklistRepository
    private lateinit var upstream: UpstreamDns
    private var tunnel: DnsTunnel? = null
    private var filter: DnsPacketFilter? = null

    override fun onCreate() {
        super.onCreate()
        repository = BlocklistRepository(this)
        upstream = UpstreamDns(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> worker.execute {
                teardown()
                stopSelf()
            }
            ACTION_RELOAD -> worker.execute(::reload)
            // ACTION_START, a restart after the process was killed (null intent),
            // or the system starting us for always-on VPN.
            else -> worker.execute(::startVpn)
        }
        return START_STICKY
    }

    override fun onRevoke() {
        // Another VPN took over, or the user revoked permission in Settings.
        worker.execute {
            teardown()
            stopSelf()
        }
    }

    override fun onDestroy() {
        worker.execute(::teardown)
        worker.shutdown()
        super.onDestroy()
    }

    private fun startVpn() {
        if (tunnel != null) return
        BlockerState.setStatus(BlockerState.Status.STARTING)
        try {
            val blocklist = repository.load()
            BlockerState.setListSize(blocklist.size)
            BlockerState.resetCounters()
            val packetFilter = DnsPacketFilter(blocklist)

            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .addAddress(VPN_ADDRESS, 24)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_DNS, 32)
                .setBlocking(true)
                .setConfigureIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            try {
                // Our own upstream queries and blocklist downloads must not loop back into the tunnel.
                builder.addDisallowedApplication(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Otherwise Android treats every network as metered while we're active.
                builder.setMetered(false)
            }

            val pfd = builder.establish()
            if (pfd == null) {
                // VPN permission not granted (or revoked).
                BlockerState.setStatus(BlockerState.Status.STOPPED)
                stopSelf()
                return
            }
            upstream.start()
            filter = packetFilter
            tunnel = DnsTunnel(this, pfd, packetFilter) { upstream.servers }.also { it.start() }
            BlockerState.setStatus(BlockerState.Status.RUNNING)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            teardown()
            stopSelf()
        }
    }

    private fun reload() {
        val f = filter ?: return
        val blocklist = repository.load()
        f.blocklist = blocklist
        BlockerState.setListSize(blocklist.size)
    }

    private fun teardown() {
        tunnel?.stop()
        tunnel = null
        filter = null
        upstream.stop()
        BlockerState.setStatus(BlockerState.Status.STOPPED)
    }

    companion object {
        private const val TAG = "AdBlockVpnService"
        private const val ACTION_START = "io.github.natejsimonsen.vimppuccin.START"
        private const val ACTION_STOP = "io.github.natejsimonsen.vimppuccin.STOP"
        private const val ACTION_RELOAD = "io.github.natejsimonsen.vimppuccin.RELOAD"

        const val VPN_ADDRESS = "10.111.222.1"
        const val VPN_DNS = "10.111.222.2"

        /** Caller must have obtained VPN consent via [VpnService.prepare] first. */
        fun start(context: Context) = send(context, ACTION_START)

        fun stop(context: Context) = send(context, ACTION_STOP)

        /** Re-reads the blocklist and allowlist if the VPN is running. */
        fun reload(context: Context) {
            if (BlockerState.status.value == BlockerState.Status.RUNNING) send(context, ACTION_RELOAD)
        }

        private fun send(context: Context, action: String) {
            context.startService(Intent(context, AdBlockVpnService::class.java).setAction(action))
        }
    }
}
