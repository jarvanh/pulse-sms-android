/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

import xyz.klinker.messenger.R

/**
 * A button that can be used for previewing theme color changes. When pressed, the accent color
 * will be shown around the outside of the button so that the user can see what the theme will
 * look like.
 */
@SuppressLint("ClickableViewAccessibility")
class ColorPreviewButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var innerSize: Float = 0.toFloat()
    private var currentOuterSize: Float = 0.toFloat()
    private var maxOuterSize: Float = 0.toFloat()
    private var size: Float = 0.toFloat()

    private val innerPaint: Paint by lazy { Paint() }
    private val outerPaint: Paint by lazy { Paint() }

    private val shower: ShowThread by lazy { ShowThread(this) }
    private val hider: HideThread by lazy { HideThread(this) }

    private var onClickListener: View.OnClickListener? = null

    init {
        size = toPx(DEFAULT_SIZE)
        innerSize = toPx(DEFAULT_INNER_SIZE)
        currentOuterSize = innerSize
        maxOuterSize = toPx(DEFAULT_OUTER_SIZE)

        minimumHeight = size.toInt()
        minimumWidth = size.toInt()

        innerPaint.isAntiAlias = true
        innerPaint.style = Paint.Style.FILL
        innerPaint.color = DEFAULT_INNER_COLOR

        outerPaint.isAntiAlias = true
        outerPaint.style = Paint.Style.FILL
        outerPaint.color = DEFAULT_OUTER_COLOR
    }

    public override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(size / 2, size / 2, currentOuterSize, outerPaint)
        canvas.drawCircle(size / 2, size / 2, innerSize, innerPaint)
    }

    public override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, w, oldW, oldH)
        size = w.toFloat()
        maxOuterSize = size / 2
        innerSize = size / 2 - toPx(6)
        currentOuterSize = innerSize
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = this.measuredWidth
        setMeasuredDimension(w, w)
    }

    fun setInnerColor(color: Int) {
        if (color == Color.WHITE) {
            this.innerPaint.color = resources.getColor(R.color.colorToReplaceWhite)
        } else {
            this.innerPaint.color = color
        }
    }

    fun setOuterColor(color: Int) {
        this.outerPaint.color = color
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            shower.setRunning(true)
            hider.setRunning(false)
            shower.start()
            return true
        } else if (e.action == MotionEvent.ACTION_CANCEL || e.action == MotionEvent.ACTION_UP) {
            shower.setRunning(false)
            hider.setRunning(true)
            hider.start()

            if (e.action == MotionEvent.ACTION_UP) {
                if (onClickListener != null) {
                    onClickListener!!.onClick(this)
                }
                return false
            }

            return true
        }

        return false
    }

    override fun setOnClickListener(listener: View.OnClickListener?) {
        this.onClickListener = listener
    }

    private fun toPx(dp: Int): Float {
        val r = resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.displayMetrics)
    }

    abstract inner class AnimThread(var view: View) : Thread() {
        private var running = false

        fun setRunning(run: Boolean): AnimThread {
            running = run
            return this
        }

        override fun run() {
            var beginTime: Long
            var timeDiff: Long
            var sleepTime: Int
            var framesSkipped: Int

            while (running) {
                try {
                    beginTime = System.currentTimeMillis()
                    framesSkipped = 0

                    updateView()
                    view.postInvalidate()

                    timeDiff = System.currentTimeMillis() - beginTime
                    sleepTime = (FRAME_PERIOD - timeDiff).toInt()

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime.toLong())
                        } catch (e: InterruptedException) {
                        }

                    }

                    while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                        val updating = updateView()
                        sleepTime += FRAME_PERIOD
                        framesSkipped++

                        if (!updating) {
                            try {
                                setRunning(false)
                                join()
                                stop()
                            } catch (e: Exception) {
                            }

                        }
                    }
                } finally {
                    view.postInvalidate()
                }
            }
        }

        abstract fun updateView(): Boolean
    }

    inner class ShowThread(v: View) : AnimThread(v) {

        override fun updateView(): Boolean {
            if (currentOuterSize < maxOuterSize) {
                currentOuterSize += 1f
                return true
            }

            return false
        }
    }

    inner class HideThread(v: View) : AnimThread(v) {

        override fun updateView(): Boolean {
            if (currentOuterSize > innerSize) {
                currentOuterSize -= 1f
                return true
            }
            return false
        }
    }

    companion object {

        private val DEFAULT_INNER_COLOR = -0x6800
        private val DEFAULT_OUTER_COLOR = -0xc0ae4b
        private val DEFAULT_INNER_SIZE = 30
        private val DEFAULT_OUTER_SIZE = 36
        private val DEFAULT_SIZE = 72

        private val MAX_FPS = 60
        private val MAX_FRAME_SKIPS = 5
        private val FRAME_PERIOD = 1000 / MAX_FPS
    }
}
