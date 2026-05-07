package rpt.tool.logviewer_aprifilelogetxt.utils.data

import rpt.tool.logviewer_aprifilelogetxt.utils.data.enums.LogType

data class LogLine(
    val id: Int,
    val text: String,
    val type: LogType
)