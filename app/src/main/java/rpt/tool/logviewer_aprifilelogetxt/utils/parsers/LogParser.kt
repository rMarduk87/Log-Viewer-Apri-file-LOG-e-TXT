package rpt.tool.logviewer_aprifilelogetxt.utils.parsers

import rpt.tool.logviewer_aprifilelogetxt.utils.data.LogLine
import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType

object LogParser {
    fun parseLine(id: Int, line: String): LogLine {
        val type = when {
            line.contains("error", true) -> LogType.ERROR
            line.contains("warning", true) -> LogType.WARNING
            line.contains("info", true) -> LogType.INFO
            else -> LogType.NORMAL
        }
        return LogLine(id, line, type)
    }
}