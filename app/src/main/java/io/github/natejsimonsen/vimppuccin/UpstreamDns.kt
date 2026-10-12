package io.github.natejsimonsen.vimppuccin

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import java.net.InetAddress

/**
 * Tracks the DNS servers of the underlying (non-VPN) network, so allowed
 * queries go to the same resolver they would without the blocker. This app
 * is excluded from its own VPN, so its default network is the real one.
 */
class UpstreamDns(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    @Volatile
    var servers: List<InetAddress> = FALLBACK
        private set

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            update(linkProperties)
        }
    }

    fun start() {
        cm.activeNetwork?.let { cm.getLinkProperties(it) }?.let(::update)
        cm.registerDefaultNetworkCallback(callback)
    }

    fun stop() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: IllegalArgumentException) {
            // Not registered.
        }
    }

    private fun update(lp: LinkProperties) {
        val dns = lp.dnsServers.filterNot { it.hostAddress == AdBlockVpnService.VPN_DNS }
        servers = dns.ifEmpty { FALLBACK }
    }

    companion object {
        /** Used only when the network reports no DNS servers. */
        private val FALLBACK: List<InetAddress> = listOf(
            InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
            InetAddress.getByAddress(byteArrayOf(9, 9, 9, 9)),
        )
    }
}
