package com.anwesh.uiprojects.alternatesqcirotview

/**
 * Created by anweshmishra on 09/06/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import android.util.Log
import java.util.*

val nodes : Int = 5
val shapes : Int = 4
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val foreColor : Int = Color.parseColor("#1565C0")
val backColor : Int = Color.parseColor("#BDBDBD")
val sweepDeg : Float = 360f
val rotDeg : Float = 90f
val shapeSizeFactor : Float = 3f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawCircleShape(sc : Float, size : Float, paint : Paint) {
    save()
    drawArc(RectF(-size / 2, -size / 2, size / 2, size / 2), 0f, sweepDeg * sc, true, paint)
    restore()
}

fun Canvas.drawSquareShape(sc : Float, size : Float, paint : Paint) {
    save()
    rotate(sweepDeg * sc)
    drawRect(RectF(-size / 2, -size / 2, size / 2, size / 2), paint)
    restore()
}

fun Canvas.drawAlternateShape(i : Int, sc : Float, size : Float, paint : Paint) {
    if (i % 2 == 0) {
        drawSquareShape(sc, size, paint)
    } else {
        drawCircleShape(sc, size, paint)
    }
}

fun Canvas.drawASCNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    rotate(rotDeg * sc2)
    for (j in 0..(shapes - 1)) {
        val sc : Float = sc1.divideScale(j, shapes)
        save()
        rotate(90f * j)
        translate(size / 2, size / 2)
        drawLine(0f, 0f, size, 0f, paint)
        drawAlternateShape(j, sc, size / shapeSizeFactor, paint)
        restore()
    }
    restore()
}

class AlternateSqCiRotView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap {
                    Log.d("started", "${Date().time / 1000}")
                }
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, shapes, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class ASCNode(var i : Int, val state : State = State()) {

        private var prev : ASCNode? = null
        private var next : ASCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = ASCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawASCNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : ASCNode {
            var curr : ASCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class AlternateSqCiRot(var i : Int) {

        private val root : ASCNode = ASCNode(0)
        private var curr : ASCNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : AlternateSqCiRotView) {

        private val animator : Animator = Animator(view)
        private val ascr : AlternateSqCiRot = AlternateSqCiRot(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            ascr.draw(canvas, paint)
            animator.animate {
                ascr.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap(cb : () -> Unit) {
            ascr.startUpdating {
                animator.start()
                cb()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : AlternateSqCiRotView {
            val view : AlternateSqCiRotView = AlternateSqCiRotView(activity)
            activity.setContentView(view)
            return view
        }
    }
}
