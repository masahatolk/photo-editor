package com.hits.graphic_editor.spline

import android.graphics.Canvas
import android.graphics.Point
import kotlin.math.pow
import kotlin.math.sqrt

fun calculatePoint(first: Point, second: Point, third: Point, fourth: Point, t: Float): Point {
    val point = Point()

    point.x = calculateCubicCoordinate(
        t,
        first.x.toFloat(),
        second.x.toFloat(),
        third.x.toFloat(),
        fourth.x.toFloat()
    ).toInt()
    point.y = calculateCubicCoordinate(
        t,
        first.y.toFloat(),
        second.y.toFloat(),
        third.y.toFloat(),
        fourth.y.toFloat()
    ).toInt()

    return point
}

fun calculateAuxPoint(first: Point, second: Point, third: Point, t: Float): Point {
    val point = Point()

    point.x = calculateCoordinate(
        t,
        first.x.toFloat(),
        second.x.toFloat(),
        third.x.toFloat()
    ).toInt()
    point.y = calculateCoordinate(
        t,
        first.y.toFloat(),
        second.y.toFloat(),
        third.y.toFloat()
    ).toInt()

    return point
}

fun calculateCoordinate(t: Float, c1: Float, c2: Float, c3: Float): Float {
    return (1 - t).pow(2) * c1 + 2 * (1 - t) * t * c2 + t.pow(2) * c3
}

fun calculateCubicCoordinate(t: Float, c1: Float, c2: Float, c3: Float, c4: Float): Float {
    return (1 - t).pow(3) * c1 + 3 * (1 - t).pow(2) * t * c2 + 3 * (1 - t) * t.pow(2) * c3 + t.pow(3) * c4
}

fun calculateExtraPoints(
    controlPoint: Point,
    left: Point,
    right: Point,
    coefficient: Float
): Point {
    val interPoint = Point()
    interPoint.x = ((left.x + coefficient * right.x) / (1 + coefficient)).toInt()
    interPoint.y = ((left.y + coefficient * right.y) / (1 + coefficient)).toInt()

    val diffX = controlPoint.x - interPoint.x
    val diffY = controlPoint.y - interPoint.y

    return Point(diffX, diffY)
}

fun calculateLength(first: Point, second: Point): Float {
    return sqrt((first.x - second.x).toFloat().pow(2) + (first.y - second.y).toFloat().pow(2))
}

fun calculateMiddle(first: Int, second: Int): Int {
    return (first + second) / 2
}

fun calculateMiddlePoint(first: Point, second: Point): Point {
    return Point(calculateMiddle(first.x, second.x), calculateMiddle(first.y, second.y))
}

fun calculateFieldsForMovingPoint(
    path: MutableList<Point>,
    extraPoints: MutableList<Point>,
    pointIndex: Int,
    splineMode: SplineMode
) {
    var leftMiddle: Point? = null
    var rightMiddle: Point? = null
    var leftLength = 0f
    var rightLength = 0f

    val rightIndex = getRightIndex(pointIndex, path.lastIndex)
    if (pointIndex < path.lastIndex || splineMode == SplineMode.POLYGON) {
        rightMiddle = calculateMiddlePoint(path[pointIndex], path[rightIndex])
        rightLength = calculateLength(path[pointIndex], path[rightIndex])
    }
    val leftIndex = getLeftIndex(pointIndex, path.lastIndex)
    if (pointIndex > 0 || splineMode == SplineMode.POLYGON) {
        leftMiddle = calculateMiddlePoint(path[leftIndex], path[pointIndex])
        leftLength = calculateLength(path[leftIndex], path[pointIndex])
    }

    val diffPoint: Point

    if (leftMiddle != null && rightMiddle != null) {
        diffPoint = calculateExtraPoints(
            path[pointIndex],
            leftMiddle,
            rightMiddle,
            leftLength / rightLength
        )
        val leftExtraPoint = Point(leftMiddle.x + diffPoint.x, leftMiddle.y + diffPoint.y)
        val rightExtraPoint = Point(rightMiddle.x + diffPoint.x, rightMiddle.y + diffPoint.y)

        if (pointIndex != 0) {
            extraPoints[(pointIndex - 1) * 2] = leftExtraPoint
            extraPoints[(pointIndex - 1) * 2 + 1] = rightExtraPoint
        } else {
            extraPoints[(path.lastIndex - 1) * 2 + 2] = leftExtraPoint
            extraPoints[(path.lastIndex - 1) * 2 + 3] = rightExtraPoint
        }
    }

    if ((pointIndex + 1 < path.lastIndex || (splineMode == SplineMode.POLYGON && path.size >= 3)) && rightMiddle != null) {
        calculateRightFields(path, extraPoints, rightMiddle, rightLength, pointIndex)
    }
    if ((pointIndex - 1 > 0 || (splineMode == SplineMode.POLYGON && path.size >= 3)) && leftMiddle != null) {
        calculateLeftFields(path, extraPoints, leftMiddle, leftLength, pointIndex)
    }
}

fun calculateRightFields(
    path: MutableList<Point>,
    extraPoints: MutableList<Point>,
    leftMiddle: Point,
    leftLength: Float,
    pointIndex: Int
) {

    val rightIndex = getRightIndex(pointIndex, path.lastIndex)
    val rightMiddle =
        calculateMiddlePoint(path[rightIndex], path[getRightIndex(rightIndex, path.lastIndex)])
    val rightLength =
        calculateLength(path[rightIndex], path[getRightIndex(rightIndex, path.lastIndex)])

    val diffPoint = calculateExtraPoints(
        path[rightIndex],
        leftMiddle,
        rightMiddle,
        leftLength / rightLength
    )

    val leftExtraPoint = Point(leftMiddle.x + diffPoint.x, leftMiddle.y + diffPoint.y)
    val rightExtraPoint = Point(rightMiddle.x + diffPoint.x, rightMiddle.y + diffPoint.y)

    if (pointIndex == 0) {
        extraPoints[0] = leftExtraPoint
        extraPoints[1] = rightExtraPoint
    } else {
        extraPoints[(pointIndex - 1) * 2 + 2] = leftExtraPoint
        extraPoints[(pointIndex - 1) * 2 + 3] = rightExtraPoint
    }
}

fun calculateLeftFields(
    path: MutableList<Point>,
    extraPoints: MutableList<Point>,
    rightMiddle: Point,
    rightLength: Float,
    pointIndex: Int
) {
    val leftIndex = getLeftIndex(pointIndex, path.lastIndex)
    val leftMiddle =
        calculateMiddlePoint(path[leftIndex], path[getLeftIndex(leftIndex, path.lastIndex)])
    val leftLength = calculateLength(path[leftIndex], path[getLeftIndex(leftIndex, path.lastIndex)])

    val diffPoint = calculateExtraPoints(
        path[leftIndex],
        leftMiddle,
        rightMiddle,
        leftLength / rightLength
    )

    val leftExtraPoint = Point(leftMiddle.x + diffPoint.x, leftMiddle.y + diffPoint.y)
    val rightExtraPoint = Point(rightMiddle.x + diffPoint.x, rightMiddle.y + diffPoint.y)

    if (pointIndex != 0 && pointIndex != 1) {
        extraPoints[(pointIndex - 1) * 2 - 2] = leftExtraPoint
        extraPoints[(pointIndex - 1) * 2 - 1] = rightExtraPoint
    } else if (pointIndex == 0) {
        extraPoints[(path.lastIndex - 1) * 2] = leftExtraPoint
        extraPoints[(path.lastIndex - 1) * 2 + 1] = rightExtraPoint
    } else {
        extraPoints[(path.lastIndex - 1) * 2 + 2] = leftExtraPoint
        extraPoints[(path.lastIndex - 1) * 2 + 3] = rightExtraPoint
    }
}

fun calculateAuxSplinePoints(
    canvas: Canvas,
    first: Point,
    second: Point,
    third: Point,
    antialiasingMode: Boolean
) {
    var point: Point
    var t = 0f
    while (t < 1.0f) {

        point = calculateAuxPoint(
            first, second, third, t
        )
        if (antialiasingMode) drawAntialiasingSplinePoint(canvas, point, splinePaint)
        drawSplinePoint(canvas, point, splinePaint)
        t += 0.001f
    }
}


fun getRightIndex(index: Int, lastIndex: Int): Int {
    return if (index < lastIndex) index + 1
    else 0
}

fun getLeftIndex(index: Int, lastIndex: Int): Int {
    return if (index > 0) {
        index - 1
    } else lastIndex
}