package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.model.VideoVariant
import com.cleber.baixatrix.net.Http
import com.cleber.baixatrix.util.Html
import com.cleber.baixatrix.util.LinkParser
import com.cleber.baixatrix.util.Platform

/**
 * Extrator do Kwai.
 *
 * O link curto (k.kwai.com) é resolvido seguindo os redirecionamentos até a
 * página do vídeo. A página entrega o arquivo dentro de blocos JSON
 * (photoUrl / mainMvUrls / manifest) e também em tags <video>.
 */
class KwaiResolver : PlatformResolver {

    override suspend fun resolve(url: String): ResolvedMedia {
        val errors = mutableListOf<String>()
        val attempts = buildList {
            add(url to Http.DESKTOP_UA)
            if (url.contains("://www.kwai.com")) {
                add(url.replace("://www.kwai.com", "://m.kwai.com") to Http.MOBILE_UA)
            }
            if (url.contains("://k.kwai.com")) {
                add(url.replace("://k.kwai.com", "://m.kwai.com") to Http.MOBILE_UA)
            }
        }

        for ((target, userAgent) in attempts) {
            val result = runCatching { fetch(target, userAgent) }
            result.getOrNull()?.let { return it }
            result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }?.let { errors += it }
        }

        throw ResolveException(
            message = "Não encontrei o vídeo neste link do Kwai. O vídeo pode ser privado, " +
                "ter sido removido ou o link não é de um vídeo.",
            details = errors.distinct().joinToString(" | ").takeIf { it.isNotBlank() }
        )
    }

    private suspend fun fetch(url: String, userAgent: String): ResolvedMedia {
        val (finalUrl, html) = Http.getWithFinalUrl(url, Html.browserHeaders(userAgent))

        val variants = extractVariants(html)
        if (variants.isEmpty()) throw ResolveException("Página do Kwai sem arquivo de vídeo.")

        val ogTitle = Html.meta(html, "og:title")
        val caption = Html.jsonStrings(html, "caption").firstOrNull()
            ?: Html.jsonStrings(html, "title").firstOrNull()
        val author = Regex("""kwai\.com/@([^/?#]+)""", RegexOption.IGNORE_CASE)
            .find(finalUrl)
            ?.groupValues
            ?.getOrNull(1)
            ?: Html.jsonStrings(html, "userName").firstOrNull()
            ?: Html.instagramHandle(ogTitle)
        val duration = Html.meta(html, "og:video:duration")?.toDoubleOrNull()
            ?: Html.jsonNumber(html, "duration")?.let { it / 1000.0 }

        return ResolvedMedia(
            platform = Platform.KWAI,
            sourceUrl = LinkParser.clean(finalUrl),
            title = (Html.cleanTitle(caption) ?: Html.cleanTitle(ogTitle))?.take(140),
            author = author,
            thumbnailUrl = Html.meta(html, "og:image")
                ?: Html.jsonStrings(html, "coverUrl").firstOrNull()
                ?: Html.jsonStrings(html, "poster").firstOrNull(),
            durationMillis = duration?.let { (it * 1000).toLong() },
            variants = variants
        )
    }

    private fun extractVariants(html: String): List<VideoVariant> {
        val mainUrl = mainVideoUrl(html) ?: return emptyList()
        return listOf(VideoVariant(url = mainUrl))
    }

    /**
     * A página traz vários players (o vídeo pedido e recomendações). O vídeo
     * principal é o primeiro `<a-video-player>`; por isso não usamos qualquer
     * URL .mp4 encontrada na página.
     */
    private fun mainVideoUrl(html: String): String? {
        val tags = Regex("""<a-video-player\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(html)
        for (tag in tags) {
            val src = Html.attr(tag.value, "src") ?: continue
            val url = Html.unescape(src)
            if (Html.isVideoUrl(url)) return url
        }

        val fromJson = buildList {
            addAll(Html.jsonStrings(html, "photoUrl"))
            addAll(Html.jsonStrings(html, "mainMvUrl"))
            addAll(Html.jsonStrings(html, "srcNoMark"))
            addAll(Html.jsonStrings(html, "videoUrl"))
            addAll(Html.jsonStrings(html, "playUrl"))
            addAll(Html.videoTagUrls(html))
            Html.meta(html, "og:video")?.let { add(it) }
            Html.meta(html, "og:video:secure_url")?.let { add(it) }
        }.map { Html.unescape(it) }
            .firstOrNull { Html.isVideoUrl(it) }
        if (fromJson != null) return fromJson

        return Html.mp4Urls(html).firstOrNull { Html.isVideoUrl(it) }
    }
}
