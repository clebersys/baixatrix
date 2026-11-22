package com.cleber.baixatrix.data.processing

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/** Combines separate H.264/HEVC video and AAC audio streams without re-encoding. */
internal object YoutubeStreamMuxer {

    fun mux(
        videoUrl: String,
        audioUrl: String,
        outputFile: File,
        headers: Map<String, String>,
        checkActive: () -> Unit = {}
    ) {
        require(isYoutubeMediaUrl(videoUrl) && isYoutubeMediaUrl(audioUrl)) {
            "As URLs das faixas não são do CDN permitido."
        }

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            videoExtractor.setDataSource(videoUrl, headers)
            audioExtractor.setDataSource(audioUrl, headers)

            val videoTrack = findTrack(videoExtractor, "video/")
                ?: error("A faixa de vídeo não foi encontrada.")
            val audioTrack = findTrack(audioExtractor, "audio/")
                ?: error("A faixa de áudio não foi encontrada.")
            val videoFormat = videoExtractor.getTrackFormat(videoTrack)
            val audioFormat = audioExtractor.getTrackFormat(audioTrack)
            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            val audioMime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()

            require(videoMime == "video/avc" || videoMime == "video/hevc") {
                "Este aparelho não pode combinar o codec de vídeo deste formato."
            }
            require(audioMime == "audio/mp4a-latm") {
                "Este aparelho não pode combinar o codec de áudio deste formato."
            }

            val outputMuxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            muxer = outputMuxer
            val muxerVideoTrack = outputMuxer.addTrack(videoFormat)
            val muxerAudioTrack = outputMuxer.addTrack(audioFormat)
            val rotation = videoFormat.takeIf { it.containsKey(MediaFormat.KEY_ROTATION) }
                ?.getInteger(MediaFormat.KEY_ROTATION)
                ?: 0
            if (rotation != 0) outputMuxer.setOrientationHint(rotation)

            videoExtractor.selectTrack(videoTrack)
            audioExtractor.selectTrack(audioTrack)
            outputMuxer.start()
            muxerStarted = true

            var videoAvailable = videoExtractor.sampleTrackIndex >= 0
            var audioAvailable = audioExtractor.sampleTrackIndex >= 0
            var videoTime = if (videoAvailable) videoExtractor.sampleTime else -1L
            var audioTime = if (audioAvailable) audioExtractor.sampleTime else -1L
            val firstSampleTime = listOfNotNull(
                videoTime.takeIf { videoAvailable },
                audioTime.takeIf { audioAvailable }
            ).minOrNull() ?: 0L
            var sampleBuffer = ByteBuffer.allocateDirect(
                maxOf(inputBufferSize(videoFormat), inputBufferSize(audioFormat))
            )
            val bufferInfo = MediaCodec.BufferInfo()
            var wroteVideo = false
            var wroteAudio = false

            while (videoAvailable || audioAvailable) {
                checkActive()
                val useVideo = !audioAvailable || (videoAvailable && videoTime <= audioTime)
                val extractor = if (useVideo) videoExtractor else audioExtractor
                val targetTrack = if (useVideo) muxerVideoTrack else muxerAudioTrack

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val expectedSize = extractor.sampleSize
                    if (expectedSize > MAX_SAMPLE_SIZE) {
                        throw IOException("Uma amostra de vídeo excede o limite do aparelho.")
                    }
                    if (expectedSize > sampleBuffer.capacity()) {
                        sampleBuffer = ByteBuffer.allocateDirect(expectedSize.toInt())
                    }
                }

                sampleBuffer.clear()
                val size = extractor.readSampleData(sampleBuffer, 0)
                if (size < 0) {
                    if (extractor.sampleTrackIndex >= 0) {
                        throw IOException("Não foi possível ler uma amostra da faixa.")
                    }
                    if (useVideo) videoAvailable = false else audioAvailable = false
                    continue
                }
                if (size > sampleBuffer.capacity()) {
                    throw IOException("Uma amostra de vídeo excede o buffer disponível.")
                }

                val presentationTimeUs = extractor.sampleTime - firstSampleTime
                if (presentationTimeUs < 0) {
                    throw IOException("As faixas têm timestamps incompatíveis.")
                }
                bufferInfo.set(
                    0,
                    size,
                    presentationTimeUs,
                    muxerFlags(extractor.sampleFlags)
                )
                sampleBuffer.position(0)
                sampleBuffer.limit(size)
                outputMuxer.writeSampleData(targetTrack, sampleBuffer, bufferInfo)

                if (useVideo) {
                    wroteVideo = true
                    videoExtractor.advance()
                    videoAvailable = videoExtractor.sampleTrackIndex >= 0
                    videoTime = if (videoAvailable) videoExtractor.sampleTime else -1L
                } else {
                    wroteAudio = true
                    audioExtractor.advance()
                    audioAvailable = audioExtractor.sampleTrackIndex >= 0
                    audioTime = if (audioAvailable) audioExtractor.sampleTime else -1L
                }
            }

            check(wroteVideo && wroteAudio) { "As faixas não contêm dados de mídia." }
            outputMuxer.stop()
            muxerStarted = false
        } finally {
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }
    }

    private fun findTrack(extractor: MediaExtractor, mimePrefix: String): Int? =
        (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith(mimePrefix) == true
        }

    private fun inputBufferSize(format: MediaFormat): Int {
        val requested = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            DEFAULT_BUFFER_SIZE
        }
        if (requested > MAX_SAMPLE_SIZE) {
            throw IOException("O aparelho não suporta o tamanho de amostra deste formato.")
        }
        return requested.coerceAtLeast(MIN_BUFFER_SIZE)
    }

    private fun isYoutubeMediaUrl(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        return url.scheme == "https" &&
            url.username.isEmpty() &&
            url.password.isEmpty() &&
            url.port == 443 &&
            (url.host == "googlevideo.com" || url.host.endsWith(".googlevideo.com"))
    }

    private fun muxerFlags(extractorFlags: Int): Int {
        var flags = 0
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return flags
    }

    private const val DEFAULT_BUFFER_SIZE = 8 * 1024 * 1024
    private const val MIN_BUFFER_SIZE = 64 * 1024
    private const val MAX_SAMPLE_SIZE = 32L * 1024 * 1024
}
