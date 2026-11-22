package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.util.Platform
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.Image.ResolutionLevel
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.services.youtube.ItagItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class YoutubeResolverTest {

    @Test
    fun `converte metadata local e organiza apenas MP4 progressivo com audio`() {
        val sourceUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val info = streamInfo(
            sourceUrl,
            listOf(
                video("720", "https://rr1.googlevideo.com/video-720", MediaFormat.MPEG_4),
                video("1080", "https://rr1.googlevideo.com/video-only", MediaFormat.MPEG_4, videoOnly = true),
                video("360", "https://rr1.googlevideo.com/video-360", MediaFormat.MPEG_4),
                video("1440", "https://rr1.googlevideo.com/video-webm", MediaFormat.WEBM),
                video(
                    "480",
                    "https://rr1.googlevideo.com/video-dash",
                    MediaFormat.MPEG_4,
                    delivery = DeliveryMethod.DASH
                )
            )
        )

        val media = mapYoutubeInfo(info, sourceUrl)

        assertEquals(Platform.YOUTUBE, media.platform)
        assertEquals("Vídeo público", media.title)
        assertEquals("Canal", media.author)
        assertEquals(12_000L, media.durationMillis)
        assertEquals(listOf("720p", "360p"), media.variants.map { it.label })
        assertEquals("https://i.ytimg.com/vi/id/hqdefault.jpg", media.thumbnailUrl)
    }

    @Test
    fun `falha com orientacao quando so existem streams nao compativeis`() {
        val sourceUrl = "https://youtu.be/dQw4w9WgXcQ"
        val info = streamInfo(
            sourceUrl,
            listOf(video("720", "https://rr1.googlevideo.com/video", MediaFormat.MPEG_4, videoOnly = true))
        )

        assertThrows(ResolveException::class.java) {
            mapYoutubeInfo(info, sourceUrl)
        }
    }

    @Test
    fun `combina a variante de video com a faixa AAC sem baixar formatos separados como saida`() {
        val sourceUrl = "https://youtu.be/dQw4w9WgXcQ"
        val info = streamInfo(
            sourceUrl,
            videoOnlyStreams = listOf(
                video(
                    "720",
                    "https://rr1.googlevideo.com/video-only",
                    MediaFormat.MPEG_4,
                    videoOnly = true
                )
            ),
            audioStreams = listOf(
                audio("https://rr1.googlevideo.com/audio-aac", MediaFormat.M4A),
                audio("https://rr1.googlevideo.com/audio-opus", MediaFormat.WEBMA_OPUS)
            )
        )

        val media = mapYoutubeInfo(info, sourceUrl)

        assertEquals("720p", media.variants.single().label)
        assertEquals("https://rr1.googlevideo.com/audio-aac", media.variants.single().audioUrl)
    }

    @Test
    fun `recusa CDN fora do YouTube e ignora thumbnail externa`() {
        val sourceUrl = "https://youtu.be/dQw4w9WgXcQ"
        val externalThumbnail = Image(
            "https://evil.example/pixel",
            720,
            1280,
            ResolutionLevel.HIGH
        )
        val info = streamInfo(
            sourceUrl,
            listOf(video("360", "https://evil.example/video", MediaFormat.MPEG_4)),
            thumbnails = listOf(externalThumbnail)
        )

        assertThrows(ResolveException::class.java) { mapYoutubeInfo(info, sourceUrl) }
    }

    private fun streamInfo(
        sourceUrl: String,
        streams: List<VideoStream> = emptyList(),
        videoOnlyStreams: List<VideoStream> = emptyList(),
        audioStreams: List<AudioStream> = emptyList(),
        thumbnails: List<Image> = listOf(
            Image("https://i.ytimg.com/vi/id/hqdefault.jpg", 720, 1280, ResolutionLevel.HIGH)
        )
    ) = StreamInfo(
        0,
        sourceUrl,
        sourceUrl,
        StreamType.VIDEO_STREAM,
        "dQw4w9WgXcQ",
        "Vídeo público",
        0
    ).apply {
        setVideoStreams(streams)
        setVideoOnlyStreams(videoOnlyStreams)
        setAudioStreams(audioStreams)
        setThumbnails(thumbnails)
        setUploaderName("Canal")
        setDuration(12)
    }

    private fun video(
        resolution: String,
        url: String,
        format: MediaFormat,
        videoOnly: Boolean = false,
        delivery: DeliveryMethod = DeliveryMethod.PROGRESSIVE_HTTP
    ): VideoStream {
        val itag = ItagItem(
            resolution.toIntOrNull() ?: 18,
            if (videoOnly) ItagItem.ItagType.VIDEO_ONLY else ItagItem.ItagType.VIDEO,
            format,
            "${resolution}p"
        ).apply { setCodec("avc1.64001F") }
        return VideoStream.Builder()
            .setId("itag-$resolution-${format.suffix}")
            .setContent(url, true)
            .setMediaFormat(format)
            .setItagItem(itag)
            .setDeliveryMethod(delivery)
            .setIsVideoOnly(videoOnly)
            .setResolution("${resolution}p")
            .build()
    }

    private fun audio(url: String, format: MediaFormat): AudioStream {
        val codec = if (format == MediaFormat.M4A) "mp4a.40.2" else "opus"
        val itag = ItagItem(140, ItagItem.ItagType.AUDIO, format, 128)
            .apply { setCodec(codec) }
        return AudioStream.Builder()
            .setId("audio-${format.suffix}")
            .setContent(url, true)
            .setMediaFormat(format)
            .setItagItem(itag)
            .setAverageBitrate(128)
            .build()
    }
}
