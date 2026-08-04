package rpt.tool.logviewer_aprifilelogetxt.utils.parsers

import rpt.tool.logviewer_aprifilelogetxt.utils.data.LogLine
import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType

object LogParser {
    private val levelPattern = Regex("(?i)\\b(ERROR|WARN|WARNING|INFO|DEBUG|VERBOSE)\\b|\\b([EWIDV])/")

    fun parseLine(id: Int, line: String): LogLine {
        val match = levelPattern.find(line)
        val type = if (match != null) {
            val tag = match.value.uppercase()
            when {
                tag.contains("ERROR") || tag.startsWith("E/") -> LogType.ERROR
                tag.contains("WARN") || tag.startsWith("W/") -> LogType.WARNING
                tag.contains("INFO") || tag.startsWith("I/") -> LogType.INFO
                tag.contains("DEBUG") || tag.startsWith("D/") -> LogType.DEBUG
                tag.contains("VERBOSE") || tag.startsWith("V/") -> LogType.VERBOSE
                else -> LogType.NORMAL
            }
        } else {
            LogType.NORMAL
        }
        return LogLine(id, line, type)
    }
}