package io.github.natejsimonsen.vimppuccin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-wide state shared between the VPN service, the activity and the QS tile. */
object BlockerState {
    enum class Status { STOPPED, STARTING, RUNNING }

    data class Stats(
        val queries: Long = 0,
        val blocked: Long = 0,
        val listSize: Int = 0,
        val recent: List<Query> = emptyList(),
    )

    /** A recent lookup; consecutive repeats of the same lookup are folded into [count]. */
    data class Query(val domain: String, val blocked: Boolean, val count: Int = 1)

    private const val RECENT_LIMIT = 100

    private val _status = MutableStateFlow(Status.STOPPED)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun setStatus(status: Status) {
        _status.value = status
    }

    fun setListSize(size: Int) = _stats.update { it.copy(listSize = size) }

    fun resetCounters() = _stats.update { Stats(listSize = it.listSize) }

    fun recordQuery(domain: String?, blocked: Boolean) = _stats.update { s ->
        val recent = when {
            domain == null -> s.recent
            s.recent.firstOrNull()?.let { it.domain == domain && it.blocked == blocked } == true ->
                listOf(s.recent[0].copy(count = s.recent[0].count + 1)) + s.recent.drop(1)
            else -> (listOf(Query(domain, blocked)) + s.recent).take(RECENT_LIMIT)
        }
        s.copy(
            queries = s.queries + 1,
            blocked = if (blocked) s.blocked + 1 else s.blocked,
            recent = recent,
        )
    }
}
