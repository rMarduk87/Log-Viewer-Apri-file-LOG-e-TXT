package rpt.tool.logviewer_aprifilelogetxt.ui.log

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rpt.tool.logviewer_aprifilelogetxt.utils.Prefs
import rpt.tool.logviewer_aprifilelogetxt.utils.data.LogLine
import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType
import rpt.tool.logviewer_aprifilelogetxt.utils.parsers.LogParser
import java.util.regex.Pattern

class LogViewModel : ViewModel() {

    private val allLines = mutableListOf<LogLine>()

    var displayedLines by mutableStateOf<List<LogLine>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
    var isRegexMode by mutableStateOf(false)
    var currentTab by mutableIntStateOf(0)

    var showSplash by mutableStateOf(true)
    var showOnboarding by mutableStateOf(false)

    var errorCount by mutableIntStateOf(0)
        private set

    private val bookmarks = mutableSetOf<Int>()

    private var currentIndex = 0
    private val pageSize = 400

    fun init(context: Context) {
        bookmarks.clear()
        bookmarks.addAll(Prefs.loadBookmarks(context))

        showOnboarding = !Prefs.isOnboardingDone(context)

        viewModelScope.launch {
            delay(1200)
            showSplash = false
        }
    }


    fun completeOnboarding(context: Context) {
        Prefs.setOnboardingDone(context)
        showOnboarding = false
    }

    fun toggleBookmark(context: Context, id: Int) {
        if (!bookmarks.add(id)) bookmarks.remove(id)
        Prefs.saveBookmarks(context, bookmarks)
        refresh()
    }

    fun isBookmarked(id: Int) = bookmarks.contains(id)

    fun setTab(i: Int) {
        currentTab = i
        refresh()
    }

    fun loadFile(context: Context, uri: Uri, save: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            allLines.clear()
            currentIndex = 0

            var id = 0

            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.useLines { lines ->
                    lines.forEach { l ->
                        allLines.add(LogParser.parseLine(id++, l))
                    }
                }

            errorCount = allLines.count { it.type == LogType.ERROR }

            if (save) Prefs.saveLastUri(context, uri)

            reset()
        }
    }

    fun loadNextPage() {
        val next = allLines.asSequence().drop(currentIndex).take(pageSize).toList()
        displayedLines += next
        currentIndex += next.size
    }

    fun reset() {
        displayedLines = allLines.take(pageSize)
        currentIndex = pageSize
    }

    fun refresh() {
        displayedLines = when (currentTab) {
            1 -> allLines.filter { bookmarks.contains(it.id) }
            else -> allLines.take(pageSize)
        }
    }

    fun search(q: String) {
        searchQuery = q
        val base = if (currentTab == 1) allLines.filter { bookmarks.contains(it.id) } else allLines

        displayedLines = if (q.isEmpty()) {
            if (currentTab == 1) base else allLines.take(pageSize)
        } else if (!isRegexMode) {
            base.filter { it.text.contains(q, true) }
        } else {
            try {
                val r = Pattern.compile(q)
                base.filter { r.matcher(it.text).find() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun getFilteredText(): String {
        return displayedLines.joinToString("\n") { it.text }
    }

    fun totalSize(): Int = if (currentTab == 1) displayedLines.size else allLines.size
}
