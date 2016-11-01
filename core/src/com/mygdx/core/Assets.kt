package com.mygdx.core

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.Model

val assets = AssetManager()

fun AssetManager.getTexture(path: String): Texture {
    if (!assets.isLoaded(path)) {
        assets.load(path, Texture::class.java)
        assets.finishLoading()
    }
    return assets.get(path)
}

fun AssetManager.getFont(path: String): BitmapFont {
    if (!assets.isLoaded(path)) {
        assets.load(path, BitmapFont::class.java)
        assets.finishLoading()
    }
    return assets.get(path)
}

fun AssetManager.getSound(path: String): Sound {
    if (!assets.isLoaded(path)) {
        assets.load(path, Sound::class.java)
        assets.finishLoading()
    }
    return assets.get(path)
}

fun AssetManager.getModel(path: String): Model {
    if (!assets.isLoaded(path)) {
        assets.load(path, Model::class.java)
        assets.finishLoading()
    }
    return assets.get(path)
}

fun AssetManager.getMusic(path: String): Music {
    if (!assets.isLoaded(path)) {
        assets.load(path, Music::class.java)
        assets.finishLoading()
    }
    return assets.get(path)
}
