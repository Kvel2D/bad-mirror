package com.mygdx.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.mygdx.core.Constants

class UIButton {
    var text: String
    val bounds: Rectangle
    val touchFunction: (button: UIButton) -> Unit

    constructor(text: String, x: Float, y: Float, layout: GlyphLayout, font: BitmapFont, touchFunction: (button: UIButton) -> Unit) {
        this.touchFunction = touchFunction
        this.text = text
        layout.setText(font, text)
        bounds = Rectangle(x, y, layout.width, layout.height)
    }

    fun draw(batch: SpriteBatch, font: BitmapFont) {
        if (bounds.contains(Gdx.input.x.toFloat(), Constants.VIEWPORT_HEIGHT - Gdx.input.y.toFloat()))
            font.color = Color.WHITE
        else
            font.color = Color.LIGHT_GRAY
        font.draw(batch, text, bounds.x, bounds.y + bounds.height)
    }

    fun update() {
        if (Gdx.input.justTouched() && bounds.contains(Gdx.input.x.toFloat(), Constants.VIEWPORT_HEIGHT - Gdx.input.y.toFloat())) {
            touchFunction(this)
        }
    }
}
