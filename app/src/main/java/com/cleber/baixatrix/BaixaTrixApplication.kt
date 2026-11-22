package com.cleber.baixatrix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.cleber.baixatrix.data.resolver.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe

class BaixaTrixApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(NewPipeDownloader())
        getSharedPreferences("youtube_backend", MODE_PRIVATE).edit().clear().apply()
    }

    /** Habilita miniatura de vídeos (usada na biblioteca). */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()
}
