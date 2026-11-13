package com.cleber.baixavideo.data.resolver

import com.cleber.baixavideo.data.model.ResolveException
import com.cleber.baixavideo.data.model.ResolvedMedia
import com.cleber.baixavideo.data.model.VideoVariant
import com.cleber.baixavideo.net.Http
import com.cleber.baixavideo.util.LinkParser
import com.cleber.baixavideo.util.Platform
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Resolver remoto opcional.
 *
 * Se o usuário configurar um servidor próprio (por exemplo, um wrapper de
 * yt-dlp), o app envia o link e espera um JSON simples:
 *
 *   { "url": "https://.../video.mp4",
 *     "title": "...", "author": "...", "thumbnail": "...",
 *     "duration": 12.5,
 *     "variants": [ { "url": "...", "height": 720 } ] }
 *
 * Também aceita respostas no estilo Cobalt ({ "status": "redirect", "url": ... }).
 * Isso mantém o app funcionando mesmo quando Instagram/Kwai mudam o layout.
 */
class RemoteResolver(private val baseUrl: String) : PlatformResolver {

    override suspend fun resolve(url: String): ResolvedMedia {
        val endpoint = buildEndpoint(baseUrl, url)
        val body = Http.get(
            endpoint,
            mapOf(
                "Accept" to "application/json, text/plain, */*",
                "User-Agent" to Http.DESKTOP_UA
            )
        ).trim()

        if (body.startsWith("http")) {
            return simple(body)
        }

        val root = runCatching { JSONObject(body) }.getOrElse {
            throw ResolveException("O servidor de extração não devolveu um JSON válido.")
        }

        val status = root.optString("status").lowercase()
        if (status == "error") {
            throw ResolveException(
                root.optString("text").ifBlank { "O servidor de extração recusou o link." }
            )
        }

        val variants = mutableListOf<VideoVariant>()
        val array = root.optJSONArray("variants") ?: root.optJSONArray("medias") ?: root.optJSONArray("links")
        if (array != null) {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val entryUrl = firstString(entry, "url", "video", "video_url", "src") ?: continue
                variants += VideoVariant(
                    url = entryUrl,
                    width = entry.optInt("width", 0).takeIf { it > 0 },
                    height = entry.optInt("height", 0).takeIf { it > 0 },
                    bitrate = entry.optLong("bitrate", 0L).takeIf { it > 0 },
                    mimeType = entry.optString("mimeType", entry.optString("type", "video/mp4"))
                )
            }
        }

        val single = firstString(root, "url", "video", "video_url", "src")
            ?: root.optJSONObject("data")?.let { firstString(it, "url", "video", "video_url", "src") }
        if (single != null) {
            variants += VideoVariant(url = single)
        }

        if (variants.isEmpty()) {
            throw ResolveException(
                root.optString("message")
                    .ifBlank { "O servidor de extração não retornou nenhum arquivo de vídeo." }
            )
        }

        val duration = root.optDouble("duration", 0.0).takeIf { it > 0 }

        return ResolvedMedia(
            platform = LinkParser.platform(url),
            sourceUrl = url,
            title = firstString(root, "title", "caption", "filename")?.take(140),
            author = firstString(root, "author", "username", "uploader"),
            thumbnailUrl = firstString(root, "thumbnail", "thumb", "poster"),
            durationMillis = duration?.let { (it * 1000).toLong() },
            variants = variants
                .distinctBy { it.url.substringBefore('?') }
                .sortedByDescending { it.pixelCount }
        )
    }

    private fun simple(videoUrl: String) = ResolvedMedia(
        platform = Platform.OTHER,
        sourceUrl = "",
        title = null,
        author = null,
        thumbnailUrl = null,
        durationMillis = null,
        variants = listOf(VideoVariant(videoUrl))
    )

    private fun firstString(json: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            val value = json.optString(key)
            if (value.isNotBlank() && value != "null") return value
        }
        return null
    }

    private fun buildEndpoint(base: String, url: String): String {
        val separator = if (base.contains('?')) '&' else '?'
        return base + separator + "url=" + URLEncoder.encode(url, "UTF-8")
    }
}

/** Aceita variantes de resposta em que o JSON vem dentro de uma lista. */
internal fun JSONArray.firstObject(): JSONObject? =
    if (length() > 0) optJSONObject(0) else null
