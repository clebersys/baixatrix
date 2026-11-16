package com.cleber.baixatrix.data.model

import com.cleber.baixatrix.util.Platform

/** Uma qualidade/arquivo de vídeo encontrada para o mesmo post. */
data class VideoVariant(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val bitrate: Long? = null,
    val mimeType: String = "video/mp4"
) {
    val label: String
        get() = when {
            height != null && height > 0 -> "${height}p"
            width != null && width > 0 -> "${width}px"
            else -> "Original"
        }

    val pixelCount: Long
        get() = (width ?: 0).toLong() * (height ?: 0).toLong()
}

/** Resultado da extração de um link: tudo que a tela precisa para pré-visualizar e baixar. */
data class ResolvedMedia(
    val platform: Platform,
    val sourceUrl: String,
    val title: String?,
    val author: String?,
    val thumbnailUrl: String?,
    val durationMillis: Long?,
    val variants: List<VideoVariant>
) {
    /** Melhor arquivo disponível (maior resolução, depois maior bitrate). */
    val bestVariant: VideoVariant
        get() = variants.maxWithOrNull(
            compareBy<VideoVariant> { it.pixelCount }.thenBy { it.bitrate ?: 0L }
        ) ?: variants.first()

    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "Vídeo do ${platform.label}"

    val displayAuthor: String?
        get() = author?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("@")) it else "@$it" }

    val aspectRatio: Float
        get() {
            val variant = variants.firstOrNull()
            val w = variant?.width ?: 0
            val h = variant?.height ?: 0
            return if (w > 0 && h > 0) w.toFloat() / h.toFloat() else 9f / 16f
        }

    val isVertical: Boolean
        get() = aspectRatio < 1f

    /** Cabeçalhos necessários para o CDN entregar o arquivo (player e download). */
    fun requestHeaders(): Map<String, String> {
        val referer = when (platform) {
            Platform.INSTAGRAM -> "https://www.instagram.com/"
            Platform.KWAI -> "https://www.kwai.com/"
            Platform.OTHER -> sourceUrl
        }
        return mapOf(
            "Referer" to referer,
            "User-Agent" to com.cleber.baixatrix.net.Http.DESKTOP_UA
        )
    }
}

/**
 * Falha de extração com uma mensagem amigável e, opcionalmente, um detalhe
 * técnico que a interface mostra apenas sob demanda.
 */
class ResolveException(
    message: String,
    val details: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
