package io.github.natejsimonsen.vimppuccin.core

/**
 * An immutable set of blocked domains plus an allowlist that overrides it.
 *
 * Matching is by domain suffix on label boundaries: blocking `example.com`
 * also blocks `ads.example.com`, and allowing `example.com` allows every
 * subdomain of it even if a subdomain is on the blocklist.
 */
class Blocklist(
    private val blocked: Set<String>,
    private val allowed: Set<String> = emptySet(),
) {
    val size: Int get() = blocked.size

    fun isBlocked(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        if (d.isEmpty()) return false
        if (matches(allowed, d)) return false
        return matches(blocked, d)
    }

    private fun matches(set: Set<String>, domain: String): Boolean {
        if (set.isEmpty()) return false
        var current = domain
        while (true) {
            if (current in set) return true
            val dot = current.indexOf('.')
            if (dot < 0) return false
            current = current.substring(dot + 1)
        }
    }

    companion object {
        val EMPTY = Blocklist(emptySet())
    }
}
