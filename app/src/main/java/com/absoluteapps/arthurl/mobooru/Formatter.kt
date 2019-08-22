package com.absoluteapps.arthurl.mobooru

object Formatter {

    // Converts and rounds:
    // 1234 = 1.2k
    // 9876543 = 9.9M
    fun shortHandFormatter(value: Int): String {
        return if (value < 1000) {
            "$value "
        } else if (value < 1000000) {
            (Math.round((value / 100).toFloat()) / (1.0 * 10)).toString() + "k "
        } else {
            (Math.round((value / 100000).toFloat()) / (1.0 * 10)).toString() + "M "
        }
    }
}
