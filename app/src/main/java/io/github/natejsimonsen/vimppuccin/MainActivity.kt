package io.github.natejsimonsen.vimppuccin

import android.app.AlertDialog
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.NumberFormat

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private lateinit var repository: BlocklistRepository

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var queriesText: TextView
    private lateinit var blockedText: TextView
    private lateinit var listSizeText: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var updateButton: Button
    private lateinit var recentText: TextView

    private val numbers = NumberFormat.getIntegerInstance()

    private val vpnConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            AdBlockVpnService.start(this)
        } else {
            Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        repository = BlocklistRepository(this)

        statusText = findViewById(R.id.status)
        toggleButton = findViewById(R.id.toggle)
        queriesText = findViewById(R.id.queries)
        blockedText = findViewById(R.id.blocked)
        listSizeText = findViewById(R.id.list_size)
        lastUpdatedText = findViewById(R.id.last_updated)
        updateButton = findViewById(R.id.update_lists)
        recentText = findViewById(R.id.recent)

        toggleButton.setOnClickListener {
            if (BlockerState.status.value == BlockerState.Status.STOPPED) requestStart() else AdBlockVpnService.stop(this)
        }
        updateButton.setOnClickListener { updateBlocklists() }
        findViewById<Button>(R.id.edit_allowlist).setOnClickListener {
            editList(R.string.allowlist_title, R.string.allowlist_hint, prefs.allowlist) { prefs.allowlist = it }
        }
        findViewById<Button>(R.id.edit_custom).setOnClickListener {
            editList(R.string.custom_title, R.string.custom_hint, prefs.customBlocklist) { prefs.customBlocklist = it }
        }
        findViewById<Button>(R.id.edit_sources).setOnClickListener {
            editList(R.string.sources_title, R.string.sources_hint, prefs.sources) {
                prefs.sources = it.ifBlank { Prefs.DEFAULT_SOURCES }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                BlockerState.status.combine(BlockerState.stats) { s, st -> s to st }.collect { (status, stats) -> render(status, stats) }
            }
        }

        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        renderLastUpdated()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_REQUEST_START && BlockerState.status.value == BlockerState.Status.STOPPED) {
            requestStart()
        }
    }

    private fun requestStart() {
        val consent = VpnService.prepare(this)
        if (consent == null) AdBlockVpnService.start(this) else vpnConsent.launch(consent)
    }

    private fun render(status: BlockerState.Status, stats: BlockerState.Stats) {
        when (status) {
            BlockerState.Status.STOPPED -> {
                statusText.setText(R.string.status_stopped)
                statusText.setTextColor(getColor(R.color.red))
                toggleButton.setText(R.string.start)
            }
            BlockerState.Status.STARTING -> {
                statusText.setText(R.string.status_starting)
                statusText.setTextColor(getColor(R.color.peach))
                toggleButton.setText(R.string.stop)
            }
            BlockerState.Status.RUNNING -> {
                statusText.setText(R.string.status_running)
                statusText.setTextColor(getColor(R.color.green))
                toggleButton.setText(R.string.stop)
            }
        }
        queriesText.text = numbers.format(stats.queries)
        blockedText.text = numbers.format(stats.blocked)
        listSizeText.text = if (stats.listSize > 0) {
            getString(R.string.list_size, numbers.format(stats.listSize))
        } else {
            getString(R.string.list_size_unknown)
        }
        recentText.text = if (stats.recent.isEmpty()) getString(R.string.recent_empty) else formatRecent(stats.recent)
    }

    /** Blocked lookups in red with ✕, allowed ones with ✓, so leaks are easy to spot. */
    private fun formatRecent(recent: List<BlockerState.Query>): CharSequence {
        val out = SpannableStringBuilder()
        val blockedColor = getColor(R.color.red)
        val allowedColor = getColor(R.color.text)
        for ((i, q) in recent.withIndex()) {
            if (i > 0) out.append('\n')
            val start = out.length
            out.append(if (q.blocked) "✕ " else "✓ ").append(q.domain)
            if (q.count > 1) out.append(" ×").append(q.count.toString())
            out.setSpan(
                ForegroundColorSpan(if (q.blocked) blockedColor else allowedColor),
                start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return out
    }

    private fun renderLastUpdated() {
        val last = prefs.lastUpdated
        lastUpdatedText.text = if (last == 0L) {
            getString(R.string.never_updated)
        } else {
            getString(R.string.last_updated, DateUtils.getRelativeTimeSpanString(last))
        }
    }

    private fun updateBlocklists() {
        updateButton.isEnabled = false
        updateButton.setText(R.string.updating)
        lifecycleScope.launch {
            val message = try {
                val result = withContext(Dispatchers.IO) { repository.update() }
                AdBlockVpnService.reload(this@MainActivity)
                if (result.failedSources.isEmpty()) {
                    getString(R.string.update_ok, numbers.format(result.domains))
                } else {
                    getString(R.string.update_partial, numbers.format(result.domains), result.failedSources.size)
                }
            } catch (e: IOException) {
                getString(R.string.update_failed, e.message ?: e.javaClass.simpleName)
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            updateButton.isEnabled = true
            updateButton.setText(R.string.update_lists)
            renderLastUpdated()
        }
    }

    private fun editList(title: Int, hint: Int, current: String, save: (String) -> Unit) {
        val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
        val input = EditText(this).apply {
            setText(current)
            setHint(hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_URI
            gravity = Gravity.TOP or Gravity.START
            minLines = 6
            setPadding(padding, padding, padding, padding)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                save(input.text.toString().trim())
                AdBlockVpnService.reload(this)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val ACTION_REQUEST_START = "io.github.natejsimonsen.vimppuccin.REQUEST_START"
    }
}
