package com.cleber.baixatrix.util

import java.net.URI

/** Plataformas suportadas pelo app. */
enum class Platform(val label: String) {
    INSTAGRAM("Instagram"),
    KWAI("Kwai"),
    YOUTUBE("YouTube"),
    OTHER("Link")
}

/**
 * Utilitários para reconhecer e normalizar os links colados pelo usuário.
 * Aceita links completos (com parâmetros de rastreio) e textos que contenham
 * um link no meio, como os textos compartilhados pelo Instagram, Kwai, Kwai Lite
 * ou YouTube.
 */
object LinkParser {

    private val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

    private val instagramRegex = Regex(
        """(?:instagram\.com|instagr\.am)/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )

    private val instagramKindRegex = Regex(
        """(?:instagram\.com|instagr\.am)/(p|reel|reels|tv)/""",
        RegexOption.IGNORE_CASE
    )

    private val kwaiDomains = setOf("kwai.com", "kwai.app", "kwai.video", "kwai-video.com")
    private val youtubeHosts = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be"
    )
    private val youtubeVideoIdRegex = Regex("^[A-Za-z0-9_-]{11}$")
    private val youtubeWatchIdRegex = Regex("(?:^|&)v=([A-Za-z0-9_-]{11})(?:&|$)")

    /** Extrai o primeiro link http(s) de um texto livre. */
    fun firstUrl(text: String): String? {
        val match = urlRegex.find(text.trim()) ?: return null
        return match.value.trimEnd('.', ',', ')', ']', ';', '"', '\'')
    }

    fun platform(url: String): Platform = when {
        isYoutubeUrl(url) -> Platform.YOUTUBE
        isInstagramUrl(url) -> Platform.INSTAGRAM
        isKwaiUrl(url) -> Platform.KWAI
        else -> Platform.OTHER
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (uri.scheme?.lowercase() != "https" || host !in youtubeHosts) return false
        if (uri.userInfo != null || uri.port !in setOf(-1, 443)) return false

        val segments = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
        val videoId = when {
            host == "youtu.be" -> segments.firstOrNull()
            uri.path == "/watch" -> youtubeWatchIdRegex.find(uri.rawQuery.orEmpty())
                ?.groupValues?.getOrNull(1)
            segments.size >= 2 && segments.first() in setOf("shorts", "live", "embed") ->
                segments[1]
            else -> null
        }
        return youtubeVideoIdRegex.matches(videoId.orEmpty())
    }

    private fun isInstagramUrl(url: String): Boolean {
        val host = host(url) ?: return false
        val isInstagramHost = hostMatches(host, "instagram.com") || hostMatches(host, "instagr.am")
        return isInstagramHost && instagramRegex.containsMatchIn(url)
    }

    private fun isKwaiUrl(url: String): Boolean {
        val host = host(url) ?: return false
        return host == "k.kwai" || kwaiDomains.any { domain -> hostMatches(host, domain) }
    }

    private fun host(url: String): String? = runCatching { URI(url).host?.lowercase() }.getOrNull()

    private fun hostMatches(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")

    /** Retorna true quando o texto contém link de uma plataforma suportada. */
    fun supports(text: String): Boolean {
        val url = firstUrl(text) ?: return false
        return platform(url) != Platform.OTHER
    }

    fun instagramShortcode(url: String): String? =
        instagramRegex.find(url)?.groupValues?.getOrNull(1)

    /** O Instagram usa "reel", "reels", "tv" ou "p" na URL. */
    fun instagramKind(url: String): String {
        val kind = instagramKindRegex.find(url)?.groupValues?.getOrNull(1)?.lowercase() ?: "p"
        return if (kind == "reels") "reel" else kind
    }

    /** Remove parâmetros de rastreio para exibir um link limpo. */
    fun clean(url: String): String = url.substringBefore('?').trimEnd('/')
}
