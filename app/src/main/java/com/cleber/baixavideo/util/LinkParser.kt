package com.cleber.baixavideo.util

/** Plataformas suportadas nativamente pelo app. */
enum class Platform(val label: String) {
    INSTAGRAM("Instagram"),
    KWAI("Kwai"),
    OTHER("Link")
}

/**
 * Utilitários para reconhecer e normalizar os links colados pelo usuário.
 * Aceita links completos (com parâmetros de rastreio) e textos que contenham
 * um link no meio, como o texto compartilhado pelo Instagram ou Kwai.
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

    private val kwaiRegex = Regex(
        """(?:kwai\.com|kwai\.app|kwai\.video|k\.kwai)""",
        RegexOption.IGNORE_CASE
    )

    /** Extrai o primeiro link http(s) de um texto livre. */
    fun firstUrl(text: String): String? {
        val match = urlRegex.find(text.trim()) ?: return null
        return match.value.trimEnd('.', ',', ')', ']', ';', '"', '\'')
    }

    fun platform(url: String): Platform = when {
        instagramRegex.containsMatchIn(url) -> Platform.INSTAGRAM
        kwaiRegex.containsMatchIn(url) -> Platform.KWAI
        else -> Platform.OTHER
    }

    /** Retorna true quando o texto contém um link do Instagram ou Kwai. */
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
