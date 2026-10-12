package io.github.natejsimonsen.vimppuccin.core

/**
 * Parses blocklists in the common formats:
 *  - hosts files (`0.0.0.0 ads.example.com`, `127.0.0.1 a.com b.com`)
 *  - plain domain lists (`ads.example.com`)
 *  - simple Adblock-style domain rules (`||ads.example.com^`)
 *
 * Anything else (cosmetic filters, URL-path rules, exceptions) is ignored.
 */
object HostsParser {
    private val IGNORED = setOf(
        "localhost", "localhost.localdomain", "local", "broadcasthost",
        "ip6-localhost", "ip6-loopback", "ip6-localnet", "ip6-mcastprefix",
        "ip6-allnodes", "ip6-allrouters", "ip6-allhosts", "0.0.0.0",
    )
    private val ADBLOCK_RULE = Regex("""^\|\|([a-z0-9._-]+)\^(\${'$'}important)?$""")
    private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun parse(lines: Sequence<String>, into: MutableSet<String> = HashSet()): MutableSet<String> {
        for (line in lines) parseLine(line, into)
        return into
    }

    fun parse(text: String): MutableSet<String> = parse(text.lineSequence())

    private fun parseLine(raw: String, into: MutableSet<String>) {
        val line = stripComment(raw).trim().lowercase()
        if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) return

        if (line.startsWith("||")) {
            ADBLOCK_RULE.matchEntire(line)?.let { m -> normalize(m.groupValues[1])?.let(into::add) }
            return
        }

        val tokens = line.split(' ', '\t').filter { it.isNotEmpty() }
        val hosts = if (tokens.size > 1 && isIpAddress(tokens[0])) tokens.drop(1) else tokens.take(1)
        for (host in hosts) normalize(host)?.let(into::add)
    }

    /**
     * `#` starts a comment only at the start of a line or after whitespace, so
     * Adblock cosmetic rules like `example.com##.banner` aren't misread as a
     * bare domain (they are rejected by [normalize] instead).
     */
    private fun stripComment(line: String): String {
        var i = line.indexOf('#')
        while (i >= 0) {
            if (i == 0 || line[i - 1].isWhitespace()) return line.substring(0, i)
            i = line.indexOf('#', i + 1)
        }
        return line
    }

    private fun isIpAddress(token: String) = IPV4.matches(token) || token.contains(':')

    /** Returns a canonical lowercase domain, or null if [raw] is not a usable domain name. */
    fun normalize(raw: String): String? {
        val d = raw.trim().lowercase().trimEnd('.')
        if (d.isEmpty() || d.length > 253 || d in IGNORED) return null
        if (!d.contains('.') || d.startsWith('.') || d.contains("..")) return null
        if (IPV4.matches(d)) return null
        if (d.any { !(it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '-' || it == '_') }) return null
        return d
    }
}
