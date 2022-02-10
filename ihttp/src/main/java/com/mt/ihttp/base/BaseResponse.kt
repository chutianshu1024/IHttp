package com.mt.ihttp.base

/**
 * @Description:
 * @Author: CTS
 * @Date: 2020/9/16 17:54
 */
//, val result: Int?, val lang: String = "", val baseMd5: String = "")//此行暂时不删
data class BaseResponse<out T>(val result: Int, val msg: String?, val data: T?) {
    fun isSuccess(): Boolean {
        return result == 1
    }
}