package com.smartattend.app.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object StudentRoutes {
    const val DASHBOARD = "dashboard"
    const val ATTENDANCE = "attendance"
    const val SCHEDULE = "schedule"
    const val INBOX = "inbox"
    const val MORE = "more"
    const val HISTORY = "history"
    const val SHORTAGE_CALCULATOR = "shortage-calculator"
    const val CORRECTIONS = "corrections"
    const val DEVICE = "device"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val ABOUT_DEMO = "about-demo"

    const val SUBJECT_PATTERN = "subject/{id}"
    const val NOTICE_PATTERN = "notice/{id}"
    const val SCHEDULE_DETAIL_PATTERN = "schedule-detail/{id}"
    const val CORRECTION_DETAIL_PATTERN = "correction/{id}"
    const val CORRECTION_FORM_PATTERN = "correction-new/{id}"

    fun subject(id: String) = nested("subject", id)
    fun notice(id: String) = nested("notice", id)
    fun scheduleDetail(id: String) = nested("schedule-detail", id)
    fun correctionDetail(id: String) = nested("correction", id)
    fun correctionForm(attendanceId: String) = nested("correction-new", attendanceId)

    fun subjectId(route: String?) = parse(route, "subject")
    fun noticeId(route: String?) = parse(route, "notice")
    fun scheduleId(route: String?) = parse(route, "schedule-detail")
    fun correctionId(route: String?) = parse(route, "correction")
    fun attendanceId(route: String?) = parse(route, "correction-new")

    private fun nested(prefix: String, id: String) = "$prefix/${encode(id)}"

    private fun parse(route: String?, prefix: String): String? {
        val encoded = route?.removePrefix("$prefix/")?.takeIf {
            route.startsWith("$prefix/") && it.isNotBlank() && '/' !in it
        } ?: return null
        return runCatching { URLDecoder.decode(encoded, StandardCharsets.UTF_8.name()) }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
