package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import xyz.klinker.giphy.GiphyView
import xyz.klinker.messenger.BuildConfig
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener

@SuppressLint("ViewConstructor")
class GiphySearchView(context: Context, private val listener: ImageSelectedListener, useStickers: Boolean) : FrameLayout(context) {

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_giphy_search, this, true)

        val giphy = findViewById<GiphyView>(R.id.giphy)
        giphy.setSelectedCallback { uri ->
            listener.onImageSelected(uri, MimeType.IMAGE_GIF)
        }

        giphy.initializeView(BuildConfig.GIPHY_API_KEY, MmsSettings.maxImageSize, useStickers)
    }

}