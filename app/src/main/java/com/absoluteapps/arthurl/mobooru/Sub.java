package com.absoluteapps.arthurl.mobooru;

import java.io.Serializable;

/**
 * Created by pspka_000 on 9/10/2015.
 */
public class Sub implements Serializable, Comparable {
    public String subName;
    public int subID = 0;
    public int subscriberCount = 0;
    public boolean selected = false;
    public boolean isNSFW = false;
    public String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Sub(String subName, int subID, int subscriberCount, boolean selected, boolean isNSFW, String desc) {
        this.subName = subName;
        this.subID = subID;
        this.subscriberCount = subscriberCount;
        this.selected = selected;
        this.isNSFW = isNSFW;
        this.desc = desc;
    }

    public boolean isNSFW() {
        return isNSFW;
    }

    public void setNSFW(boolean NSFW) {
        isNSFW = NSFW;
    }

    public Sub(String subName, int id) {
        this.subName = subName.toLowerCase();
        this.subID = id;
    }

    public Sub(String subName, int id, int subscriberCount) {
        this.subName = subName.toLowerCase();
        this.subID = id;
        this.subscriberCount = subscriberCount;
    }

    public int compareTo(Object s2) {
        return ((Sub) this).subName.compareTo(((Sub) s2).subName);
    }
}
