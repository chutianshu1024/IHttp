package com.mt.ihttp.base

import android.os.Parcel
import android.os.Parcelable

open class ErrorMsg(var code: Int = 0, var msg: String = "网络不稳定~") : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString() ?: "")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(code)
        parcel.writeString(msg)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ErrorMsg> {
        override fun createFromParcel(parcel: Parcel): ErrorMsg {
            return ErrorMsg(parcel)
        }

        override fun newArray(size: Int): Array<ErrorMsg?> {
            return arrayOfNulls(size)
        }
    }

}