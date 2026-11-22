package com.cleber.baixatrix

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.processing.YoutubeStreamMuxer
import com.cleber.baixatrix.data.resolver.YoutubeResolver
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Teste manual de rede real, executado apenas com `connectedAndroidTest`.
 *
 * Verifica a extração local em um aparelho/emulador: metadados, variantes,
 * acesso HTTPS ao CDN e a combinação H.264/HEVC + AAC. Não roda no host.
 */
@RunWith(AndroidJUnit4::class)
class YoutubeLiveDeviceTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val urls = listOf(
        "https://www.youtube.com/watch?v=jNQXAC9IVRw",
        "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
        "https://youtu.be/dQw4w9WgXcQ",
        "https://www.youtube.com/shorts/9bZkp7q19f0"
    )

    @Test
    fun extraiEbaixaQuatroVideosPublicos() = runBlocking {
        val medias = urls.map { url ->
            val media = YoutubeResolver().resolve(url)
            report(url, media)
            assertTrue("sem variantes para $url", media.variants.isNotEmpty())
            assertTrue("variante sem URL para $url", media.bestVariant.url.isNotBlank())

            val variant = media.bestVariant
            assertReadable(variant.url, media, "vídeo")
            variant.audioUrl?.let { assertReadable(it, media, "áudio") }
            media
        }

        assertTrue("esperava 4 vídeos resolvidos", medias.size == 4)
    }

    @Test
    fun combinaFaixasDeUmVideoCurto() = runBlocking {
        val url = "https://www.youtube.com/watch?v=jNQXAC9IVRw"
        val media = YoutubeResolver().resolve(url)
        val variant = media.variants
            .filter { it.audioUrl != null }
            .minByOrNull { it.height ?: Int.MAX_VALUE }
            ?: error("Nenhuma variante com faixa de áudio separada para o teste de mux")

        val output = File.createTempFile("live_mux_", ".mp4")
        val startedAt = System.currentTimeMillis()
        try {
            YoutubeStreamMuxer.mux(
                videoUrl = variant.url,
                audioUrl = variant.audioUrl!!,
                outputFile = output,
                headers = media.requestHeaders()
            )
            val elapsed = System.currentTimeMillis() - startedAt
            val tracks = inspectTracks(output)
            log("MUX ok em ${elapsed}ms, ${output.length()} bytes, faixas=$tracks")
            assertTrue("mux sem faixa de vídeo", tracks.any { it.startsWith("video/") })
            assertTrue("mux sem faixa de áudio", tracks.any { it.startsWith("audio/") })
        } finally {
            output.delete()
        }
    }

    private fun report(url: String, media: ResolvedMedia) {
        log(
            "RESOLVE $url -> plataforma=${media.platform} titulo=${media.title} " +
                "duracao=${media.durationMillis} variantes=" +
                media.variants.joinToString { v -> "${v.label}${if (v.audioUrl != null) "+aac" else ""}" }
        )
    }

    private fun assertReadable(url: String, media: ResolvedMedia, kind: String) {
        val request = Request.Builder()
            .url(url)
            .apply { media.requestHeaders().forEach { (name, value) -> header(name, value) } }
            .header("Range", "bytes=0-65535")
            .build()

        client.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes()?.size ?: 0
            log("FETCH $kind HTTP ${response.code} tipo=${response.header("Content-Type")} bytes=$bytes")
            assertTrue("$kind retornou HTTP ${response.code}", response.isSuccessful)
            assertTrue("$kind sem bytes", bytes > 0)
        }
    }

    private fun inspectTracks(file: File): List<String> {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            (0 until extractor.trackCount).mapNotNull { index ->
                val format = extractor.getTrackFormat(index)
                format.getString(MediaFormat.KEY_MIME)
            }
        } finally {
            extractor.release()
        }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        println("$TAG $message")
    }

    companion object {
        private const val TAG = "LIVE_TEST"
    }
}
