package com.absoluteapps.arthurl.mobooru

import java.io.Serializable

/**
 * Created by pspka_000 on 9/10/2015.
 */
open class Sub : Serializable, Comparable<Sub> {
    override fun compareTo(other: Sub): Int {
        return when {
            this.subName > other.subName -> 1
            this.subName < other.subName -> -1
            else -> 0
        }
    }

    var subName = ""
    var subscriberCount = 0
    var selected = false
    var isNSFW = false
    var isCustom = false
    var desc = ""

    constructor(subName: String, subscriberCount: Int, selected: Boolean, isNSFW: Boolean, isCustom: Boolean, desc: String) {
        this.subName = subName
        this.subscriberCount = subscriberCount
        this.selected = selected
        this.isNSFW = isNSFW
        this.isCustom = isCustom
        this.desc = desc
    }

    constructor(subName: String) {
        this.subName = subName.toLowerCase()
    }

    constructor(subName: String, subscriberCount: Int) {
        this.subName = subName.toLowerCase()
        this.subscriberCount = subscriberCount
    }

    constructor()
}
