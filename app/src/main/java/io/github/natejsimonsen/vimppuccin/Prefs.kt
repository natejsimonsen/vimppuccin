package io.github.natejsimonsen.vimppuccin

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("vimppuccin", Context.MODE_PRIVATE)

    /** Domains that are never blocked, one per line. */
    var allowlist: String
        get() = sp.getString(KEY_ALLOWLIST, "") ?: ""
        set(value) = sp.edit().putString(KEY_ALLOWLIST, value).apply()

    /** Extra domains to block on top of the downloaded lists, one per line. */
    var customBlocklist: String
        get() = sp.getString(KEY_CUSTOM, "") ?: ""
        set(value) = sp.edit().putString(KEY_CUSTOM, value).apply()

    /** HTTPS URLs of hosts files / domain lists to download, one per line. */
    var sources: String
        get() = sp.getString(KEY_SOURCES, null) ?: DEFAULT_SOURCES
        set(value) = sp.edit().putString(KEY_SOURCES, value).apply()

    var lastUpdated: Long
        get() = sp.getLong(KEY_LAST_UPDATED, 0)
        set(value) = sp.edit().putLong(KEY_LAST_UPDATED, value).apply()

    companion object {
        private const val KEY_ALLOWLIST = "allowlist"
        private const val KEY_CUSTOM = "custom_blocklist"
        private const val KEY_SOURCES = "sources"
        private const val KEY_LAST_UPDATED = "last_updated"

        const val DEFAULT_SOURCES =
            // Wildcard list: each entry blocks the domain and all its subdomains, which
            // covers the many hostnames used by mobile ad SDKs and mediation networks.
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/pro.mini-onlydomains.txt\n" +
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    }
}
