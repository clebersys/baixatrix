package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.net.Http
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response as ExtractorResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** OkHttp bridge used by NewPipe Extractor for on-device requests. */
class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    override fun execute(request: ExtractorRequest): ExtractorResponse {
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .header("User-Agent", USER_AGENT)
            .method(
                request.httpMethod(),
                request.dataToSend()?.toRequestBody()
            )

        request.headers().forEach { (name, values) ->
            requestBuilder.removeHeader(name)
            values.forEach { value -> requestBuilder.addHeader(name, value) }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("YouTube requested a verification challenge", request.url())
            }

            val responseBody = response.body
            val bodyText = if (responseBody == null) {
                ""
            } else {
                val source = responseBody.source()
                source.request(MAX_RESPONSE_BYTES + 1)
                if (source.buffer.size > MAX_RESPONSE_BYTES) {
                    throw IOException("Extractor response exceeded the size limit")
                }
                source.buffer.readUtf8()
            }

            return ExtractorResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                bodyText,
                response.request.url.toString()
            )
        }
    }

    companion object {
        /** Keep this aligned with the user agent that generates YouTube stream URLs. */
        const val USER_AGENT = Http.YOUTUBE_UA

        private const val MAX_RESPONSE_BYTES = 8L * 1024 * 1024
    }
}
