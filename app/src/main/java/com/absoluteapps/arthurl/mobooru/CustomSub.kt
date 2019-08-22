package com.absoluteapps.arthurl.mobooru

class CustomSub : Sub() {
    override fun compareTo(other: Sub): Int {
        return when {
            this.subName > other.subName -> 1
            this.subName < other.subName -> -1
            else -> 0
        }
    }

    // Store in format r/xyz
    var subAddress = ""

    init {
        subAddress = ""
    }
}
