package com.focusfit.app.domain

import java.net.URLEncoder

object YouTube {
    private val ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val PATTERNS = listOf(
        Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
        Regex("""youtube\.com/(?:shorts|embed|live)/([A-Za-z0-9_-]{11})"""),
    )

    /** Extracts the video id from a pasted link (watch, youtu.be, shorts, embed) or a bare id. */
    fun idFrom(input: String): String? {
        val s = input.trim()
        if (ID.matches(s)) return s
        if (!s.contains("youtu")) return null
        return PATTERNS.firstNotNullOfOrNull { it.find(s)?.groupValues?.get(1) }
    }

    fun watchUrl(id: String, startSec: Int = 0) = "https://www.youtube.com/watch?v=$id" + if (startSec > 0) "&t=${startSec}s" else ""

    fun searchUrl(query: String) = "https://www.youtube.com/results?search_query=" + URLEncoder.encode("$query exercise how to", "UTF-8")
}
