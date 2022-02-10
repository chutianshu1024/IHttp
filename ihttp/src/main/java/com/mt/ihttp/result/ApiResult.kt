package com.mt.ihttp.result

/**
 * @Description: 通用回调
 * @Author: CTS
 * @Date: 2020/2/27 22:42
 */
sealed class ApiResult<out T> {

    data class Success<out T>(val data: T?) : ApiResult<T>()
    data class Error(val code: Int, val mes: String, val data: String) : ApiResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[code =$code,mes=$mes,data = $data]"
        }
    }

}
