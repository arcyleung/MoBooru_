package com.absoluteapps.arthurl.mobooru;

public class Formatter {

    // Converts and rounds:
    // 1234 = 1.2k
    // 9876543 = 9.9M
    public static String shortHandFormatter(int value) {
        if (value < 1000) {
            return value + " ";
        } else if (value < 1000000) {
            return Math.round(value / 100) / (1.0 * 10) + "k ";
        } else {
            return Math.round(value / 100000) / (1.0 * 10) + "M ";
        }
    }
}
