package com.mt.ihttp.cache.model

import java.io.Serializable

/**
 * @Description: 缓存bean
 * @Author: CTS
 * @Date: 2020/9/24 10:35
 */
data class CacheResult<T>(var isFromCache: Boolean = false, var data: T) : Serializable {
//    var isFromCache = false
//    var data: T

//    constructor() {}
//    constructor(isFromCache: Boolean) {
//        this.isFromCache = isFromCache
//    }
//
//    constructor(isFromCache: Boolean, data: T) {
//        this.isFromCache = isFromCache
//        this.data = data
//    }

    fun isCache(): Boolean {
        return isFromCache
    }

    fun setCache(cache: Boolean) {
        isFromCache = cache
    }

    override fun toString(): String {
        return "CacheResult{" +
                "isCache=" + isFromCache +
                ", data=" + data +
                '}'
    }
}