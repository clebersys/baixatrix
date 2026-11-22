package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.BackendSettingsRepository
import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.model.VideoVariant
import com.cleber.baixatrix.util.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Resolves YouTube links through the user's configured self-hosted backend. */
class YoutubeResolver(private val settingsRepository: BackendSettingsRepository) : PlatformResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun resolve(url: String): ResolvedMedia {
        val settings = settingsRepository.load()
        val baseUrl = settings.baseUrl.toHttpUrlOrNull()
            ?: throw ResolveException("Configure o endereço HTTPS do backend em Configurações.")
        if (
            baseUrl.scheme != "https" ||
            baseUrl.username.isNotEmpty() ||
            baseUrl.password.isNotEmpty()
        ) {
            throw ResolveException("O endereço do backend precisa usar HTTPS válido.")
        }
        val endpoint = baseUrl.newBuilder()
            .addPathSegment("resolve")
            .addQueryParameter("url", url)
            .build()

        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .apply {
                if (settings.apiKey.isNotBlank()) header("X-Api-Key", settings.apiKey)
            }
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val message = when (response.code) {
                            401 -> "A chave do backend foi recusada. Confira a configuração em Configurações."
                            422 -> "O backend não encontrou um MP4 progressivo compatível para este vídeo."
                            else -> "O backend YouTube respondeu com HTTP ${response.code}."
                        }
                        throw ResolveException(message)
                    }
                    val responseBody = response.body
                        ?: throw ResolveException("O backend retornou uma resposta vazia.")
                    val source = responseBody.source()
                    source.request(MAX_RESPONSE_BYTES + 1)
                    if (source.buffer.size > MAX_RESPONSE_BYTES) {
                        throw ResolveException("A resposta do backend excedeu o limite permitido.")
                    }
                    parseYoutubeResponse(source.buffer.readUtf8(), url)
                }
            } catch (error: IOException) {
                throw ResolveException(
                    "Não foi possível acessar o backend YouTube. Verifique a URL e a conexão.",
                    cause = error
                )
            }
        }
    }
}

internal fun parseYoutubeResponse(responseBody: String, sourceUrl: String): ResolvedMedia {
    val response = try {
        JSONObject(responseBody)
    } catch (error: Exception) {
        throw ResolveException("O backend retornou uma resposta inválida.", cause = error)
    }

    if (response.optString("status") != "ok") {
        throw ResolveException(
            "Não encontrei um formato MP4 compatível. O vídeo pode estar indisponível."
        )
    }

    val variantsJson = response.optJSONArray("variants")
        ?: throw ResolveException("O backend não retornou formatos de vídeo.")
    val variants = buildList {
        for (index in 0 until minOf(variantsJson.length(), MAX_VARIANTS)) {
            val item = variantsJson.optJSONObject(index) ?: continue
            if (item.optString("mimeType") != "video/mp4") continue
            val mediaUrl = item.optString("url").toHttpUrlOrNull() ?: continue
            if (
                mediaUrl.scheme != "https" ||
                mediaUrl.username.isNotEmpty() ||
                mediaUrl.port != 443 ||
                mediaUrl.host != "googlevideo.com" && !mediaUrl.host.endsWith(".googlevideo.com")
            ) {
                continue
            }
            add(
                VideoVariant(
                    url = mediaUrl.toString(),
                    width = item.optInt("width").takeIf { it > 0 },
                    height = item.optInt("height").takeIf { it > 0 },
                    bitrate = item.optLong("bitrate").takeIf { it > 0 },
                    mimeType = "video/mp4"
                )
            )
        }
    }
    if (variants.isEmpty()) {
        throw ResolveException("O backend não retornou um MP4 progressivo válido.")
    }

    val durationSeconds = response.optDouble("duration", Double.NaN)
    val thumbnailUrl = response.optString("thumbnail")
        .toHttpUrlOrNull()
        ?.takeIf {
            it.scheme == "https" &&
                it.username.isEmpty() &&
                (it.host == "ytimg.com" || it.host.endsWith(".ytimg.com"))
        }
        ?.toString()

    return ResolvedMedia(
        platform = Platform.YOUTUBE,
        sourceUrl = sourceUrl,
        title = response.optString("title").takeIf { it.isNotBlank() && it != "null" }?.take(140),
        author = response.optString("author").takeIf { it.isNotBlank() && it != "null" }?.take(120),
        thumbnailUrl = thumbnailUrl,
        durationMillis = durationSeconds
            .takeIf { it.isFinite() && it >= 0 }
            ?.let { (it * 1000).toLong() },
        variants = variants
    )
}

private const val MAX_VARIANTS = 20
private const val MAX_RESPONSE_BYTES = 1_000_000L
