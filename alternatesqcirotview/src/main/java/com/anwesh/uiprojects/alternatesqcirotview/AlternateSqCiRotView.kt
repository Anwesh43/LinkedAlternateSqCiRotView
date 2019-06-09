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
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(shapes - 1)) {
        val sc : Float = scale.divideScale(j, shapes)
        save()
        rotate(90f * j)
        translate(size / 2, size / 2)
        drawLine(0f, 0f, size, 0f, paint)
        drawAlternateShape(j, sc, size / shapeSizeFactor, paint)
        restore()
    }
    restore()
}
