package com.cleber.baixatrix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class BaixaTrixApplication : Application(), ImageLoaderFactory {

    /** Habilita miniatura de vídeos (usada na biblioteca). */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()
}
