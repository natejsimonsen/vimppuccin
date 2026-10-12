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
        val recentBlocked: List<String> = emptyList(),
    )

    private const val RECENT_LIMIT = 50

    private val _status = MutableStateFlow(Status.STOPPED)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun setStatus(status: Status) {
        _status.value = status
    }

    fun setListSize(size: Int) = _stats.update { it.copy(listSize = size) }

    fun resetCounters() = _stats.update { Stats(listSize = it.listSize) }

    fun recordQuery(blockedDomain: String?) = _stats.update { s ->
        if (blockedDomain == null) {
            s.copy(queries = s.queries + 1)
        } else {
            s.copy(
                queries = s.queries + 1,
                blocked = s.blocked + 1,
                recentBlocked = (listOf(blockedDomain) + s.recentBlocked).take(RECENT_LIMIT),
            )
        }
    }
}
