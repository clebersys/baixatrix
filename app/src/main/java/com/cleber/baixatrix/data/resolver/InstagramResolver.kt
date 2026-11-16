package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.model.VideoVariant
import com.cleber.baixatrix.net.Http
import com.cleber.baixatrix.util.Html
import com.cleber.baixatrix.util.LinkParser
import com.cleber.baixatrix.util.Platform
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

/**
 * Extrator do Instagram (posts e Reels públicos).
 *
 * Ordem das tentativas:
 *  1. página de embed com User-Agent de iPhone — o Instagram entrega os dados
 *     do post renderizados no servidor para esse agente (rota mais confiável);
 *  2. mesma página com User-Agent de robô de indexação;
 *  3. API interna por shortcode (funciona quando há cookie de sessão);
 *  4. API interna por media id;
 *  5. tags Open Graph da página do post.
 *
 * O cookie sessionid (opcional, em Ajustes) é necessário quando o Instagram
 * passa a exigir login também nessa rota.
 */
class InstagramResolver(private val cookie: String?) : PlatformResolver {

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

        // 3) API interna por shortcode (com cookie)
        if (variants.isEmpty() && !cookie.isNullOrBlank()) {
            runCatching { viaApiShortcode(shortcode) }
                .onSuccess { merge(it) }
                .onFailure { errors += it.message.orEmpty() }
        }

        // 4) API interna por media id (com cookie)
        if (variants.isEmpty() && !cookie.isNullOrBlank()) {
            runCatching { viaApiMediaId(shortcode) }
                .onSuccess { merge(it) }
                .onFailure { errors += it.message.orEmpty() }
        }

        // 5) Página normal (Open Graph)
        if (variants.isEmpty()) {
            runCatching {
                val html = Http.get("https://www.instagram.com/$kind/$shortcode/", pageHeaders())
                parseOpenGraph(html)
            }.onSuccess { merge(it) }
                .onFailure { errors += it.message.orEmpty() }
        }

        val ordered = variants.values.sortedWith(
            compareByDescending<VideoVariant> { it.pixelCount }.thenByDescending { it.bitrate ?: 0L }
        )

        if (ordered.isEmpty()) {
            throw ResolveException(
                message = "O Instagram não liberou este vídeo sem login. " +
                    "Em Ajustes, cole o cookie sessionid de uma conta sua ou configure um servidor " +
                    "de extração. Posts privados e removidos também não são acessíveis.",
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

    private suspend fun viaApiShortcode(shortcode: String): Partial {
        val json = Http.get(
            "https://www.instagram.com/api/v1/media/shortcode/$shortcode/info/?__d=dis",
            apiHeaders()
        )
        return parseApiJson(json)
    }

    private suspend fun viaApiMediaId(shortcode: String): Partial {
        val mediaId = mediaIdFromShortcode(shortcode)
        val json = Http.get(
            "https://i.instagram.com/api/v1/media/$mediaId/info/",
            apiHeaders() + mapOf("X-IG-App-Locale" to "pt_BR")
        )
        return parseApiJson(json)
    }

    private fun parseApiJson(json: String): Partial {
        val root = JSONObject(json)
        if (root.optString("status") == "fail") {
            throw ResolveException(root.optString("message").ifBlank { "Instagram recusou a requisição." })
        }
        val items = root.optJSONArray("items")
            ?: throw ResolveException("Resposta inesperada do Instagram.")
        if (items.length() == 0) throw ResolveException("Post não encontrado.")

        val item = firstVideoItem(items)
        val variants = mutableListOf<VideoVariant>()
        val versions = item.optJSONArray("video_versions")
        if (versions != null) {
            for (index in 0 until versions.length()) {
                val version = versions.optJSONObject(index) ?: continue
                val videoUrl = version.optString("url")
                if (videoUrl.isBlank()) continue
                variants += VideoVariant(
                    url = videoUrl,
                    width = version.optInt("width", 0).takeIf { it > 0 },
                    height = version.optInt("height", 0).takeIf { it > 0 },
                    bitrate = version.optLong("bitrate", 0L).takeIf { it > 0 }
                )
            }
        }

        val thumbnail = item.optJSONObject("image_versions2")
            ?.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optString("url")
            ?.takeIf { it.isNotBlank() }
            ?: item.optString("thumbnail_url").takeIf { it.isNotBlank() }

        val caption = item.optJSONObject("caption")?.optString("text")?.takeIf { it.isNotBlank() }
        val author = item.optJSONObject("user")?.optString("username")?.takeIf { it.isNotBlank() }
        val duration = item.optDouble("video_duration", 0.0).takeIf { it > 0 }

        return Partial(
            variants = variants,
            thumbnail = thumbnail,
            title = Html.cleanTitle(caption)?.take(140),
            author = author,
            durationMillis = duration?.let { (it * 1000).toLong() }
        )
    }

    // endregion

    private fun firstVideoItem(items: JSONArray): JSONObject {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            if (item.has("video_versions")) return item
            val carousel = item.optJSONArray("carousel_media")
            if (carousel != null) {
                for (childIndex in 0 until carousel.length()) {
                    val child = carousel.optJSONObject(childIndex) ?: continue
                    if (child.has("video_versions")) return child
                }
            }
        }
        return items.getJSONObject(0)
    }

    private fun embedHeaders(userAgent: String): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "pt-BR,pt;q=0.9,en;q=0.8",
        "Referer" to "https://www.instagram.com/"
    )

    private fun apiHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "User-Agent" to Http.INSTAGRAM_APP_UA,
            "X-IG-App-ID" to Http.INSTAGRAM_APP_ID,
            "Accept" to "*/*",
            "Accept-Language" to "pt-BR,pt;q=0.9,en;q=0.8"
        )
        cookie?.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = normalizeCookie(it) }
        return headers
    }

    private fun pageHeaders(): Map<String, String> {
        val headers = Html.browserHeaders().toMutableMap()
        cookie?.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = normalizeCookie(it) }
        return headers
    }

    private fun normalizeCookie(value: String): String =
        if (value.contains('=')) value else "sessionid=$value"

    /** Converte o shortcode da URL no media id numérico usado pela API. */
    private fun mediaIdFromShortcode(shortcode: String): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        var value = BigInteger.ZERO
        for (character in shortcode) {
            val index = alphabet.indexOf(character)
            if (index < 0) return shortcode
            value = value.multiply(BigInteger.valueOf(64)).add(BigInteger.valueOf(index.toLong()))
        }
        return value.toString()
    }

    private data class Partial(
        val variants: List<VideoVariant> = emptyList(),
        val thumbnail: String? = null,
        val title: String? = null,
        val author: String? = null,
        val durationMillis: Long? = null
    )
}
