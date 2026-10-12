package io.github.natejsimonsen.vimppuccin

import android.content.Context
import io.github.natejsimonsen.vimppuccin.core.Blocklist
import io.github.natejsimonsen.vimppuccin.core.HostsParser
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Builds the active [Blocklist] from three sources:
 *  1. a small list bundled in the APK (so blocking works before the first download),
 *  2. the lists downloaded from [Prefs.sources],
 *  3. the user's custom blocklist,
 * minus anything on the user's allowlist.
 */
class BlocklistRepository(private val context: Context) {
    private val prefs = Prefs(context)
    private val downloaded = File(context.filesDir, "downloaded-hosts.txt")

    fun load(): Blocklist {
        val blocked = HashSet<String>(1 shl 17)
        context.assets.open(BUNDLED_ASSET).bufferedReader().useLines { HostsParser.parse(it, blocked) }
        if (downloaded.exists()) {
            downloaded.bufferedReader().useLines { HostsParser.parse(it, blocked) }
        }
        HostsParser.parse(prefs.customBlocklist.lineSequence(), blocked)
        val allowed = HostsParser.parse(prefs.allowlist)
        return Blocklist(blocked, allowed)
    }

    data class UpdateResult(val domains: Int, val failedSources: List<String>)

    /** Downloads every configured source. Blocking; call off the main thread. */
    @Throws(IOException::class)
    fun update(): UpdateResult {
        val urls = prefs.sources.lineSequence().map { it.trim() }.filter { it.startsWith("https://") }.toList()
        val domains = HashSet<String>(1 shl 17)
        val failed = mutableListOf<String>()
        for (url in urls) {
            try {
                download(url, domains)
            } catch (e: IOException) {
                failed += url
            }
        }
        if (urls.isNotEmpty() && failed.size == urls.size) {
            throw IOException("Could not download any blocklist")
        }

        val tmp = File(context.filesDir, "downloaded-hosts.tmp")
        tmp.bufferedWriter().use { w ->
            for (d in domains) {
                w.write(d)
                w.write('\n'.code)
            }
        }
        if (!tmp.renameTo(downloaded)) throw IOException("Could not save blocklist")
        prefs.lastUpdated = System.currentTimeMillis()
        return UpdateResult(domains.size, failed)
    }

    private fun download(url: String, into: MutableSet<String>) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "Vimppuccin")
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
            conn.inputStream.bufferedReader().useLines { HostsParser.parse(it, into) }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val BUNDLED_ASSET = "blocklist.txt"
    }
}
