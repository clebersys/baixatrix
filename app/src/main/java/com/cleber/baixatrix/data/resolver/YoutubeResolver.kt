package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.model.VideoVariant
import com.cleber.baixatrix.util.LinkParser
import com.cleber.baixatrix.util.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException

/** Extracts public YouTube video streams locally with NewPipe Extractor. */
class YoutubeResolver : PlatformResolver {

    override suspend fun resolve(url: String): ResolvedMedia {
        if (LinkParser.platform(url) != Platform.YOUTUBE) {
            throw ResolveException("Cole um link válido de vídeo do YouTube.")
        }
        if (NewPipe.getDownloader() == null) {
            throw ResolveException("O extrator local do YouTube não foi inicializado.")
        }

        return withContext(Dispatchers.IO) {
            val info = try {
                StreamInfo.getInfo(url)
            } catch (error: IOException) {
                throw ResolveException(
                    "Não consegui acessar o YouTube. Verifique sua conexão e tente novamente.",
                    cause = error
                )
            } catch (error: SignInConfirmNotBotException) {
                throw ResolveException(
                    "O YouTube recusou a extração automática nesta conexão. Tente novamente mais tarde.",
                    cause = error
                )
            } catch (error: ExtractionException) {
                throw ResolveException(
                    "Não consegui extrair este vídeo público. O YouTube pode ter alterado o formato ou o vídeo pode estar indisponível.",
                    cause = error
                )
            }

            mapYoutubeInfo(info, url)
        }
    }
}

internal fun mapYoutubeInfo(info: StreamInfo, sourceUrl: String): ResolvedMedia {
    val muxedVariants = info.videoStreams
        .asSequence()
        .filter { stream ->
                stream.isUrl() &&
                !stream.isVideoOnly() &&
                stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                stream.format == MediaFormat.MPEG_4 &&
                isMuxerCompatibleVideoCodec(stream.codec)
        }
        .mapNotNull { stream -> toProgressiveVariant(stream, audioUrl = null) }
        .sortedWith(
            compareByDescending<VideoVariant> { it.height ?: 0 }
                .thenByDescending { it.bitrate ?: 0L }
        )
        .distinctBy { it.height ?: 0 }
        .toList()

    val audioUrl = info.audioStreams
        .asSequence()
        .filter { stream ->
                stream.isUrl() &&
                stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                stream.format == MediaFormat.M4A &&
                isMuxerCompatibleAudioCodec(stream.codec)
        }
        .sortedByDescending(AudioStream::getAverageBitrate)
        .mapNotNull { audioStream -> safeYoutubeMediaUrl(audioStream.content) }
        .firstOrNull()

    val variants = if (muxedVariants.isNotEmpty()) {
        muxedVariants
    } else {
        val separateAudioUrl = audioUrl
            ?: throw ResolveException("O YouTube não ofereceu uma faixa AAC compatível.")
        info.videoOnlyStreams
            .asSequence()
            .filter { stream ->
                stream.isUrl() &&
                    stream.isVideoOnly() &&
                    stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    stream.format == MediaFormat.MPEG_4 &&
                    isMuxerCompatibleVideoCodec(stream.codec)
            }
            .mapNotNull { stream -> toProgressiveVariant(stream, separateAudioUrl) }
            .sortedWith(
                compareByDescending<VideoVariant> { it.height ?: 0 }
                    .thenByDescending { it.bitrate ?: 0L }
            )
            .distinctBy { it.height ?: 0 }
            .toList()
    }.take(MAX_VARIANTS)

    if (variants.isEmpty()) {
        throw ResolveException(
            "Este vídeo não oferece MP4 progressivo ou faixas H.264/AAC compatíveis. Tente outro vídeo público."
        )
    }

    val thumbnailUrl = info.thumbnails
        .maxByOrNull { it.height }
        ?.url
        ?.toHttpUrlOrNull()
        ?.takeIf { imageUrl ->
            imageUrl.scheme == "https" &&
                imageUrl.username.isEmpty() &&
                imageUrl.password.isEmpty() &&
                (imageUrl.host == "ytimg.com" || imageUrl.host.endsWith(".ytimg.com"))
        }
        ?.toString()

    return ResolvedMedia(
        platform = Platform.YOUTUBE,
        sourceUrl = sourceUrl,
        title = info.name.takeIf(String::isNotBlank)?.take(140),
        author = info.uploaderName.takeIf(String::isNotBlank)?.take(120),
        thumbnailUrl = thumbnailUrl,
        durationMillis = info.duration.takeIf { it >= 0 }?.times(1_000L),
        variants = variants
    )
}

private fun toProgressiveVariant(stream: VideoStream, audioUrl: String?): VideoVariant? {
    val mediaUrl = safeYoutubeMediaUrl(stream.content) ?: return null

    val height = stream.height.takeIf { it > 0 }
        ?: stream.getResolution().removeSuffix("p").toIntOrNull()?.takeIf { it > 0 }
    return VideoVariant(
        url = mediaUrl.toString(),
        width = stream.width.takeIf { it > 0 },
        height = height,
        bitrate = stream.bitrate.takeIf { it > 0 }?.toLong(),
        mimeType = MediaFormat.MPEG_4.mimeType,
        audioUrl = audioUrl
    )
}

private fun safeYoutubeMediaUrl(value: String): String? {
    val url = value.toHttpUrlOrNull() ?: return null
    if (
        url.scheme != "https" ||
        url.username.isNotEmpty() ||
        url.password.isNotEmpty() ||
        url.port != 443 ||
        (url.host != "googlevideo.com" && !url.host.endsWith(".googlevideo.com"))
    ) {
        return null
    }
    return url.toString()
}

private fun isMuxerCompatibleVideoCodec(codec: String?): Boolean {
    if (codec.isNullOrBlank()) return false
    val normalized = codec.lowercase()
    return normalized.startsWith("avc1") ||
        normalized.startsWith("h264") ||
        normalized.startsWith("hev1") ||
        normalized.startsWith("hvc1")
}

private fun isMuxerCompatibleAudioCodec(codec: String?): Boolean =
    codec?.lowercase()?.startsWith("mp4a") == true

private const val MAX_VARIANTS = 20
