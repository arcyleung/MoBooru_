package com.absoluteapps.arthurl.mobooru

import kotlin.math.roundToInt

object Formatter {

    // Converts and rounds:
    // 1234 = 1.2k
    // 9876543 = 9.9M
    fun shortHandFormatter(value: Int): String {
        return if (value < 1000) {
            "$value "
        } else if (value < 1000000) {
            ((value / 100).toFloat().roundToInt() / (1.0 * 10)).toString() + "k "
        } else {
            ((value / 100000).toFloat().roundToInt() / (1.0 * 10)).toString() + "M "
        }
    }
}
