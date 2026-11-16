package com.cleber.baixatrix.util

import com.cleber.baixatrix.net.Http

/**
 * Pequenos utilitários de HTML/JSON usados pelos extratores.
 * Não é um parser completo: as páginas do Instagram e do Kwai trazem o
 * necessário dentro de blocos JSON embutidos, então extraímos por padrão.
 */
object Html {

    fun meta(html: String, key: String): String? {
        val tags = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(html)
        for (tagMatch in tags) {
            val tag = tagMatch.value
            val name = attr(tag, "property") ?: attr(tag, "name") ?: continue
            if (name.equals(key, ignoreCase = true)) {
                return attr(tag, "content")?.let { decodeEntities(it) }
            }
        }
        return null
    }

    fun attr(tag: String, name: String): String? {
        val regex = Regex(
            "\\b" + Regex.escape(name) + "\\s*=\\s*(\"([^\"]*)\"|'([^']*)')",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(tag) ?: return null
        return match.groupValues[2].ifEmpty { match.groupValues[3] }
    }

    /** Desfaz escapes comuns de JSON dentro de HTML (inclusive escapes duplos). */
    fun unescape(value: String): String {
        val unicode = Regex("""\\+u([0-9a-fA-F]{4})""")
        val slashes = Regex("""\\+/""")
        var result = unicode.replace(value) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
        result = slashes.replace(result, "/")
        result = Regex("""\\+[nrt]""").replace(result, " ")
        return result
            .replace("\\\"", "\"")
            .replace("&amp;", "&")
    }

    fun decodeEntities(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    /** Todos os valores de string de uma chave JSON, já sem escapes. */
    fun jsonStrings(json: String, key: String): List<String> {
        val normalized = json.replace("\\\"", "\"")
        val regex = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        return regex.findAll(normalized)
            .map { unescape(it.groupValues[1]) }
            .filter { it.isNotBlank() && it != "null" }
            .toList()
    }

    fun jsonNumber(json: String, key: String): Double? {
        val normalized = json.replace("\\\"", "\"")
        val regex = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return regex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    /** URLs de vídeo dentro de tags <video src="..."> e <source src="...">. */
    fun videoTagUrls(html: String): List<String> {
        val out = mutableListOf<String>()
        val tagRegex = Regex("""<(?:video|source)\b[^>]*>""", RegexOption.IGNORE_CASE)
        for (match in tagRegex.findAll(html)) {
            attr(match.value, "src")?.let { out += decodeEntities(unescape(it)) }
            attr(match.value, "data-src")?.let { out += decodeEntities(unescape(it)) }
        }
        return out
    }

    /** Todas as URLs de vídeo encontradas no HTML, inclusive as escapadas. */
    fun mp4Urls(html: String): List<String> {
        val text = unescape(html)
        val regex = Regex("""https?://[^\s"'<>\\]+?\.(?:mp4|m3u8)[^\s"'<>\\]*""", RegexOption.IGNORE_CASE)
        return regex.findAll(text).map { it.value }.toList()
    }

    fun isVideoUrl(url: String): Boolean =
        url.startsWith("http") && (url.contains(".mp4", true) || url.contains(".m3u8", true))

    fun cleanTitle(value: String?): String? = value
        ?.replace("\\n", " ")
        ?.replace("\\r", " ")
        ?.substringBefore(" • ")
        ?.substringBefore(" | ")
        ?.replace(Regex("""\s+"""), " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.contains("Instagram", ignoreCase = true) }

    /** Extrai @usuario de títulos do Instagram: "Nome (@usuario) • ...". */
    fun instagramHandle(value: String?): String? {
        if (value == null) return null
        return Regex("""\(@([A-Za-z0-9._]+)\)""").find(value)?.groupValues?.getOrNull(1)
    }

    /** Cabeçalhos padrão de navegador usados nas requisições de extração. */
    fun browserHeaders(userAgent: String = Http.DESKTOP_UA): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "pt-BR,pt;q=0.9,en;q=0.8"
    )
}
