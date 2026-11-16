package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.model.VideoVariant
import com.cleber.baixatrix.net.Http
import com.cleber.baixatrix.util.Html
import com.cleber.baixatrix.util.LinkParser
import com.cleber.baixatrix.util.Platform

/**
 * Extrator do Instagram para posts e Reels públicos. Não usa cookies de sessão.
 *
 * Ordem das tentativas:
 *  1. página de embed com User-Agent de iPhone — o Instagram entrega os dados
 *     do post renderizados no servidor para esse agente (rota mais confiável);
 *  2. mesma página com User-Agent de robô de indexação;
 *  3. tags Open Graph da página pública do post.
 */
class InstagramResolver : PlatformResolver {

    override suspend fun resolve(url: String): ResolvedMedia {
        val shortcode = LinkParser.instagramShortcode(url)
            ?: throw ResolveException("Não encontrei o código do post neste link do Instagram.")
        val kind = LinkParser.instagramKind(url)

        val errors = mutableListOf<String>()
        val variants = LinkedHashMap<String, VideoVariant>()
        var thumbnail: String? = null
        var title: String? = null
        var author: String? = null
        var durationMillis: Long? = null

        fun merge(partial: Partial?) {
            if (partial == null) return
            if (thumbnail == null) thumbnail = partial.thumbnail
            if (title == null) title = partial.title
            if (author == null) author = partial.author
            if (durationMillis == null) durationMillis = partial.durationMillis
            partial.variants.forEach { variant ->
                if (!variant.url.startsWith("http")) return@forEach
                val key = variant.url.substringBefore('?')
                if (!variants.containsKey(key)) variants[key] = variant
            }
        }

        // 1 e 2) Página de embed
        runCatching { viaEmbed(shortcode, kind) }
            .onSuccess { merge(it) }
            .onFailure { errors += it.message.orEmpty() }

        // 3) Página pública normal (Open Graph)
        if (variants.isEmpty()) {
            runCatching {
                val html = Http.get(
                    "https://www.instagram.com/$kind/$shortcode/",
                    Html.browserHeaders()
                )
                parseOpenGraph(html)
            }.onSuccess { merge(it) }
                .onFailure { errors += it.message.orEmpty() }
        }

        val ordered = variants.values.sortedWith(
            compareByDescending<VideoVariant> { it.pixelCount }.thenByDescending { it.bitrate ?: 0L }
        )

        if (ordered.isEmpty()) {
            throw ResolveException(
                message = "O Instagram não disponibilizou este vídeo público. " +
                    "O BaixaTrix só oferece suporte a posts públicos; posts privados ou removidos não são suportados.",
                details = errors.filter { it.isNotBlank() }.distinct().joinToString(" | ")
                    .takeIf { it.isNotBlank() }
            )
        }

        return ResolvedMedia(
            platform = Platform.INSTAGRAM,
            sourceUrl = LinkParser.clean(url),
            title = title,
            author = author,
            thumbnailUrl = thumbnail,
            durationMillis = durationMillis,
            variants = ordered
        )
    }

    // region Rotas de extração

    /**
     * A página de embed é a rota que ainda entrega o JSON completo do post sem
     * login — mas apenas para determinados User-Agents (iOS e robôs).
     */
    private suspend fun viaEmbed(shortcode: String, kind: String): Partial {
        var lastError: Exception? = null

        for (userAgent in listOf(Http.IPHONE_UA, Http.GOOGLEBOT_UA)) {
            val attempt = runCatching {
                val html = Http.get(
                    "https://www.instagram.com/$kind/$shortcode/embed/captioned/",
                    embedHeaders(userAgent)
                )
                val partial = parseEmbed(html)
                if (partial.variants.isEmpty()) {
                    throw ResolveException("A página de embed veio sem o arquivo de vídeo.")
                }
                partial
            }
            attempt.getOrNull()?.let { return it }
            lastError = attempt.exceptionOrNull() as? Exception
        }

        throw lastError ?: ResolveException("Não foi possível ler a página do Instagram.")
    }

    private fun parseEmbed(html: String): Partial {
        val normalized = html.replace("\\\"", "\"")
        val variants = mutableListOf<VideoVariant>()

        val videoRegex = Regex("\"video_url\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        for (match in videoRegex.findAll(normalized)) {
            val videoUrl = Html.unescape(match.groupValues[1])
            if (!videoUrl.startsWith("http")) continue

            // As dimensões aparecem logo antes da URL dentro do nó de mídia.
            val before = normalized.substring(maxOf(0, match.range.first - 1500), match.range.first)
            val dimensions = Regex("\"height\"\\s*:\\s*(\\d+)\\s*,\\s*\"width\"\\s*:\\s*(\\d+)")
                .findAll(before)
                .lastOrNull()

            variants += VideoVariant(
                url = videoUrl,
                width = dimensions?.groupValues?.getOrNull(2)?.toIntOrNull(),
                height = dimensions?.groupValues?.getOrNull(1)?.toIntOrNull()
            )
        }

        val thumbnail = Html.jsonStrings(normalized, "display_url").firstOrNull()
        val duration = Html.jsonNumber(normalized, "video_duration")
        val author = Regex("\"owner\"\\s*:\\s*\\{[^{}]{0,800}?\"username\"\\s*:\\s*\"([^\"]+)\"")
            .find(normalized)?.groupValues?.getOrNull(1)
        val caption = Regex("\"edge_media_to_caption\"[\\s\\S]{0,600}?\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.){0,600})\"")
            .find(normalized)?.groupValues?.getOrNull(1)?.let { Html.unescape(it) }

        return Partial(
            variants = variants,
            thumbnail = thumbnail,
            title = Html.cleanTitle(caption)?.take(140),
            author = author,
            durationMillis = duration?.let { (it * 1000).toLong() }
        )
    }

    private fun parseOpenGraph(html: String): Partial {
        val urls = buildList {
            Html.meta(html, "og:video")?.let { add(it) }
            Html.meta(html, "og:video:secure_url")?.let { add(it) }
            addAll(Html.mp4Urls(html))
        }

        val ogTitle = Html.meta(html, "og:title")

        return Partial(
            variants = urls.filter { it.startsWith("http") }
                .distinctBy { it.substringBefore('?') }
                .map { VideoVariant(url = it) },
            thumbnail = Html.meta(html, "og:image"),
            title = Html.cleanTitle(ogTitle)?.take(140),
            author = Html.instagramHandle(ogTitle)
        )
    }

    // endregion

    private fun embedHeaders(userAgent: String): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "pt-BR,pt;q=0.9,en;q=0.8",
        "Referer" to "https://www.instagram.com/"
    )


    private data class Partial(
        val variants: List<VideoVariant> = emptyList(),
        val thumbnail: String? = null,
        val title: String? = null,
        val author: String? = null,
        val durationMillis: Long? = null
    )
}
