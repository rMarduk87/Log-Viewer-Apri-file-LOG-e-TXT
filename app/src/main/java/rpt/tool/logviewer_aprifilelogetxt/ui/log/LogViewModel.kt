package rpt.tool.logviewer_aprifilelogetxt.ui.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rpt.tool.logviewer_aprifilelogetxt.R
import rpt.tool.logviewer_aprifilelogetxt.utils.data.LogLine
import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType
import rpt.tool.logviewer_aprifilelogetxt.utils.Prefs
import rpt.tool.logviewer_aprifilelogetxt.utils.parsers.LogParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class LogViewModel : ViewModel() {

    private val allLines = mutableListOf<LogLine>()

    var displayedLines by mutableStateOf<List<LogLine>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
    var isRegexMode by mutableStateOf(false)
    var isSearchActive by mutableStateOf(false)
    var currentTab by mutableIntStateOf(0)

    var showSplash by mutableStateOf(true)
    var showOnboarding by mutableStateOf(false)

    var errorCount by mutableIntStateOf(0)
        private set

    var themeMode by mutableIntStateOf(0)
        private set

    var fileName by mutableStateOf("")
    var fileDetails by mutableStateOf("-")
    var errorMessage by mutableStateOf<String?>(null)

    private var bookmarks by mutableStateOf(setOf<Int>())

    private var currentIndex = 0
    private val pageSize = 400

    fun init(context: Context) {
        if (fileName.isEmpty()) fileName = context.getString(R.string.no_file_opened)
        bookmarks = Prefs.loadBookmarks(context).toSet()

        showOnboarding = !Prefs.isOnboardingDone(context)

        themeMode = Prefs.getThemeMode(context)

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
        val current = bookmarks.toMutableSet()
        if (!current.add(id)) current.remove(id)
        bookmarks = current
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
            try {
                errorMessage = null
                if (save) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                allLines.clear()
                var id = 0

                extractFileInfo(context, uri)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { l ->
                            allLines.add(LogParser.parseLine(id++, l))
                        }
                    }
                } ?: throw Exception("Impossibile aprire il file")

                errorCount = allLines.count { it.type == LogType.ERROR }

                if (save) Prefs.saveLastUri(context, uri)

                reset()
            } catch (e: SecurityException) {
                e.printStackTrace()
                errorMessage = "Permesso negato: non è possibile accedere al file. Selezionalo di nuovo."
            } catch (e: Throwable) {
                e.printStackTrace()
                errorMessage = "Errore nel caricamento: ${e.localizedMessage}"
            }
        }
    }

    private fun extractFileInfo(context: Context, uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: "Sconosciuto"
                    }
                    if (sizeIndex != -1) {
                        val sizeBytes = cursor.getLong(sizeIndex)
                        val sizeMB = sizeBytes / (1024.0 * 1024.0)
                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm",
                            Locale.getDefault()).format(Date())
                        fileDetails = String.format(Locale.getDefault(), "%.1f MB • %s",
                            sizeMB, dateStr)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        if (searchQuery.isNotEmpty()) search(searchQuery)
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

    fun getFilteredText(onlyBookmarks: Boolean = false): String {
        val linesToExport = if (onlyBookmarks) allLines.filter { bookmarks.contains(it.id) }
        else allLines
        return linesToExport.joinToString("\n") { it.text }
    }

    fun toggleSearchActive() {
        isSearchActive = !isSearchActive
        if (!isSearchActive) {
            searchQuery = ""
            search("")
        }
    }

    fun toggleTheme(context: Context, isCurrentlyDark: Boolean) {
        val newMode = if (isCurrentlyDark) 1 else 2 // 1 = Light, 2 = Dark
        themeMode = newMode
        Prefs.saveThemeMode(context, newMode)
    }
}