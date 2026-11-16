package com.cleber.baixatrix.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Cliente HTTP compartilhado por todos os extratores. */
object Http {

    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    /**
     * O Instagram só entrega os dados do post (inclusive a URL do vídeo) na
     * página de embed quando a requisição parece vir de um navegador móvel
     * iOS ou de um robô de indexação. Por isso esses dois agentes são usados.
     */
    const val IPHONE_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    const val GOOGLEBOT_UA =
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun buildRequest(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder()
            .url(url)
            .get()
            .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
        headers.forEach { (key, value) -> builder.header(key, value) }
        return builder.build()
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            client.newCall(buildRequest(url, headers)).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} em ${LinkHost.of(url)}")
                }
                body
            }
        }

    /** Igual a [get], mas também devolve a URL final após redirecionamentos. */
    suspend fun getWithFinalUrl(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        client.newCall(buildRequest(url, headers)).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} em ${LinkHost.of(url)}")
            }
            response.request.url.toString() to body
        }
    }
}

/** Mostra apenas o domínio nas mensagens de erro (sem expor a URL toda). */
internal object LinkHost {
    fun of(url: String): String = runCatching {
        java.net.URI(url).host ?: "site"
    }.getOrDefault("site")
}
