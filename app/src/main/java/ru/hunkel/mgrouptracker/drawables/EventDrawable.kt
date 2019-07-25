package ru.hunkel.mgrouptracker.drawables

import android.graphics.*
import android.graphics.drawable.Drawable

class EventDrawable(val color: Int) : Drawable() {
    private var paint: Paint = Paint()

    override fun draw(canvas: Canvas) {
        val height = bounds.height()
        val width = bounds.width()

        val rect = RectF(0.0f, 0.0f, width.toFloat(), height.toFloat())
        paint.color = color
        canvas.drawRoundRect(rect, 50f, 50f, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}