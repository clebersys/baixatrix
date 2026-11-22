package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.util.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class YoutubeResolverTest {

    @Test
    fun `converte resposta valida do backend em midia youtube`() {
        val media = parseYoutubeResponse(
            """{"status":"ok","title":"Vídeo autorizado","author":"Canal","thumbnail":"https://i.ytimg.com/vi/id/hqdefault.jpg","duration":12.5,"variants":[{"url":"https://rr1---sn.googlevideo.com/video.mp4","height":720,"width":1280,"bitrate":1500,"mimeType":"video/mp4"}]}""",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )

        assertEquals(Platform.YOUTUBE, media.platform)
        assertEquals("Vídeo autorizado", media.title)
        assertEquals(12_500L, media.durationMillis)
        assertEquals("720p", media.variants.single().label)
    }

    @Test
    fun `recusa URL de arquivo fora do CDN do YouTube`() {
        val response = """{"status":"ok","variants":[{"url":"https://evil.example/video.mp4","height":720,"mimeType":"video/mp4"}]}"""

        assertThrows(ResolveException::class.java) {
            parseYoutubeResponse(response, "https://youtu.be/dQw4w9WgXcQ")
        }
    }

    @Test
    fun `ignora thumbnail fora do dominio youtube`() {
        val response = """{"status":"ok","thumbnail":"https://evil.example/pixel","variants":[{"url":"https://rr1.googlevideo.com/video.mp4","height":360,"mimeType":"video/mp4"}]}"""

        val media = parseYoutubeResponse(response, "https://youtu.be/dQw4w9WgXcQ")

        assertNull(media.thumbnailUrl)
    }
}
