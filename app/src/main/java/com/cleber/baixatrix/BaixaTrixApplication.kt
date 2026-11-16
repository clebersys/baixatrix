package com.cleber.baixatrix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.cleber.baixatrix.data.SettingsStore

class BaixaTrixApplication : Application(), ImageLoaderFactory {

    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
    }

    /** Habilita miniatura de vídeos (usada na biblioteca). */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()
}
